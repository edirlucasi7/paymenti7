# ADR-002: Topologia e Consumo Idempotente de MerchantUpdated

* **Status:** Aprovado
* **Data:** 2026-08-29
* **Autor:** Edir Lucas da Silva Icety Braga
* **Impacto:** `payment-gateway-core`

## 1. Contexto e Problema

A ADR-001 deste serviço definiu o consumo idempotente para invalidar o cache de merchants. As ADRs 001 a 004 do `merchant-service` definem o papel do gateway no Data Plane, a publicação *at-least-once* via Outbox e o envelope `MerchantUpdated` versão 1 publicado em `merchant.events` com routing key `merchant.updated`.

O gateway precisa receber esse evento sem compartilhar a fila com outros consumidores, invalidar somente a chave correta no Redis e preservar mensagens que não possam ser processadas.

## 2. Decisão

O gateway declarará o exchange tópico durável `merchant.events`, a fila durável `payment-gateway-core.merchant-cache-invalidation.v1` e o binding pela routing key `merchant.updated`. A fila terá dead-letter exchange `payment-gateway-core.dlx` e dead-letter queue `payment-gateway-core.merchant-cache-invalidation.v1.dlq`.

O listener terá ACK automático ao retornar com sucesso. Exceções terão três tentativas locais, com backoff de 1, 2 e 4 segundos. Após a última falha, a mensagem será rejeitada sem requeue e encaminhada à DLQ, onde ficará disponível para análise e reprocessamento manual.

O contrato de entrada é o envelope já produzido pelo `merchant-service`:

```json
{
  "schemaVersion": 1,
  "eventId": "uuid",
  "aggregateType": "MERCHANT",
  "aggregateId": "uuid",
  "eventType": "MerchantUpdated",
  "occurredAt": "2026-08-28T00:00:00Z",
  "payload": {
    "merchantId": "uuid",
    "status": "ACTIVE"
  }
}
```

O listener aceitará apenas `schemaVersion=1`, UUIDs e `occurredAt` válidos, `aggregateType=MERCHANT`, `eventType=MerchantUpdated`, status `ACTIVE` ou `INACTIVE` e `payload.merchantId` igual a `aggregateId`. Falhas de desserialização ou validação seguem a política de retry/DLQ.

Para um evento ainda não concluído, o gateway apagará `merchant:{merchantId}` e só depois persistirá `merchant:processed-event:{eventId}` com `SET NX` e TTL de 24 horas. Antes de invalidar, o listener consultará essa chave: se ela existir, reconhecerá a mensagem como duplicata sem novo efeito. Se a aplicação falhar antes do registro, a redelivery repete apenas um `DEL`, que é seguro. A semântica permanece *at-least-once*, não *exactly-once*.

## 3. Consequências

### Positivas

* Cada consumidor possui uma fila e ciclo de falha independentes.
* Mensagens inválidas ou falhas de Redis são preservadas na DLQ.
* Duplicatas não removem uma chave reidratada após o evento já ter sido processado.
* O gateway permanece desacoplado do banco de dados do `merchant-service`.

### Riscos assumidos

* A deduplicação depende da retenção de 24 horas no Redis; duplicatas mais antigas podem executar outro `DEL`.
* Duplicatas concorrentes antes da gravação do marcador podem executar mais de um `DEL`, sem alterar o resultado.
* Reprocessamento da DLQ será operacional nesta fase; não há automação de replay.
