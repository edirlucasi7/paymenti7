# Paymenti7 Platform

Plataforma de pagamentos composta por serviços Spring Boot. O repositório contém o ciclo de atualização de merchants, publicação de eventos pelo padrão Outbox e entrada idempotente de pagamentos pelo gateway.

## Módulos

| Módulo | Responsabilidade |
| --- | --- |
| `apps/merchant-service` | Atualiza e consulta merchants; persiste e publica eventos `MerchantUpdated` pelo padrão Outbox. |
| `apps/payment-gateway-core` | Recebe intenções de pagamento de forma idempotente, publica comandos pela outbox e mantém o cache de merchants. |
| `apps/payment-authorization-service` | Consome pagamentos, roteia entre adquirentes, aplica circuit breaker/fallback seguro e publica o resultado. |
| `libs/resilience` | Biblioteca compartilhada de resiliência. |

## Visão da arquitetura atual

![Diagrama C4 do fluxo de cache-aside de merchant](apps/payment-gateway-core/docs/adrs/ADR-003-cache-aside-merchant-rehydration-C4-model.png)

## Tecnologias

- Java 25 e Spring Boot 4.1
- Maven Wrapper
- PostgreSQL 17 com bancos independentes por serviço, Flyway e JPA/Hibernate
- RabbitMQ 4.3 para mensageria
- Redis 7.4 para cache e idempotência do consumer
- Docker Compose para infraestrutura local
- Testcontainers para testes de integração
- springdoc/OpenAPI para Swagger UI

## Pré-requisitos

- JDK 25 configurado no `JAVA_HOME`;
- Docker Engine ou Docker Desktop com Docker Compose;
- portas locais disponíveis conforme a tabela abaixo.

## Subindo localmente

Na raiz do repositório, crie o arquivo local de variáveis a partir do template:

```bash
cp .env.example .env
```

Suba Postgres, RabbitMQ e Redis:

```bash
docker compose up -d
docker compose ps
```

Em três terminais diferentes, ainda a partir da raiz, inicie as aplicações:

```bash
./mvnw -pl apps/merchant-service spring-boot:run
```

```bash
./mvnw -pl apps/payment-gateway-core spring-boot:run
```

```bash
./mvnw -pl apps/payment-authorization-service spring-boot:run
```

O gateway usa `MERCHANT_SERVICE_URL` para acessar o merchant-service. No ambiente local, o valor deve ser `http://localhost:8090`, já definido no `.env.example`.

### IntelliJ IDEA

É possível iniciar as duas classes `MerchantServiceApplication` e `PaymentGatewayCoreApplication` pelo IntelliJ. Mantenha o diretório de trabalho na raiz do repositório para que `spring.config.import=optional:file:.env[.properties]` encontre o `.env`. Na configuração do gateway, confirme também `MERCHANT_SERVICE_URL=http://localhost:8090`.

## Portas locais

| Serviço | Porta | Endereço |
| --- | ---: | --- |
| payment-gateway-core | 8080 | `http://localhost:8080` |
| merchant-service | 8090 | `http://localhost:8090` |
| payment-authorization-service | 8100 | `http://localhost:8100` |
| PostgreSQL do merchant-service | 5432 | `localhost:5432` |
| PostgreSQL do payment-gateway-core | 5433 | `localhost:5433` |
| PostgreSQL do payment-authorization-service | 5434 | `localhost:5434` |
| RabbitMQ AMQP | 5672 | `localhost:5672` |
| RabbitMQ Management | 15672 | `http://localhost:15672` |
| Redis | 6380 | `localhost:6380` |

As portas de infraestrutura podem ser alteradas no `.env`.

## APIs e Swagger

| Serviço | Swagger UI | OpenAPI JSON |
| --- | --- | --- |
| merchant-service | [http://localhost:8090/swagger-ui.html](http://localhost:8090/swagger-ui.html) | [http://localhost:8090/v3/api-docs](http://localhost:8090/v3/api-docs) |
| payment-gateway-core | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |

Rotas públicas documentadas:

| Serviço | Método | Rota | Descrição |
| --- | --- | --- | --- |
| merchant-service | `PUT` | `/v1/admin/merchants/{merchantId}` | Solicita a atualização do status de um merchant. |
| payment-gateway-core | `POST` | `/v1/payments` | Aceita uma intenção de pagamento de forma idempotente e retorna `202` durante o processamento. |
| payment-gateway-core | `GET` | `/v1/payments/{paymentId}` | Consulta o estado assíncrono de um pagamento. |

`GET /internal/v1/merchants/{merchantId}` é uma rota de comunicação entre serviços e permanece fora do Swagger público.

## Testes

Execute toda a suíte a partir da raiz:

```bash
./mvnw verify
```

Os testes de integração usam Testcontainers; portanto, o Docker precisa estar em execução. Mais detalhes sobre os testes de persistência estão em [apps/merchant-service/docs/testing.md](apps/merchant-service/docs/testing.md).

## Parando a infraestrutura

```bash
docker compose down
```

Para remover também os volumes locais de Postgres, RabbitMQ e Redis:

```bash
docker compose down -v
```

O último comando apaga os dados locais persistidos.
