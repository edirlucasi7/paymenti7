# ADR-006: Autorização, Fallback e Circuit Breaker de Adquirentes

* **Status:** Aprovado
* **Data:** 2026-09-19
* **Autor:** Edir Lucas da Silva Icety Braga
* **Impacto:** `payment-gateway-core`, futuro `payment-authorization-service`

![C4 do fluxo de autorização, fallback e circuit breaker de adquirentes](ADR-006-payment-authorization-routing-and-circuit-breaker-C4-model.png)

## 1. Contexto e Problema

A ADR-005 estabeleceu que o `payment-gateway-core` aceita uma intenção de pagamento de forma idempotente, persiste o pagamento e um comando na mesma transação e publica `PaymentRequested` no RabbitMQ. A autorização financeira, o consumidor desse comando e a integração com adquirentes permaneceram fora de escopo.

O próximo estágio precisa escolher uma adquirente, solicitar a autorização e devolver um resultado terminal ao gateway. Essa integração está sujeita a recusas de negócio, indisponibilidade, timeout, respostas ambíguas e entrega duplicada de mensagens. Tratar todos esses casos como falha equivalente pode causar dois problemas opostos:

* indisponibilidade desnecessária quando uma rota comprovadamente não recebeu a autorização e outra rota saudável poderia processá-la; ou
* cobrança duplicada quando um timeout é seguido por uma autorização em outra adquirente, embora a primeira possa ter aprovado a operação.

O evento atual também não contém um instrumento de pagamento. O serviço de autorização não deve receber PAN ou CVV pelo broker nem ampliar o escopo PCI dos componentes atuais.

## 2. Decisão

### 2.1 Serviço e fronteiras

Será criado um `payment-authorization-service`, separado do gateway, responsável por:

* consumir `PaymentRequested` de forma idempotente;
* manter o estado durável da autorização e de cada tentativa;
* selecionar uma adquirente elegível;
* aplicar timeout, circuit breaker e fallback seguro;
* adaptar o contrato interno aos contratos específicos de cada adquirente; e
* publicar o resultado terminal por transactional outbox.

O gateway continuará proprietário da entrada HTTP, da idempotência da intenção e do estado público do pagamento. Credenciais, SDKs e particularidades de adquirentes não serão incorporados ao gateway.

Cada serviço terá seu próprio PostgreSQL. O banco do serviço de autorização manterá, no mínimo, a autorização por `paymentId`, as tentativas externas, os eventos de entrada processados e sua outbox. Não haverá acesso direto ao banco do gateway.

### 2.2 Instrumento de pagamento

`POST /v1/payments` passará a exigir `paymentMethodToken`, uma referência opaca previamente tokenizada. O campo fará parte do hash idempotente e do comando `PaymentRequested`. A origem e a emissão desse token ficam fora desta decisão.

PAN e CVV não serão aceitos nesse contrato, persistidos por esses serviços ou incluídos em mensagens e logs. O token poderá ser persistido apenas como referência necessária ao processamento e nunca será escrito em logs.

Como a inclusão obrigatória do token altera o contrato do comando, `PaymentRequested` passará para `schemaVersion: 2`.

### 2.3 Consumo durável e idempotente

O `payment-authorization-service` declarará a fila durável `payment-authorization-service.payment-requested.v1`, ligada ao exchange `payment.commands` pela routing key `payment.requested`. A fila terá retry limitado e dead-letter queue própria.

Ao consumir um comando, o serviço gravará o `eventId` em uma inbox com restrição única e criará a autorização pendente na mesma transação local. Somente depois do commit a entrega será reconhecida. Uma entrega duplicada não criará nova autorização nem nova tentativa externa. `paymentId` também será único no serviço para proteger contra eventos diferentes referentes ao mesmo pagamento.

A chamada à adquirente não será executada dentro da transação do consumer. Um worker selecionará autorizações pendentes e, antes da chamada externa, persistirá uma tentativa com `authorizationAttemptId`, adquirente, referência idempotente estável e estado `DISPATCHING`. Se a instância cair após esse commit sem registrar uma resposta, a tentativa será considerada incerta e não será reenviada automaticamente.

### 2.4 Roteamento e adapters

A primeira versão terá dois adapters simulados. A ordem será global, igual para todos os merchants, e externalizada em configuração, por exemplo:

```properties
payment.authorization.acquirers-order=SIMULATOR_A,SIMULATOR_B
```

Configuração vazia, adquirente desconhecida ou nomes duplicados impedirão a inicialização. Regras por merchant, distribuição por peso e otimização por custo ficam fora de escopo.

Cada adapter normalizará sua resposta em uma das seguintes classificações:

* `APPROVED`: autorização aprovada, terminal;
* `DECLINED`: recusa de negócio, terminal e sem fallback;
* `SAFE_TO_FALLBACK`: nenhuma autorização foi iniciada ou o contrato da adquirente confirma que a operação não foi processada; ou
* `UNKNOWN`: a solicitação pode ter sido processada, mas não há resultado confiável.

O fallback para a próxima adquirente somente será permitido em `SAFE_TO_FALLBACK`. Circuito já aberto, rota desabilitada ou erro explicitamente documentado pela adquirente como anterior ao processamento são exemplos seguros. Respostas genéricas `5xx`, conexão interrompida depois do envio e timeout não serão presumidos como seguros.

Se todas as rotas terminarem em `SAFE_TO_FALLBACK`, a autorização será concluída como `FAILED`. Uma recusa de negócio nunca será convertida em nova tentativa em outra adquirente.

### 2.5 Circuit breaker

Haverá um circuit breaker independente por adquirente e por instância do serviço, encapsulado pelo módulo compartilhado `libs/resilience` e baseado no Resilience4j. O estado não será coordenado entre réplicas nesta fase. Uma camada de *recovery gate* complementará o estado `HALF_OPEN`, pois o Resilience4j oferece quantidade fixa de chamadas de prova, mas não amostragem percentual com limiar de recuperação diferente do limiar de abertura.

Timeouts, indisponibilidade e erros técnicos alimentarão o breaker; aprovações e recusas de negócio serão chamadas concluídas e não contarão como falha técnica. Com o circuito aberto, pagamentos novos pularão a adquirente antes de qualquer chamada e poderão seguir para a próxima rota. Em `HALF_OPEN`, somente as chamadas de prova permitidas pela configuração alcançarão o adapter.

Em `CLOSED`, será usada uma janela deslizante temporal de 30 segundos, com mínimo de 100 chamadas concluídas. O circuito abrirá quando a taxa de falhas técnicas for maior ou igual a 50%. O timeout do adapter contará como falha, sem um limiar separado para chamadas lentas. O circuito permanecerá em `OPEN` por 60 segundos; durante esse período nenhuma chamada chegará ao adapter e a rota será tratada como segura para fallback de pagamentos novos.

Em `HALF_OPEN`, no máximo 10% dos pagamentos novos, selecionados deterministicamente pelo `paymentId`, serão elegíveis como chamadas de prova. Haverá no máximo 20 provas simultâneas e espera zero por uma vaga; os demais pagamentos seguirão para a próxima rota segura. A janela será avaliada após exatamente 100 provas concluídas e pelo menos 10 segundos de observação: taxa de sucesso técnico maior ou igual a 95% fechará o circuito; 94% ou menos o reabrirá. Se 100 provas forem concluídas antes de 10 segundos, novas provas ficarão suspensas até a avaliação. Uma amostra que não se completar em 2 minutos reabrirá o circuito.

Para o breaker, uma resposta válida e interpretável, tanto `APPROVED` quanto `DECLINED`, será sucesso técnico. Timeout, falha de conexão, `429`, `5xx`, resposta inválida, `SAFE_TO_FALLBACK` técnico e `UNKNOWN` serão falhas técnicas. Todos os parâmetros permanecerão externalizados para calibração, mantendo estes valores como padrão aprovado.

### 2.6 Resultado incerto e reconciliação futura

Timeout, queda depois de persistir `DISPATCHING` ou qualquer resposta ambígua moverá a tentativa para `PENDING_RECONCILIATION`. O pagamento permanecerá publicamente em `PROCESSING`, nenhuma outra adquirente será chamada para essa intenção e será emitido alerta operacional.

Não haverá reconciliação automática nesta fase. Um futuro `reconciliation-service` poderá consultar a adquirente pela referência estável, receber webhooks ou repetir uma operação quando o contrato externo garantir idempotência. Esse serviço e seus contratos serão objeto de outra ADR.

O circuit breaker continuará protegendo pagamentos novos mesmo que uma tentativa anterior esteja `PENDING_RECONCILIATION`; abrir o circuito não altera nem resolve tentativas já enviadas.

### 2.7 Resultado e atualização do gateway

Resultados terminais serão publicados por outbox no exchange durável `payment.events`, com routing key `payment.authorization.completed`, usando o envelope existente e o evento `PaymentAuthorizationCompleted` em `schemaVersion: 1`:

```json
{
  "schemaVersion": 1,
  "eventId": "uuid",
  "aggregateType": "PAYMENT",
  "aggregateId": "uuid-do-pagamento",
  "eventType": "PaymentAuthorizationCompleted",
  "occurredAt": "2026-09-19T00:00:00Z",
  "payload": {
    "paymentId": "uuid-do-pagamento",
    "status": "APPROVED",
    "acquirer": "SIMULATOR_A",
    "authorizationAttemptId": "uuid-da-tentativa",
    "providerReference": "referencia-externa",
    "reasonCode": "APPROVED"
  }
}
```

`providerReference`, `authorizationAttemptId` e `acquirer` poderão ser ausentes quando `FAILED` ocorrer antes de uma chamada externa. Nenhum evento terminal será publicado enquanto a tentativa estiver `PENDING_RECONCILIATION`.

O gateway declarará a fila durável `payment-gateway-core.payment-authorization-completed.v1`, com retry e DLQ, e consumirá o evento de forma idempotente. A atualização do pagamento e da resposta terminal de idempotência ocorrerá na mesma transação local. Uma repetição do mesmo resultado será ignorada; resultados terminais conflitantes irão para tratamento operacional e não sobrescreverão a primeira decisão.

## 3. Contrato HTTP

O comando de criação passará a aceitar:

```json
{
  "merchantId": "11111111-1111-1111-1111-111111111111",
  "amount": 125.90,
  "currency": "BRL",
  "paymentMethodToken": "pmt_opaque_token"
}
```

O primeiro `POST /v1/payments` continuará retornando `202 Accepted`. Enquanto o pagamento estiver pendente, retries com a mesma `Idempotency-Key` e o mesmo payload continuarão retornando `202`. Alterar inclusive o token com a mesma chave resultará em `409 Conflict`.

Será criado `GET /v1/payments/{paymentId}` para acompanhamento assíncrono. Um pagamento existente retornará `200 OK` com `paymentId` e `status`; um identificador inexistente retornará `404 Not Found`.

```json
{
  "paymentId": "22222222-2222-2222-2222-222222222222",
  "status": "PROCESSING"
}
```

No replay terminal do `POST`, `APPROVED` e `DECLINED` retornarão `200 OK` com o estado no corpo. `FAILED`, reservado a falha técnica definitiva sem resultado financeiro incerto, retornará `502 Bad Gateway`. O `GET` retornará `200 OK` para qualquer estado existente, pois o status é o recurso consultado.

## 4. Consequências

### Positivas

* O gateway permanece desacoplado de integrações e credenciais específicas de adquirentes.
* A inbox impede que a entrega *at-least-once* do RabbitMQ crie novas autorizações para o mesmo comando.
* A tentativa persistida antes da chamada reduz o risco de repetição cega após queda da aplicação.
* Circuit breakers isolados permitem desviar pagamentos novos de uma adquirente degradada.
* A distinção entre recusa, falha segura e resultado incerto evita fallback indevido e reduz risco de cobrança duplicada.
* A consulta por `paymentId` fecha o contrato assíncrono iniciado pelo `202 Accepted`.

### Riscos assumidos

* `PENDING_RECONCILIATION` poderá permanecer indefinidamente até existir processo operacional ou o futuro serviço de reconciliação.
* O PostgreSQL e a outbox garantem durabilidade local, mas não fornecem *exactly-once* na adquirente.
* A referência idempotente externa só evita duplicidade se a adquirente honrar esse contrato.
* Uma ordem global não otimiza aprovação, custo ou capacidade por merchant.
* Os simuladores validam a arquitetura, mas não substituem testes de contrato com adquirentes reais.

## 5. Alternativas Consideradas

* **Integrar adquirentes diretamente no gateway:** rejeitada para não misturar entrada e idempotência com credenciais, SDKs, roteamento e falhas externas.
* **Autorizar de forma síncrona durante o `POST`:** rejeitada porque amplia a latência, mantém a conexão do cliente aberta e enfraquece o fluxo durável estabelecido pela ADR-005.
* **Executar fallback em qualquer timeout ou erro técnico:** rejeitada pelo risco de a primeira adquirente ter processado a autorização apesar da resposta perdida.
* **Configurar rotas por merchant desde a primeira versão:** adiada até existirem adquirentes reais e requisitos de custo, capacidade e conversão.
* **Implementar reconciliação dentro do serviço de autorização agora:** adiada para um futuro `reconciliation-service`, preservando o escopo desta fase.

## 6. Critérios de Aceitação para a Implementação

* Uma entrega duplicada de `PaymentRequested` não cria outra autorização nem outra tentativa.
* Aprovação na primeira adquirente conclui o pagamento sem chamar a segunda.
* Recusa na primeira adquirente conclui como `DECLINED`, sem fallback.
* Circuito aberto ou falha comprovadamente segura na primeira rota permite usar a segunda.
* Timeout ou queda depois de `DISPATCHING` mantém o pagamento em `PROCESSING`, registra `PENDING_RECONCILIATION` e nunca chama outra adquirente.
* Duas rotas seguramente indisponíveis resultam em `FAILED` e evento terminal.
* Eventos terminais duplicados são processados de forma idempotente pelo gateway.
* O `GET` apresenta corretamente estados pendentes e terminais.
* Reutilizar a chave com outro `paymentMethodToken` retorna `409`.
* Testes e inspeção de logs confirmam que PAN, CVV e o próprio token não são registrados.
* O circuito abre com pelo menos 100 chamadas e 50% de falhas na janela de 30 segundos, permanecendo aberto por 60 segundos.
* Em recuperação, no máximo 10% das requisições e 20 provas simultâneas atingem a adquirente; 95 de 100 sucessos fecham o circuito e 94 de 100 o reabrem.
