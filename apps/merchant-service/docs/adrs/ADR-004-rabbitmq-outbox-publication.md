# ADR-004: Publicação da Outbox no RabbitMQ

* **Status:** Aprovado
* **Data:** 2026-08-28
* **Autor:** Edir Lucas da Silva Icety Braga
* **Impacto:** `merchant-service`

## 1. Contexto e Problema

A ADR-002 definiu a entrega *at-least-once* por meio da Transactional Outbox e a ADR-003 definiu o polling com compactação. O adapter de saída existente apenas registrava os eventos em log, portanto os eventos pendentes ainda não chegavam ao barramento assíncrono.

## 2. Decisão

O `merchant-service` publicará os eventos processados em exchanges RabbitMQ do tipo `topic`, duráveis. O roteamento é definido pelo enum de infraestrutura `RabbitMqEventRouting`, que associa `eventType`, exchange e routing key. Inicialmente, `MerchantUpdated` será publicado no exchange `merchant.events` com a routing key `merchant.updated`.

Para adicionar um novo evento, será suficiente incluir uma constante nesse enum com seu `eventType`, exchange e routing key. No deploy, a configuração de topologia percorrerá o enum e declarará automaticamente cada exchange distinto. Eventos sem rota cadastrada falharão na publicação e permanecerão pendentes na outbox.

A mensagem terá `contentType` `application/json`, será persistente e seguirá o envelope de versão `1`:

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

O publisher usará *publisher confirms* correlacionados, com timeout de **2 segundos**. Somente um ACK do broker permite que o worker marque o evento como `PUBLISHED`. O ACK confirma o aceite da publicação pelo broker no exchange; não confirma a entrega em uma fila ou a um consumidor. NACK, timeout, indisponibilidade ou falha de serialização causam erro e mantêm o evento pendente para a próxima execução, aceitando duplicidade em caso de confirmação perdida.

O publisher registrará em nível `INFO` o envio ao exchange e o ACK recebido, identificando o evento, o aggregate, o tipo, a routing key e o tempo de confirmação. Falhas de envio, NACK, timeout, interrupção e erros de confirmação serão registradas em nível `WARN`, sem incluir o payload da mensagem.

O RabbitMQ será disponibilizado localmente por Docker Compose, incluindo o plugin de Management em `http://localhost:15672`. Nesta fase não serão declaradas filas ou bindings: a confirmação representa o aceite pelo exchange, e consumidores definirão suas próprias filas posteriormente.

## 3. Consequências

### Positivas

* Concretiza a publicação assíncrona sem acoplar o `merchant-service` a um consumidor.
* Preserva a semântica *at-least-once* da outbox entre banco e broker.
* Expõe `eventId` para a futura deduplicação pelos consumidores.

### Riscos assumidos

* Sem fila vinculada, o exchange não retém eventos para consumidores criados futuramente; isto é aceitável nesta fase de publisher isolado.
* Não há DLQ, política de retry externa nem ShedLock nesta decisão.
