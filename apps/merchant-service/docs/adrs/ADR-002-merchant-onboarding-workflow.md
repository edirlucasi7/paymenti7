# ADR-002: Garantia de Entrega de Eventos via Outbox Pattern e Diretriz de TTL no Cache

* **Status:** Aprovado
* **Data:** 2026-08-21
* **Autor:** Edir Lucas da Silva Icety Braga
* **Impacto:** `merchant-service` (Control Plane), `payment-gateway-core` (Data Plane)

---

## 1. Contexto e Problema

A **ADR-001** estabeleceu o desacoplamento entre a gestão de lojistas (`merchant-service`) e a autorização de pagamentos (`payment-gateway-core`) utilizando invalidação de cache orientada a eventos via RabbitMQ.

No entanto, a arquitetura identificou dois cenários críticos de falha:

1. **Dual-Write / Falha na Publicação de Eventos:** Se o `merchant-service` atualizar o PostgreSQL e falhar (crash, perda de rede) antes de publicar a mensagem no RabbitMQ, o evento `MerchantUpdated` será perdido. Isso deixará o cache do Redis com dados desatualizados indefinidamente (*Stale Cache*).
2. **Dependência de Cache Infinito:** Se o evento de invalidação for perdido ou o broker ficar indisponível por alguns minutos, o `payment-gateway-core` pode continuar autorizando transações com configurações antigas se as chaves no Redis não tiverem um tempo de expiração definido.

---

## 2. Decisão

Para resolver esses pontos, decidimos aplicar duas estratégias complementares:

### A. Implementação do Transactional Outbox Pattern no `merchant-service`
O `merchant-service` **não enviará mensagens diretamente ao RabbitMQ** dentro do fluxo da requisição HTTP.

1. A gravação do cadastro/status do merchant e a inserção do evento na tabela `outbox_events` ocorrerão **na mesma transação ACID do PostgreSQL**.
2. Um *Worker* assíncrono interno lerá a tabela `outbox_events` e publicará as mensagens pendentes no RabbitMQ com garantia de entrega *At-Least-Once* (Pelo Menos Uma Vez).
3. Após a confirmação de publicação pelo broker (ACK), a mensagem será marcada como processada na tabela.

### B. Diretriz de Contrato: Exigência de TTL Curto no Redis pelo Data Plane
Fica estabelecido como diretriz de arquitetura para qualquer consumidor do cache de merchants (incluindo o `payment-gateway-core`):

1. **Proibição de Chaves Sem Expiração:** Nenhuma chave de merchant deve ser salva no Redis com tempo de vida ilimitado.
2. **Janela de TTL:** O tempo de vida (TTL) das chaves de merchant no Redis deve ser configurado entre **5 e 15 minutos**.
3. **Autocura de Inconsistência:** Caso ocorra uma falha catastrófica no evento de invalidação via RabbitMQ, o Redis se expurgará sozinho dentro dessa janela, forçando o Data Plane a buscar a versão mais recente e reidratar o cache com segurança.

---

## 3. Consequências

### Positivas
* **Garantia de Entrega (At-Least-Once):** Elimina a perda de eventos causada pelo problema do *Dual-Write*.
* **Resiliência e Autocura:** O sistema se recupera de inconsistências no cache de forma automática dentro do tempo estipulado pelo TTL, mesmo com o `merchant-service` fora do ar durante uma janela de manutenção.
* **Simplicidade Operacional:** O Outbox Pattern é mantido de forma nativa na própria transação do PostgreSQL, dispensando ferramentas externas complexas de Change Data Capture (CDC) no estágio atual.

### Negativas / Riscos Mitigados
* **Idempotência Obrigatória no Consumidor:** A garantia *At-Least-Once* do Outbox Pattern pode resultar no envio de mensagens duplicadas em cenários de reconexão.
  * *Mitigação:* O `payment-gateway-core` deve processar os eventos do RabbitMQ de forma **idempotente** (utilizando o `event_id` único do evento para ignorar duplicatas).
* **Sobrecarga de Polling no Banco:** A leitura constante da tabela `outbox_events` pode gerar overhead de I/O no banco relacional.
  * *Mitigação:* O worker utilizará busca paginada otimizada por índice e índices parciais no PostgreSQL (`WHERE processed_at IS NULL`).