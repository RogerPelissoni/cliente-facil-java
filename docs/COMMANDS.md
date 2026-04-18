# 📦 Comandos Essenciais - Maven, Flyway, Docker e Spring Boot

## 🚀 Maven (mvn)

### ▶️ Rodar aplicação (Spring Boot)

```bash
mvn spring-boot:run
```

### 📦 Build do projeto (gera .jar)

```bash
mvn clean package
```

### 🧹 Limpar build

```bash
mvn clean
```

### 📥 Baixar dependências

```bash
mvn install
```

### 🔄 Rebuild completo

```bash
mvn clean install
```

### 🚫 Pular testes

```bash
mvn clean install -DskipTests
```

---

## 🗄️ Flyway

### ▶️ Executar migrations

```bash
mvn flyway:migrate
```

### 🔍 Ver status das migrations

```bash
mvn flyway:info
```

### 🛠️ Reparar histórico (corrigir checksums)

```bash
mvn flyway:repair
```

### 🔥 Resetar banco (CUIDADO!)

```bash
mvn flyway:clean
```

### 🔄 Reset completo + recriar estrutura

```bash
mvn flyway:clean flyway:migrate
```

---

## ⚙️ Configuração importante (Spring Boot)

### Habilitar clean (apenas DEV)

```yaml
spring:
  flyway:
    clean-disabled: false
```

---

## 🐳 Docker

### ▶️ Subir containers

```bash
docker-compose up -d
```

### 🔄 Rebuild containers

```bash
docker-compose up -d --build
```

### 🛑 Parar containers

```bash
docker-compose down
```

### 📜 Ver logs

```bash
docker-compose logs -f
```

---

## 🐳 Resetar banco de dados (PostgreSQL)

### 🔥 Reset completo (remove volume e dados)

```bash
docker-compose down -v
```

### 🔄 Subir novamente banco zerado

```bash
docker-compose up -d
```

### 🔁 Fluxo completo (reset + migrations)

```bash
docker-compose down -v
docker-compose up -d
mvn flyway:migrate
```

---

## ⚠️ Alternativa (reset apenas do banco)

```bash
rm -rf ../cliente-facil-database
docker-compose up -d
```

---

## 🧠 Reset apenas do schema (sem destruir container)

```bash
docker exec -it db psql -U postgres -d clientefacil -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
mvn flyway:migrate
```

---

## 🧪 Testes

### Rodar testes

```bash
mvn test
```

### Rodar testes com log detalhado

```bash
mvn test -X
```

---

## 📌 Dicas úteis

- Quando algo quebrar:

```bash
mvn clean install
```

- Para desenvolvimento:

```bash
mvn spring-boot:run
```

- Reset completo do banco:

```bash
docker-compose down -v && docker-compose up -d && mvn flyway:migrate
```

---

## ⚠️ Atenção

❌ Nunca rode:

```bash
mvn flyway:clean
```

em produção.

❌ Nunca rode:

```bash
docker-compose down -v
```

em ambiente com dados importantes.

---

## ✅ Fluxo recomendado (DEV)

```bash
docker-compose up -d
mvn flyway:migrate
mvn spring-boot:run
```

---

## ⚡ Reset rápido (mais usado no dia a dia)

```bash
rm -rf ../cliente-facil-database && docker-compose up -d && mvn flyway:migrate
```
