# ADR-001: Consumo Idempotente de Invalidação do Cache de Merchants

* **Status:** Aprovado
* **Data:** 2026-08-29
* **Autor:** Edir Lucas da Silva Icety Braga
* **Impacto:** `payment-gateway-core`

## 1. Contexto e Problema

As ADRs do `merchant-service` estabeleceram o `merchant-service` como Control Plane e o `payment-gateway-core` como Data Plane. A ADR-001 definiu que o Data Plane consulta o cache distribuído, em vez de consultar o banco de merchants no caminho crítico. A ADR-002 definiu Transactional Outbox com publicação *at-least-once* e explicitou que consumidores devem tratar o `eventId` como chave de idempotência. A ADR-003 permite compactação de eventos pendentes e a ADR-004 definiu o contrato publicado no RabbitMQ.

O `MerchantUpdated` é publicado no exchange durável `merchant.events`, com routing key `merchant.updated`, envelope `schemaVersion` 1 e identificador único `eventId`. RabbitMQ pode reenviar a mesma mensagem quando ocorrer uma falha entre o processamento do consumidor e seu ACK. O gateway precisa invalidar o cache sem deixar uma atualização de merchant sem efeito e sem introduzir uma dependência do banco do `merchant-service` no Data Plane.

## 2. Decisão

O `payment-gateway-core` terá uma fila durável exclusiva para a invalidação de merchants, chamada `payment-gateway-core.merchant-cache-invalidation.v1`, vinculada ao exchange `merchant.events` pela routing key `merchant.updated`.

Falhas de processamento terão até três tentativas locais, com backoff, antes de a mensagem ser rejeitada sem requeue e encaminhada para a dead-letter queue `payment-gateway-core.merchant-cache-invalidation.v1.dlq`, por meio de uma dead-letter exchange durável. A DLQ será preservada para inspeção e reprocessamento operacional; não haverá descarte automático.

O cache de cada merchant usará a chave `merchant:{merchantId}`. A futura escrita dessa chave terá TTL configurável entre 5 e 15 minutos, com padrão de 10 minutos, conforme a diretriz da ADR-002. A invalidação remove a chave com `DEL`, operação naturalmente idempotente.

O consumidor registrará no Redis o `eventId` concluído por 24 horas, com operação atômica `SET NX`. A ordem de processamento será:

1. Validar o envelope e obter o `merchantId`.
2. Executar `DEL merchant:{merchantId}`.
3. Registrar o `eventId` como concluído no Redis.
4. Confirmar a mensagem para o RabbitMQ.

Se o processo falhar antes do registro do `eventId`, a reentrega repetirá apenas o `DEL`, que permanece segura. Se a falha ocorrer depois do registro e antes do ACK, a reentrega encontrará o `eventId` e será ignorada. Essa estratégia mantém a semântica *at-least-once* e impede que uma deduplicação prematura deixe um cache desatualizado.

## 3. Consequências

### Positivas

* Mantém o `payment-gateway-core` desacoplado do banco de dados do `merchant-service`.
* Garante que duplicatas não produzam efeitos incorretos no cache.
* Preserva mensagens com falha na DLQ para análise, em vez de descartá-las.
* A TTL do cache continua sendo a autocura para falhas catastróficas de entrega.

### Riscos assumidos

* O registro de deduplicação não fornece *exactly-once*; ele reduz processamento repetido dentro de 24 horas.
* Duplicatas concorrentes antes do registro podem executar mais de um `DEL`, sem impacto funcional.
* O primeiro estágio não declara a topologia RabbitMQ nem inicia listeners; isso será implementado com testes de integração na próxima etapa.