# Testes de persistência

Os testes de repositório usam Testcontainers e iniciam `postgres:17-alpine` automaticamente. Não é necessário subir o `compose.yaml` nem definir variáveis `POSTGRES_*`.

Execute a suíte completa a partir da raiz do repositório:

```bash
./mvnw verify
```

O Testcontainers detecta o Docker automaticamente, tanto em máquinas locais quanto no GitHub Actions. Caso a detecção falhe em uma instalação Linux que usa o socket padrão, crie `~/.testcontainers.properties` com:

```properties
docker.client.strategy=org.testcontainers.dockerclient.UnixSocketClientProviderStrategy
```
