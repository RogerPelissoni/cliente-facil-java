# 📦 COMMANDS.md

Guia rápido com os principais comandos utilizados durante o desenvolvimento do Cliente Fácil.

---

# ☕ Maven

## Executar a aplicação

```bash
mvn spring-boot:run
```

## Compilar o projeto

```bash
mvn clean package
```

## Instalar dependências e gerar build

```bash
mvn clean install
```

## Build sem testes

```bash
mvn clean install -DskipTests
```

## Executar testes

```bash
mvn test
```

## Executar um teste específico

```bash
mvn test -Dtest=UserServiceTest
```

## Limpar build

```bash
mvn clean
```

## Limpar pasta de build (casos específicos)

```bash
rm -rf target
```

> Útil quando o Flyway continua encontrando migrations removidas.

## Ver árvore de dependências

```bash
mvn dependency:tree
```

## Limpar cache local de dependências

```bash
mvn dependency:purge-local-repository
```

---

# 🗄️ Flyway

## Executar migrations

```bash
mvn flyway:migrate
```

## Verificar status

```bash
mvn flyway:info
```

## Reparar checksums

```bash
mvn flyway:repair
```

## Limpar banco (⚠️ Desenvolvimento apenas)

```bash
mvn flyway:clean
```

## Limpar e recriar banco

```bash
mvn flyway:clean flyway:migrate
```

Para permitir o comando `clean` em desenvolvimento:

```yaml
spring:
  flyway:
    clean-disabled: false
```

---

# 🐳 Docker

## Subir containers

```bash
docker compose up -d
```

## Recriar containers

```bash
docker compose up -d --build
```

## Parar containers

```bash
docker compose down
```

## Remover containers e volumes

```bash
docker compose down -v
```

## Visualizar logs

```bash
docker compose logs -f
```

## Listar containers

```bash
docker compose ps
```

---

# 🐘 PostgreSQL

## Resetar apenas o schema

```bash
docker exec -it db psql -U postgres -d clientefacil -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

Depois execute:

```bash
mvn flyway:migrate
```

---

# 🚀 Fluxos recomendados

## Primeira execução

```bash
docker compose up -d
mvn spring-boot:run
```

## Atualizar banco

```bash
mvn flyway:migrate
```

## Build para distribuição

```bash
mvn clean package
```

## Reset completo do ambiente

```bash
docker compose down -v
docker compose up -d
mvn flyway:migrate
```

---

# 🛠️ Troubleshooting

## Flyway encontrou migrations duplicadas

```bash
rm -rf target
mvn clean
```

## Corrigir checksum

```bash
mvn flyway:repair
```

## Banco inconsistente

```bash
docker compose down -v
docker compose up -d
mvn flyway:migrate
```

---

# 🔍 Verificações

## Versão do Java

```bash
java -version
```

## Versão do Maven

```bash
mvn -version
```

## Versão do Docker

```bash
docker --version
docker compose version
```

---

# ⚠️ Atenção

Nunca execute os comandos abaixo em produção:

```bash
mvn flyway:clean
```

```bash
docker compose down -v
```

Esses comandos removem dados do banco e devem ser utilizados apenas em ambiente de desenvolvimento.
