# ClienteFacil API (Projeto de Estudo)

API REST em Java/Spring Boot com autenticacao JWT, auditoria automatica e suporte a multi-tenant com escopo global (`company_id = null`).

## 1) Objetivo do projeto

Este projeto foi construido para estudo de:
- arquitetura em camadas (controller -> service -> repository -> entity)
- autenticacao stateless com JWT
- auditoria automatica com Spring Data JPA Auditing
- isolamento de dados por tenant (empresa)
- registros globais compartilhados entre empresas
- versionamento de schema com Flyway

## 2) Stack tecnica

- Java 21
- Spring Boot 3.2.5
- Spring Web
- Spring Data JPA (Hibernate)
- Spring Security
- Spring AOP (para filtro automatico de tenant)
- PostgreSQL
- Flyway
- MapStruct
- Lombok
- springdoc OpenAPI (Swagger)

Arquivo principal de dependencias:
- [pom.xml](/E:/DeveloperContainer/Java/clientefacil/pom.xml)

## 3) Como rodar

### 3.1 Pre-requisitos

- Java 21
- Maven Wrapper (ja incluso no projeto: `mvnw`)
- PostgreSQL rodando

### 3.2 Configuracao

Ajuste o datasource em:
- [application.yml](/E:/DeveloperContainer/Java/clientefacil/src/main/resources/application.yml)

Propriedades atuais:
- `spring.datasource.url=jdbc:postgresql://localhost:5432/clientefacil`
- `spring.datasource.username=postgres`
- `spring.datasource.password=admin`
- `jwt.secret` e `jwt.expiration`

### 3.3 Inicializacao

1. Suba o banco.
2. Rode:

```bash
./mvnw spring-boot:run
```

O Flyway cria/atualiza as tabelas automaticamente.

## 4) Migrations e banco

Migrations em:
- `src/main/resources/db/migration`

Arquivos:
- [V1__create_users_table.sql](/E:/DeveloperContainer/Java/clientefacil/src/main/resources/db/migration/V1__create_users_table.sql)
- [V2__create_company_table.sql](/E:/DeveloperContainer/Java/clientefacil/src/main/resources/db/migration/V2__create_company_table.sql)
- [V3_1__create_person_gender_enum.sql](/E:/DeveloperContainer/Java/clientefacil/src/main/resources/db/migration/V3_1__create_person_gender_enum.sql)
- [V3_2__create_person_table.sql](/E:/DeveloperContainer/Java/clientefacil/src/main/resources/db/migration/V3_2__create_person_table.sql)
- [V4__create_profile_table.sql](/E:/DeveloperContainer/Java/clientefacil/src/main/resources/db/migration/V4__create_profile_table.sql)
- [V5__create_users_company_constraints.sql](/E:/DeveloperContainer/Java/clientefacil/src/main/resources/db/migration/V5__create_users_company_constraints.sql)

Observacao importante (ambiente de estudo):
- Se voce editar migrations ja aplicadas, o Flyway pode acusar `checksum mismatch`.
- Em inicio de projeto, a pratica mais simples e recriar o schema local.

## 5) Estrutura de camadas

### 5.1 Controller

Expoe endpoints HTTP e delega para services.

- [AuthController.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/controller/AuthController.java)
- [UserController.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/controller/UserController.java)

### 5.2 Service

Concentra regras de negocio.

- [AuthService.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/service/AuthService.java)
- [AuthenticatedUserService.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/service/AuthenticatedUserService.java)
- [UserService.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/service/UserService.java)
- [JwtService.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/service/JwtService.java)

### 5.3 Repository

Acesso a dados com Spring Data JPA.

- [UserRepository.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/repository/UserRepository.java)
- [PersonRepository.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/repository/PersonRepository.java)
- [ProfileRepository.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/repository/ProfileRepository.java)
- [CompanyRepository.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/repository/CompanyRepository.java)

### 5.4 Entity

Modelo relacional.

- [User.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/entity/User.java)
- [Person.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/entity/Person.java)
- [Profile.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/entity/Profile.java)
- [Company.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/entity/Company.java)

## 6) Auditoria automatica

### 6.1 O que e auditado

A base [AbstractAuditableEntity.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/entity/base/AbstractAuditableEntity.java) adiciona:
- `created_at`
- `updated_at`
- `created_by`
- `updated_by`

### 6.2 Como funciona

1. `@EnableJpaAuditing` e ativado em [JpaAuditingConfig.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/config/JpaAuditingConfig.java).
2. `AuditingEntityListener` preenche datas automaticamente.
3. [CurrentUserAuditorAware.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/security/CurrentUserAuditorAware.java) retorna o usuario atual (`userId`) para `created_by/updated_by`.

## 7) Multi-tenant e registros globais

### 7.1 Base tenant

[AbstractAuditableTenantEntity.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/entity/base/AbstractAuditableTenantEntity.java) define:
- `company_id`
- preenchimento automatico de `company_id` no `@PrePersist` quando possivel
- modo global por `markAsGlobalScope()` (forca `company_id = null`)

### 7.2 Regra de escopo

- Registro tenant: `company_id = <id da empresa>`
- Registro global: `company_id = null`

Registros globais sao visiveis para qualquer empresa.

## 8) Finalidade do TenantFilterAspect

Arquivo:
- [TenantFilterAspect.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/security/TenantFilterAspect.java)

Ele existe para evitar repeticao de filtro de tenant em todo service/repository.

### Sem o aspecto

Voce precisaria repetir manualmente em varios pontos algo como:
- pegar `companyId` do usuario logado
- aplicar `where company_id = ... or company_id is null`

### Com o aspecto (atual)

1. Intercepta metodos publicos em `br.com.clientefacil.service..*`.
2. Se houver usuario autenticado, habilita o filtro Hibernate `tenantFilter`.
3. Injeta `companyId` atual no filtro.
4. Executa o metodo de negocio.
5. Desabilita o filtro ao final.

Condicao do filtro:
- `(company_id = :companyId OR company_id IS NULL)`

Resultado:
- dados da empresa atual + dados globais
- menos codigo repetido
- menos chance de esquecer filtro em alguma query

### Desabilitar o filtro em casos especificos

Para casos como relatorios gerenciais, voce pode desabilitar o filtro com `@SkipTenantFilter`.

Arquivo:
- [SkipTenantFilter.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/security/SkipTenantFilter.java)

Uso por metodo:

```java
@Service
public class RelatorioService {

    @SkipTenantFilter
    public RelatorioResponse consolidadoGeral() {
        // sem filtro de tenant
    }
}
```

Uso por classe:

```java
@SkipTenantFilter
@Service
public class RelatorioService {
}
```

Observacao:
- use com cuidado; esse bypass ignora o isolamento por empresa.

## 9) Seguranca e autenticacao JWT

### 9.1 Configuracao HTTP

[SecurityConfig.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/config/SecurityConfig.java):
- `POST /api/v1/auth/login` liberado
- Swagger liberado
- demais rotas autenticadas

### 9.2 Fluxo de login

1. `POST /api/v1/auth/login`
2. [AuthService.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/service/AuthService.java) valida email/senha.
3. [JwtService.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/service/JwtService.java) gera token JWT.

### 9.3 Fluxo de request autenticada

1. Header `Authorization: Bearer <token>`
2. [JwtFilter.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/security/JwtFilter.java) extrai email do token.
3. [AuthenticatedUserService.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/service/AuthenticatedUserService.java) carrega usuario e `companyId`.
4. Contexto de seguranca recebe `AuthenticatedUser`.
5. Auditoria e filtro de tenant passam a usar esse contexto.

## 10) Utilitarios de seguranca

- [SecurityUtils.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/security/SecurityUtils.java): acesso ao usuario autenticado, `userId` e `companyId`.
- [AuthenticatedUser.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/security/AuthenticatedUser.java): principal autenticado da aplicacao.

## 11) Seeder de dados

[DataSeeder.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/config/DataSeeder.java) cria dados iniciais:
- company: `Cliente Facil`
- person, profile e usuario admin
- login inicial: `admin@admin.com` / `123456`

## 12) Endpoints atuais

### 12.1 Login

`POST /api/v1/auth/login`

Exemplo:

```json
{
  "email": "admin@admin.com",
  "password": "123456"
}
```

Retorno:

```json
{
  "token": "..."
}
```

### 12.2 Buscar usuario por id

`GET /api/v1/users/{id}` (autenticado)

### 12.3 Criar usuario

`POST /api/v1/users` (autenticado)

Exemplo:

```json
{
  "name": "Joao",
  "email": "joao@empresa.com",
  "password": "123456",
  "personId": 1,
  "profileId": 1
}
```

## 13) Swagger

- UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`

## 14) Tratamento de erros

[GlobalExceptionHandler.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/exception/GlobalExceptionHandler.java) trata:
- validacao (`400`)
- integridade de dados (`400`)
- runtime generico (`500`)

[CustomAuthenticationEntryPoint.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/security/CustomAuthenticationEntryPoint.java) trata nao autenticado (`401`).

[CustomAccessDeniedHandler.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/security/CustomAccessDeniedHandler.java) trata acesso negado (`403`).

## 15) Pontos de estudo recomendados

1. Melhorar erros de negocio (evitar `RuntimeException` generica).
2. Cobrir autenticacao e tenant com testes de integracao.
3. Evoluir autorizacao por perfil/role.
4. Revisar impacto do filtro de tenant em todos os tipos de consulta (principalmente custom/nativas).
5. Extrair DTOs de erro padronizados.

## 16) Glossario rapido (versao didatica)

Formato desta secao:
- `Definicao`: o que e
- `Serve para`: finalidade pratica
- `Quando usar`: regra de bolso
- `Neste projeto`: onde aparece

### Spring Core

`Bean`
- Definicao: objeto gerenciado pelo Spring.
- Serve para: centralizar criacao/configuracao e permitir injecao de dependencia.
- Quando usar: praticamente sempre para servicos/componentes da aplicacao.
- Neste projeto: services, repositories, filtros e configs sao beans.

`ApplicationContext` (container Spring)
- Definicao: registro central de beans da aplicacao.
- Serve para: resolver dependencias e ciclo de vida.
- Quando usar: normalmente de forma indireta (via injecao), sem acessar manualmente.
- Neste projeto: usado implicitamente em toda injecao por construtor.

`@Component`
- Definicao: marca classe como bean Spring.
- Serve para: tornar a classe injetavel.
- Quando usar: classes tecnicas/genericas.
- Neste projeto: [JwtFilter.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/security/JwtFilter.java), [TenantFilterAspect.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/security/TenantFilterAspect.java).

`@Service`
- Definicao: especializacao semantica de `@Component`.
- Serve para: deixar claro que a classe representa regra de negocio.
- Quando usar: camada service.
- Neste projeto: [UserService.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/service/UserService.java), [AuthService.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/service/AuthService.java).

`@Configuration`
- Definicao: classe de configuracao Spring.
- Serve para: declarar beans e comportamentos globais.
- Quando usar: setup de seguranca, swagger, auditoria, beans utilitarios.
- Neste projeto: [SecurityConfig.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/config/SecurityConfig.java), [JpaAuditingConfig.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/config/JpaAuditingConfig.java).

`@Bean`
- Definicao: transforma o retorno de um metodo em bean Spring.
- Serve para: criar objeto configurado de forma explicita.
- Quando usar: classe de terceiro ou construcao manual.
- Neste projeto: [SecurityBeansConfig.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/config/SecurityBeansConfig.java) cria `PasswordEncoder`.
- Observacao: se a classe e sua e simples, prefira `@Component/@Service` na propria classe.

`@RequiredArgsConstructor` (Lombok)
- Definicao: gera construtor com campos `final`.
- Serve para: injecao por construtor sem boilerplate.
- Quando usar: classes com dependencias `final`.
- Neste projeto: [AuthService.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/service/AuthService.java).

### AOP (Aspect-Oriented Programming)

`@Aspect`
- Definicao: marca classe como aspecto (codigo transversal).
- Serve para: aplicar comportamento comum sem repetir codigo.
- Quando usar: log, auditoria tecnica, filtros, transacoes cruzadas.
- Neste projeto: [TenantFilterAspect.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/security/TenantFilterAspect.java) ativa filtro de tenant automaticamente.

`@Around`
- Definicao: advice que executa antes e depois do metodo alvo.
- Serve para: controlar a execucao da chamada (inclusive bloquear/alterar fluxo).
- Quando usar: quando precisa abrir/fechar contexto (como filtro Hibernate).
- Neste projeto: habilita e desabilita `tenantFilter` por chamada de service.

`ProceedingJoinPoint`
- Definicao: representa o metodo interceptado.
- Serve para: chamar `proceed()` e executar o metodo real.
- Quando usar: dentro de `@Around`.

`pointcut` (`execution(...)`)
- Definicao: expressao que seleciona metodos interceptados.
- Serve para: limitar escopo do aspecto.
- Neste projeto: `execution(public * br.com.clientefacil.service..*(..))`.

### Web e REST

`@RestController`
- Definicao: controller REST (retorno serializado em JSON).
- Serve para: expor endpoints HTTP.
- Neste projeto: [AuthController.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/controller/AuthController.java).

`@RequestMapping`
- Definicao: rota base de classe/metodo.
- Serve para: organizar URL dos endpoints.

`@GetMapping` / `@PostMapping`
- Definicao: mapeamento por verbo HTTP.
- Serve para: declarar contrato REST.

`@RequestBody`
- Definicao: converte corpo JSON em objeto Java.
- Serve para: entrada de payload.

`@PathVariable`
- Definicao: captura parametro de rota.
- Serve para: ids e identificadores no path.

`@Valid`
- Definicao: ativa validacao Bean Validation no parametro.
- Serve para: impedir entrada invalida antes da regra de negocio.

### Validacao

`@NotBlank`
- Definicao: string nao nula e nao vazia.
- Neste projeto: campos obrigatorios de DTO.

`@NotNull`
- Definicao: valor obrigatorio.

`@Email`
- Definicao: formato de email valido.

`@Size`
- Definicao: tamanho minimo/maximo.

### JPA e Hibernate

`@Entity`
- Definicao: classe persistida como tabela.

`@Table`
- Definicao: nome da tabela.

`@Id` + `@GeneratedValue`
- Definicao: chave primaria + estrategia de geracao.

`@Column`
- Definicao: mapeamento de coluna.
- Serve para: nome, `nullable`, `unique`, etc.

`@ManyToOne` + `@JoinColumn`
- Definicao: relacionamento N:1 e FK.
- Neste projeto: `User -> Person`, `User -> Profile`.

`@MappedSuperclass`
- Definicao: classe base com campos herdados por entidades.
- Neste projeto: [AbstractAuditableEntity.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/entity/base/AbstractAuditableEntity.java) e [AbstractAuditableTenantEntity.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/entity/base/AbstractAuditableTenantEntity.java).

`@PrePersist`
- Definicao: callback antes do `INSERT`.
- Serve para: preencher valores automaticos.
- Neste projeto: preencher `company_id` quando ausente.

`@Transient`
- Definicao: campo nao persistido.
- Neste projeto: `globalScope`.

`@FilterDef` / `@Filter` (Hibernate)
- Definicao: filtro de consulta ativado em runtime.
- Serve para: aplicar restricao global sem repetir `where`.
- Neste projeto: `(company_id = :companyId OR company_id IS NULL)`.

### Auditoria

`@EnableJpaAuditing`
- Definicao: liga auditoria automatica do Spring Data.
- Neste projeto: [JpaAuditingConfig.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/config/JpaAuditingConfig.java).

`AuditorAware<T>`
- Definicao: informa quem e o "auditor atual".
- Serve para: preencher `created_by/updated_by`.
- Neste projeto: [CurrentUserAuditorAware.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/security/CurrentUserAuditorAware.java).

`@CreatedDate` / `@LastModifiedDate`
- Definicao: datas automaticas de criacao/alteracao.

`@CreatedBy` / `@LastModifiedBy`
- Definicao: usuario criador/ultimo editor automatico.

`AuditingEntityListener`
- Definicao: listener que aplica anotacoes de auditoria.

### Seguranca

`SecurityFilterChain`
- Definicao: pipeline de filtros de seguranca.
- Neste projeto: libera login/swagger e protege o restante.

`OncePerRequestFilter`
- Definicao: filtro executado uma vez por requisicao.
- Neste projeto: [JwtFilter.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/security/JwtFilter.java).

`SecurityContextHolder`
- Definicao: armazena autenticacao da thread atual.
- Serve para: recuperar usuario logado.

`Authentication`
- Definicao: objeto que representa autenticacao atual.

`UsernamePasswordAuthenticationToken`
- Definicao: implementacao comum de `Authentication`.
- Neste projeto: usado para setar `AuthenticatedUser` no contexto.

`UserDetails`
- Definicao: contrato do principal autenticado.
- Neste projeto: [AuthenticatedUser.java](/E:/DeveloperContainer/Java/clientefacil/src/main/java/br/com/clientefacil/security/AuthenticatedUser.java).

`GrantedAuthority`
- Definicao: permissao/role do usuario.

`AuthenticationEntryPoint`
- Definicao: resposta para requisicao sem autenticacao.
- Neste projeto: retorna `401`.

`AccessDeniedHandler`
- Definicao: resposta para usuario autenticado sem permissao.
- Neste projeto: retorna `403`.

### JWT

`JWT`
- Definicao: token assinado digitalmente com claims.
- Neste projeto: guarda email no `subject`.

`Bearer Token`
- Definicao: formato do header `Authorization`.

`subject` (claim)
- Definicao: identificador principal do token.

`expiration`
- Definicao: tempo de expiracao do token.

### Banco e migrations

`Flyway`
- Definicao: versionamento de schema.
- Serve para: aplicar SQL em ordem e manter historico.

`migration`
- Definicao: arquivo SQL versionado (`V1__...`, `V2__...`).

`checksum`
- Definicao: hash da migration aplicado pelo Flyway.
- Armadilha comum: editar migration ja aplicada gera `checksum mismatch`.

### Multi-tenant (neste projeto)

`tenant`
- Definicao: escopo de dados por empresa (`company_id`).

`registro global`
- Definicao: registro com `company_id = null`.
- Efeito: visivel para todas as empresas.

`TenantFilterAspect`
- Definicao: aspecto que ativa o filtro Hibernate por chamada de service.
- Serve para: evitar repetir filtro manual em cada consulta.

`@SkipTenantFilter`
- Definicao: anotacao para pular o filtro de tenant em metodo/classe de service.
- Serve para: consultas cross-tenant controladas (ex.: relatorios gerenciais).
- Risco: pode expor dados de varias empresas; usar somente em cenarios autorizados.

`company_id`
- Regra no projeto:
- se existe valor: dado pertence a uma empresa especifica.
- se `null`: dado global.
