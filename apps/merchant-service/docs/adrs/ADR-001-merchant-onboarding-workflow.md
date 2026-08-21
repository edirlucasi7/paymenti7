# ADR-001: Desacoplamento da Gestão de Merchants da Execução de Pagamentos via Cache Distribuído

* **Status:** Aprovado
* **Data:** 2026-08-20
* **Autor:** [Edir Lucas da Silva Icety Braga]
* **Impacto:** `merchant-service`, `payment-gateway-core`

## 1. Contexto e Problema
O `merchant-service` é responsável por cadastrar e manter os dados de contas, credenciais, limites operacionais, moedas aceitas e adquirentes configuradas para cada lojista.

No entanto, o motor de autorização de pagamentos (Data Plane) precisa validar essas informações a cada requisição síncrona de pagamento (até 5.000 TPS no pico). Se o Data Plane consultar diretamente o banco de dados SQL do `merchant-service` em cada transação, enfrentaremos:
1. Latência excessiva no caminho síncrono do pagamento.
2. Forte acoplamento: Se o `merchant-service` cair, todo o processamento de pagamentos da plataforma para de funcionar.

## 2. Opções Consideradas

* **Opção 1 (Síncrona direta):** Chamada gRPC/HTTP do `payment-core` para o `merchant-service` em cada requisição.
    * *Prós:* Dados 100% atualizados em tempo real.
    * *Contras:* Latência alta, gargalo no banco de dados do merchant-service, SPOF catastrófico.

* **Opção 2 (Cache Distribuído com Event-Driven Invalidation):**
  O `payment-core` consulta as configurações no Redis. Quando um merchant atualiza seus dados no `merchant-service`, um evento `MerchantUpdated` é publicado no RabbitMQ, e o cache do Redis é invalidado ou atualizado.
    * *Prós:* Resposta em micro-segundos no Data Plane. Se o `merchant-service` cair, o pagamento continua funcionando com os dados em cache.
    * *Contras:* Eventual consistência curta (alguns milissegundos para refletir alterações de cadastro).

## 3. Decisão
A opção escolhida foi a **Opção 2**.

O `merchant-service` funcionará estritamente no **Control Plane** (operações de escrita/painel do lojista), enquanto os dados necessários para o processamento de pagamento serão propagados para um **Redis Cluster** de leitura rápida acessível pelo `payment-gateway-core` no **Data Plane**.

## 4. Consequências

### Positivas
* **Isolamento de Falha:** O `merchant-service` pode entrar em manutenção ou sofrer indisponibilidade sem afetar a autorização de novos pagamentos.
* **Baixa Latência:** Redução do overhead de validação de merchant para menos de 2ms.

### Negativas / Riscos Mitigados
* **Consistência Eventual:** Se um merchant for desativado por fraude, haverá uma janela de milissegundos até o evento de invalidação expurgar o cache.
* *Mitigação:* O evento de alteração de status/fraude terá prioridade máxima de publicação no broker.

