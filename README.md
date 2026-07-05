
# Cliente Fácil

> API REST desenvolvida em **Java 21** e **Spring Boot**, utilizando uma arquitetura em camadas, autenticação JWT, auditoria automática e suporte a multi-tenancy.

---

## Objetivo

O Cliente Fácil é um projeto desenvolvido com foco em boas práticas de arquitetura, organização de código e escalabilidade. Seu objetivo é servir como base para aplicações corporativas, adotando padrões amplamente utilizados no ecossistema Spring.

Principais características:

- Arquitetura em camadas
- API REST
- Autenticação Stateless com JWT
- Multi-tenancy por empresa
- Auditoria automática
- Versionamento do banco com Flyway
- Mapeamento utilizando MapStruct
- Documentação OpenAPI (Swagger)

---

# Tecnologias

| Tecnologia | Versão |
|------------|---------|
| Java | 21 |
| Spring Boot | 3.x |
| Spring Security | ✔ |
| Spring Data JPA | ✔ |
| Hibernate | ✔ |
| PostgreSQL | ✔ |
| Flyway | ✔ |
| MapStruct | ✔ |
| Lombok | ✔ |
| Maven | ✔ |
| Swagger / OpenAPI | ✔ |

---

# Arquitetura

O projeto utiliza uma arquitetura em camadas, separando claramente as responsabilidades entre cada componente.

```text
HTTP Request
      │
      ▼
 Controller
      │
      ▼
  Service
      │
      ▼
 Repository
      │
      ▼
 PostgreSQL
      │
      ▲
   Mapper
      │
      ▼
HTTP Response
```

## Responsabilidades

### Controller

- Expor endpoints REST
- Validar requisições
- Delegar processamento para os serviços

### Service

- Implementar regras de negócio
- Orquestrar operações
- Controlar transações

### Repository

- Persistência utilizando Spring Data JPA

### Mapper

- Conversão entre Entidades e DTOs

### Entity

- Modelo persistido no banco de dados

### DTO

- Objetos utilizados para entrada e saída da API

---

# Estrutura do Projeto

```text
src/main/java/br/com/clientefacil
│
├── controller
├── service
├── repository
├── entity
├── dto
├── mapper
├── search
├── seeder
└── core
```

## Organização

| Pacote | Responsabilidade |
|--------|------------------|
| controller | Endpoints REST |
| service | Regras de negócio |
| repository | Persistência |
| entity | Entidades JPA |
| dto | Objetos de entrada e saída |
| mapper | Conversão entre objetos |
| search | Filtros e consultas |
| seeder | Dados iniciais |
| core | Componentes compartilhados |

---

# Padrão dos Módulos

Cada módulo segue uma estrutura padronizada.

```text
User
├── UserController
├── UserService
├── UserRepository
├── UserMapper
├── UserRequest
├── UserResponse
├── UserScreenResponse
└── UserSearchConfig
```

Esse padrão facilita manutenção, padronização e escalabilidade do projeto.

---

# Funcionalidades

- Autenticação JWT
- Controle de acesso
- Auditoria automática
- Multi-tenancy
- Paginação
- Ordenação
- Busca dinâmica
- Versionamento do banco
- Seed de dados
- Documentação OpenAPI

---

# Multi-tenancy

O sistema possui isolamento automático por empresa (tenant).

Cada usuário trabalha apenas com os registros pertencentes à sua empresa, mantendo a possibilidade de utilizar registros globais compartilhados quando necessário.

O isolamento é aplicado automaticamente durante a execução das consultas, reduzindo código repetitivo e diminuindo a possibilidade de falhas.

---

# Auditoria

As entidades auditáveis registram automaticamente:

- Data de criação
- Usuário criador
- Data da última alteração
- Usuário responsável pela alteração

Esse comportamento é transparente para as regras de negócio.

---

# Segurança

A autenticação é baseada em JSON Web Token (JWT).

Fluxo simplificado:

```text
Login
   │
   ▼
 JWT
   │
   ▼
 Spring Security
   │
   ▼
 Controller
```

Todos os endpoints protegidos exigem um token válido.

---

# Banco de Dados

O projeto utiliza PostgreSQL com versionamento de schema através do Flyway.

As alterações estruturais do banco são realizadas exclusivamente por migrations.

---

# Como Executar

## Pré-requisitos

- Java 21
- PostgreSQL
- Maven (ou Maven Wrapper)

## Clonar o projeto

```bash
git clone <url-do-repositorio>
cd cliente-facil-java
```

## Configurar banco

Configure o datasource em:

```text
src/main/resources/application.yml
```

## Executar

```bash
./mvnw spring-boot:run
```

O Flyway criará ou atualizará automaticamente o banco de dados.

---

# Swagger

Após iniciar a aplicação:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI:

```text
http://localhost:8080/v3/api-docs
```

---

# Convenções

O projeto segue algumas convenções simples:

- Controllers não implementam regras de negócio.
- Services concentram a lógica da aplicação.
- Repositories são responsáveis apenas pela persistência.
- Mappers realizam toda conversão entre entidades e DTOs.
- Cada módulo mantém uma estrutura consistente.
- Novas funcionalidades devem seguir o padrão existente.

---

# Roadmap

Funcionalidades previstas para evolução do projeto:

- Eventos assíncronos
- Cache
- Testes de integração
- Observabilidade
- Upload de arquivos
- Melhorias na autorização

---

## License

This project is distributed under the **Cliente Fácil License**.

The source code is publicly available for study, learning, portfolio, and evaluation purposes.

Commercial use, redistribution as a commercial product, or offering this software as a hosted service (SaaS) requires prior written authorization from the copyright holder.

For commercial licensing or partnership inquiries, please contact the author.
