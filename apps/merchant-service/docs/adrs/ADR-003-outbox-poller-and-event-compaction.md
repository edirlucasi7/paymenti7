# ADR-003: Polling da Outbox com Compactação de Eventos de Merchant

* **Status:** Aprovado
* **Data:** 2026-08-24
* **Autor:** Edir Lucas da Silva Icety Braga
* **Impacto:** `merchant-service`

## 1. Contexto e Problema

A ADR-002 definiu o Transactional Outbox Pattern, mas a tabela pode acumular múltiplos eventos pendentes do mesmo merchant enquanto o broker estiver indisponível. Para invalidação de cache, apenas o estado mais recente do merchant é relevante; publicar estados intermediários aumenta trabalho sem alterar o estado final do consumidor.

## 2. Decisão

O `merchant-service` executará um worker interno com Spring `@Scheduled` a cada **5 segundos**, processando no máximo **20 merchants** por execução.

O worker selecionará apenas o evento pendente mais recente por `aggregate_id`. Eventos anteriores ainda pendentes serão marcados como `SUPPRESSED`, receberão `processed_at` e apontarão para o evento vencedor (último deles) em `superseded_by_event_id`. O evento vencedor será publicado e, após sucesso, marcado como `PUBLISHED`.

A seleção usará `FOR UPDATE SKIP LOCKED`. O índice parcial existente por `occurred_at` é preservado para varredura cronológica de pendências e um índice parcial composto por `aggregate_id`, `occurred_at DESC` e `id DESC` será usado para encontrar eficientemente o último evento por merchant.

A publicação é representada por uma porta de saída; nesta etapa, seu adapter apenas registra o evento em log. Uma futura integração RabbitMQ substituirá esse adapter sem alterar o caso de uso.

## 3. Consequências

### Positivas

* Reduz eventos redundantes enviados ao consumidor, preservando uma trilha auditável de supressão.
* Evita que workers concorrentes processem a mesma linha bloqueada.
* Mantém o scheduler, a regra de negócio e os detalhes de infraestrutura separados pela arquitetura hexagonal.

### Riscos assumidos

* Sem ShedLock, cada réplica da aplicação dispara seu próprio scheduler. `SKIP LOCKED` impede que a mesma linha seja processada simultaneamente, mas não centraliza a execução do job nem elimina a disputa por lotes. ShedLock deverá ser reavaliado antes do escalonamento horizontal.
