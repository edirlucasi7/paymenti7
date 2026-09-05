# ADR-004: Ordenação por Revisão do Cache de Merchants

* **Status:** Aprovado
* **Data:** 2026-09-05
* **Autor:** Edir Lucas da Silva Icety Braga
* **Impacto:** `payment-gateway-core`, `merchant-service`

## 1. Contexto e Problema

A ADR-003 definiu a reidratação *cache-aside* de `merchant:{merchantId}`. Quando essa chave não existe, o `payment-gateway-core` consulta `GET /internal/v1/merchants/{merchantId}` e armazena a resposta no Redis. A ADR-002 definiu que `MerchantUpdated` remove a chave para que a próxima validação obtenha o estado posterior à alteração.

Há uma corrida entre esses dois fluxos. Uma validação pode iniciar a consulta remota para o estado atual de um merchant; antes de sua resposta ser gravada, o merchant pode ser alterado e o evento pode invalidar a chave. Se a resposta correspondente ao estado anterior terminar depois do `DEL`, ela recria um cache obsoleto até a próxima invalidação ou expiração.

O `eventId` garante deduplicação, mas não define a ordem entre estados diferentes do mesmo merchant. O `occurredAt` também não é uma revisão persistida do agregado e não deve ser usado para decidir qual estado é mais recente.

## 2. Decisão

O `merchant-service` será proprietário de uma revisão monotônica por merchant.

1. A tabela `merchants` terá a coluna `revision BIGINT NOT NULL DEFAULT 0`. Cada alteração efetiva do merchant incrementará a revisão na mesma transação que persiste o merchant e cria o evento na outbox.
2. A revisão fará parte do domínio do merchant e será exposta por `GET /internal/v2/merchants/{merchantId}`. O gateway usará essa rota para reidratar o cache; a rota interna v1 permanecerá inalterada.
3. O `payload` do evento `MerchantUpdated` conterá `merchantId`, `status` e `revision`.
4. O valor armazenado em `merchant:{merchantId}` incluirá a revisão. O gateway também manterá `merchant:revision:{merchantId}` com a maior revisão conhecida, por 24 horas. Ambas as chaves usarão `{merchantId}` para pertencerem ao mesmo *hash slot* em Redis Cluster.
5. O consumidor de `MerchantUpdated` executará uma operação atômica no Redis: se a revisão do evento for maior que a marca conhecida, atualizará a marca por 24 horas e removerá `merchant:{merchantId}`. Eventos de revisão igual ou inferior não alterarão o cache.
6. Depois de consultar o endpoint interno, o gateway executará uma operação atômica de reidratação: se a revisão recebida for menor que a marca conhecida, não salvará a resposta no Redis; caso contrário, gravará o valor em `merchant:{merchantId}` com o TTL configurado e atualizará a marca de revisão por 24 horas.

As operações dos itens 5 e 6 serão implementadas como scripts Lua curtos e atômicos. Os scripts não executarão I/O, chamadas remotas, varreduras ou processamento proporcional à quantidade de chaves.

O marcador existente `merchant:processed-event:{eventId}` continuará com TTL de 24 horas e preservará a política de idempotência definida na ADR-002.

O contrato do evento será:

```json
{
  "schemaVersion": 1,
  "eventId": "uuid",
  "aggregateType": "MERCHANT",
  "aggregateId": "uuid",
  "eventType": "MerchantUpdated",
  "occurredAt": "2026-09-05T00:00:00Z",
  "payload": {
    "merchantId": "uuid",
    "status": "ACTIVE",
    "revision": 17
  }
}
```

## 3. Consequências

### Positivas

* Uma resposta anterior à invalidação não consegue recriar persistentemente um valor obsoleto no cache.
* Eventos duplicados ou recebidos fora de ordem não reduzem a maior revisão conhecida.
* A origem da ordenação é o estado persistido do merchant, e não horário ou identificador aleatório do evento.

### Riscos assumidos

* A validação que já estava em voo pode ainda retornar sua resposta anterior ao chamador; a decisão impede apenas que ela volte ao cache compartilhado.
* Até o evento ser consumido pelo gateway, permanece a janela de consistência eventual estabelecida pelas ADRs anteriores.
* `merchant:revision:{merchantId}` permanece por 24 horas após a última atividade, aumentando o uso de memória do Redis proporcionalmente aos merchants acessados nesse período.
