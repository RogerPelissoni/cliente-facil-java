# Roadmap do projeto

Ponto de entrada único para "o que vem a seguir" no Cliente Fácil. É neste arquivo que se baseia o
fluxo de desenvolvimento e as ideias em aberto do projeto — antes de começar qualquer funcionalidade
nova, comece por aqui. Todo conteúdo de roadmap do projeto vive neste arquivo; não existe outro
documento de roadmap, limitações conhecidas ou regras de negócio futuras espalhado pelo repositório.

Ver também (documentação de estado atual, não de roadmap): [product/1_business-rules.md](product/1_business-rules.md)
(regras de negócio já implementadas hoje) e [database_diagram.drawio](database_diagram.drawio) (fonte
editável do diagrama ERD, abrir em draw.io / diagrams.net — o conteúdo dele está resumido na seção
"Estrutura de dados base" abaixo).

## Índice

1. [Estrutura de dados base](#estrutura-de-dados-base)
2. [Roadmap técnico](#roadmap-técnico)
3. [Regras de negócio para módulos futuros](#regras-de-negócio-para-módulos-futuros)
4. [Limitações conhecidas (trade-offs aceitos)](#limitações-conhecidas-trade-offs-aceitos)

## Como usar este documento

1. Antes de começar uma funcionalidade nova, confira se ela já está mapeada nas seções "Roadmap
   técnico" ou "Regras de negócio para módulos futuros" — evita redesenhar algo que já foi pensado.
2. Se a estrutura de dados precisar mudar em relação ao `database_diagram.drawio`, atualize o diagrama
   e a tabela-resumo da seção "Estrutura de dados base" junto com a implementação, pra este documento
   não ficar desatualizado.
3. Ao terminar um item, marque `[x]` na lista correspondente e mude o status para ✅ na tabela de
   estrutura de dados, quando for referente à estrutura de dados.

---

## Estrutura de dados base

O `database_diagram.drawio` é a base de dados pensada para o sistema completo (não só o que já existe
no código) — inclui os módulos futuros descritos na seção "Regras de negócio para módulos futuros".
**Não é definitiva**: pode e deve ser ajustada sempre que uma regra de negócio real não bater com o que
está desenhado.

Resumo por módulo (✅ já existe como entidade no código, 🔲 só está no diagrama):

### User - Profile - Permission - Company
| Tabela | Campos | Status |
|---|---|---|
| `users` | id, email, password, tp_role, profile_id→profile, company_id→company, person_id→person | ✅ |
| `profile` | id, name | ✅ |
| `profile_permission` | id, profile_id→profile, resource_id→resource | ✅ |
| `resource` | id, module_id→module, name, signature | ✅ |
| `module` | id, name (financial, ...) | ✅ |
| `tp_role` (ENUM) | admin, company `[CONFIG]`, professional `[CONFIG]`, client `[CONFIG]` | ✅ (enum) |

### Person and Related
| Tabela | Campos | Status |
|---|---|---|
| `person` | id, name, ds_document, tp_gender, fl_active | ✅ |
| `person_address` | id, person_id→person, ds_street, ds_number, ds_complement, ds_district, ds_city, ds_state, ds_zip_code | ✅ |
| `person_phone` | id, person_id→person, ds_phone, fl_main | ✅ |
| `person_mail` | id, person_id→person, ds_mail, fl_main | ✅ |
| `company` | id, person_id→person, name | ✅ |
| `client` | id, person_id→person | ✅ |
| `professional` | id, person_id→person | ✅ |
| `supplier` | id, person_id→person | 🔲 |
| `client_interaction` | id, client_id→client, ds_message, created_at | 🔲 |

### Event and Service
| Tabela | Campos | Status |
|---|---|---|
| `event` | id, ds_title, ds_description, dt_start, dt_end, tp_status, tp_event | ✅ |
| `event_service` | id, event_id→event, client_id→client, professional_id→professional, account_receivable_id→account_receivable (hoje 1:1 com `event`, ver seção "Agenda" abaixo) | ✅ |
| `event_message` | id, event_id→event, ds_message | ✅ |
| `event_owner` | id, event_id (UK_EVENT_USER), user_id→users (UK_EVENT_USER) — `fl_super_user` no diagrama marca quem enxerga todos os eventos | ✅ |
| `service` (pré-config) | id, name, ds_description, vl_price — catálogo de serviços citado no roadmap ("Duração padrão por serviço") | 🔲 |

### Financial Module
| Tabela | Campos | Status |
|---|---|---|
| `account_receivable` | id, person_id→person, ds_code, nr_installment (unique), vl_total, vl_balance, da_due, dt_paid, tp_status [pending/paid/late], ds_observation | ✅ |
| `account_receivable_movement` | id, accounts_receivable_id→account_receivable, reversal_account_receivable_movement (auto-FK), vl_movement, vl_discount, dt_movement, tp_payment [money/pix...], tp_movement [payment/reversal], ds_observations | ✅ |
| `accounts_payable` | id, person_id→person, nr_title (unique), nr_installment (unique), vl_total, vl_balance, dt_due, dt_paid, tp_status [pendente/pago/atrasado], ds_observations | 🔲 |
| `accounts_payable_movement` | id, accounts_payable_id→accounts_payable, vl_movement, vl_discount, dt_movement, tp_payment [dinheiro/pix...], ds_observations | 🔲 |

### Order and Product
| Tabela | Campos | Status |
|---|---|---|
| `product` | id, product_category_id→product_category, name, ds_description, vl_price, ds_code, fl_active | 🔲 |
| `product_category` | id, name | 🔲 |
| `stock` | id, product_id→product, nr_quantity, nr_minimun_stock | 🔲 |
| `stock_movement` | id, stock_id→stock, nr_quantity, tp_action [entrada/saída], supplier_id→supplier (entrada), account_playable_id→accounts_payable (entrada) | 🔲 |
| `order` | id, accounts_receivable_id→account_receivable, dt_order, dt_delivery, tp_status [preparando/entregue...], ds_observations | 🔲 |
| `order_product` | id, order_id→order, product_id→product, nr_quantity, vl_price_unit | 🔲 |

### Subscription - Modules
| Tabela | Campos | Status |
|---|---|---|
| `plan` | id, name (basic, pro, ...), vl_price, tp_recurrence (monthly/yearly) | 🔲 |
| `plan_module` | id, plan_id→plan, module_id→module | 🔲 |
| `subscription` | id, company_id→company, plan_id→plan, dt_start, dt_end, tp_status (active/canceled/trial) | 🔲 |
| `subscription_receivable` | id, subscription_id→subscription, account_receivable_id→account_receivable, da_competence | 🔲 |

O ponto de maior alavancagem entre os módulos ainda não implementados (ver seção "Order/Product"
abaixo): unificar `order` e `event_service` numa "comanda" única **antes** de `order` ser implementado
do zero — depois que os dois módulos existirem cada um gerando financeiro separadamente, juntar os
dois fica bem mais caro do que desenhar certo desde o início.

---

## Roadmap técnico

Itens em aberto — nada aqui está bloqueado por código, são decisões/trabalho futuro. Parte é
continuação natural do que já existe (curto prazo); o resto é uma varredura mais ampla do que um
sistema deste tipo costuma precisar pra ficar robusto/consistente em produção de verdade — organizada
por tema pra você validar o que faz sentido pro seu negócio e o que não vale a pena agora. Nenhum
item aqui foi verificado como "impossível" ou "obrigatório" — são candidatos, não decisões.

Cada item tem uma nota de por que importa e, quando relevante, o que já foi conferido no código atual
(pra não sugerir algo que já existe).

### 🔐 Segurança

- [x] **Rate limit em `/auth/login` e `/auth/forgot-password`** — implementado (`RateLimiter`, por
  e-mail, ver `docs/guides/2_authentication.md`). Captcha continua não implementado — só entra em
  pauta se o rate limit por e-mail se mostrar insuficiente na prática.
- [x] **Bloqueio de conta após N tentativas de login falhas** — implementado (5 senhas erradas
  seguidas bloqueiam por 15 minutos, `nr_failed_login_attempts`/`dt_locked_until` em `users`).
- [x] **Invalidar tokens antigos de `user_token` ao emitir um novo** — implementado
  (`UserTokenService.issue` invalida qualquer token não usado do mesmo tipo pro mesmo usuário antes
  de emitir o novo). Verificado ponta a ponta: pedir recuperação de senha duas vezes e confirmar que
  o link mais antigo passa a responder "Link inválido ou expirado".
- [ ] **Revogação de JWT** — hoje o token vale 24h (`jwt.expiration`) sem nenhum mecanismo de
  invalidar antes disso (nem logout revoga — só expira o cookie no navegador que pediu). Se um token
  vazar, fica válido até expirar sozinho. Opções: blocklist em Redis, tokens de vida mais curta +
  refresh token, ou aceitar o trade-off documentado.
- [x] **Política de complexidade de senha** — implementado (`@StrongPassword`, uma regra só
  reaproveitada em `UserRequest`/`ChangePasswordRequest`/`ResetPasswordRequest`: mínimo 8 caracteres,
  com letra e número — deliberadamente sem exigir maiúscula/especial, ver javadoc da anotação).
- [x] **Segredos hardcoded/com default fraco** — `jwt.secret` agora segue o mesmo padrão de
  `mail.config.encryption-key` (`${JWT_SECRET:valor-de-exemplo}`), e os dois têm um guard
  (`SecretConfigurationGuard`) que recusa subir a aplicação — não só loga um aviso — se o valor de
  exemplo ainda estiver ativo num perfil que pareça produção (`prod`/`production`/`staging`). Ainda
  falta o próprio deploy real configurar `JWT_SECRET`/`MAIL_CONFIG_ENCRYPTION_KEY` via cofre de
  segredos quando esse dia chegar — o guard só garante que não vai subir *silenciosamente* sem isso.
- [ ] **Trilha de auditoria de ações sensíveis** — hoje só existe `created_by`/`updated_at`
  (`AbstractAuditableEntity`) por registro; não tem um log dedicado de "quem mudou a permissão de
  quem", "quem resetou a senha de quem", etc. Ver desenho detalhado (tabela `audit_log` genérica +
  soft delete) na seção "Dados & Persistência" abaixo.
- [ ] **2FA/MFA** — não existe hoje. Só relevante dependendo de quão sensível é o dado que o
  sistema guarda pros seus clientes.
- [ ] **CORS explícito** — hoje não há `CorsConfigurationSource` customizado; funciona porque o
  front nunca fala direto com o backend fora do proxy same-origin do Next.js
  (`src/app/api/proxy/[...path]/route.ts`). Só vira necessário no dia em que existir um app mobile,
  uma integração de terceiro, ou qualquer client que chame a API sem passar por esse proxy.
- [x] **Scan de vulnerabilidade de dependências** — `.github/dependabot.yml` nos dois repositórios
  (maven/npm + github-actions), atualizações menores/patch agrupadas num PR só pra reduzir ruído. Só
  passa a abrir PRs de verdade depois que o arquivo for parar no branch padrão de cada repositório no
  GitHub — não é algo que roda localmente.
- [ ] **HTTPS/HSTS** — ambiente atual é só HTTP local (dev). Fora de escopo até existir um domínio
  de produção de verdade, mas vale não esquecer.
- [ ] Um papel "super-admin" cross-tenant pra proteger a config base de e-mail com permissão própria,
  em vez de reaproveitar `MAIL_CONFIG_MANAGE` (a mesma da config da empresa) — trade-off já
  documentado na seção "Limitações conhecidas" abaixo.

### 📊 Observabilidade

- [x] **Log level fixo em DEBUG/TRACE pra qualquer ambiente** — implementado: `application.yml`
  ganhou um segundo documento YAML (`spring.config.activate.on-profile: "prod | production |
  staging"`, mesmo vocabulário de perfil do `SecretConfigurationGuard`) que baixa
  `org.hibernate.SQL`/`org.hibernate.orm.jdbc.bind`/`br.com.clientefacil` pra WARN/WARN/INFO só
  nesses perfis — em dev/docker/test (ou sem perfil ativo) continua tudo como estava. Verificado
  subindo a aplicação de verdade com `SPRING_PROFILES_ACTIVE=prod`: zero linha de SQL/parâmetro no
  log, só INFO.
- [ ] **Correlação de requisição (trace/correlation ID)** — hoje não dá pra seguir uma requisição
  específica através de vários logs (ex: request HTTP → mensagem na fila → e-mail enviado). MDC do
  SLF4J ou Micrometer Tracing resolveriam.
- [x] **Métricas (básico)** — `management.endpoints.web.exposure.include:
  health,info,metrics,circuitbreakers,circuitbreakerevents` ativado, protegido por autoridade nova
  (`SYSTEM_METRICS_VIEW`, ver `SecurityConfig`) — nada em `/actuator/**` é público, nem `/health`
  (nenhum load balancer real depende disso hoje; o healthcheck do docker-compose é TCP puro).
  Verificado ao vivo: 401 sem token, 200 com token de admin, métricas de verdade (Hikari, executor,
  disco), estado do circuit breaker de e-mail (ver item de mensageria abaixo). Ainda falta o que pede
  Prometheus/Grafana de verdade (profundidade de fila, latência por endpoint, taxa de erro, série
  histórica) — isso continua precisando de Micrometer + um backend de métrica de verdade, não é só
  configuração.
- [ ] **Alerta além do e-mail de dead-letter** — hoje o único alerta automático é o e-mail/notificação
  de DLQ (Parte 7 do guia de mensageria). Um erro não tratado em qualquer outro lugar do sistema só
  aparece no `logs/application.log` — sem Sentry (ou similar) rastreando exceções em produção.
- [ ] **Dashboard de profundidade de fila / consumer lag do RabbitMQ** — hoje só dá pra ver isso
  manualmente no painel `http://localhost:15672`.

### 🚀 Infraestrutura & Deploy

- [ ] **Imagens Docker são só de desenvolvimento** — o `backend/Dockerfile` usa a imagem `maven`
  completa e roda `mvn spring-boot:run` (com devtools, hot-reload); o `frontend/Dockerfile` roda
  `pnpm dev`. Nenhum dos dois é uma imagem de produção (build multi-stage, JRE mínimo, usuário
  não-root, `next build` + `next start` em vez do dev server). Bloqueador real pra qualquer deploy
  de verdade.
- [x] **CI rodando a suíte em cada push/PR** — `.github/workflows/ci.yml` (nos dois repositórios)
  agora roda a suíte a cada push/PR (backend: `./mvnw test` com Postgres+RabbitMQ como `services` do
  job, mesmas imagens/credenciais do `docker-compose.yml`; front: lint + `tsc --noEmit` + `pnpm test`).
  **Deploy automatizado continua não existindo** — falta ainda além disso: imagem de produção (ver
  item acima) e o pipeline de deploy em si.
- [ ] **Separação de ambientes** (dev/staging/produção) — hoje só existe o `docker-compose.yml` de
  desenvolvimento, com um `application.yml` único.
- [ ] **Backup automatizado do Postgres** — hoje o banco é só um volume Docker local
  (`../cliente-facil-database`, bind mount), sem rotina de backup/restore documentada ou testada.
- [ ] **Gestão de segredos** (Vault, AWS Secrets Manager, Doppler, etc) — hoje segredos são só
  variáveis de ambiente lidas direto pelo Spring, sem rotação nem cofre centralizado. Só relevante se
  o projeto for pra um provedor de nuvem de verdade.

### 🗄️ Dados & Persistência

- [x] **Retenção/limpeza de `notification`, `notification_dead_letter`, `user_token`** —
  implementado (`DataRetentionService`, `@Scheduled` diário às 3h, dias configuráveis via
  `app.data-retention.*`): notificação lida há mais de 365 dias, dead-letter resolvido há mais de 90,
  token usado/expirado há mais de 30 — nunca algo ainda pendente de ação. Roda sem usuário
  autenticado (job em background), então alcança todas as empresas de propósito.
- [ ] **LGPD**: direito ao esquecimento (anonimizar/excluir dados de uma pessoa/empresa que pediu),
  portabilidade de dados (exportar tudo que o sistema guarda sobre um usuário). Relevante assim que o
  sistema tiver usuários/empresas reais e não for só uso interno.

#### Soft delete (✅ Fase A) + tabela de auditoria genérica (Fase B, pendente)

Recomendado com convicção maior que os outros itens desta lista, especificamente pra este sistema:
já existe dado financeiro (`AccountReceivable`/`AccountReceivableMovement`), é multi-tenant B2B (cada
empresa eventualmente vai querer/precisar de histórico), e o projeto está pré-produção — o momento
mais barato de adicionar isso é agora (depois, com dado real em produção, vira migração + backfill,
não só desenho). Soft delete (Fase A) já implementado; `audit_log` (Fase B) deliberadamente adiada —
capturar `old_value`/`new_value` corretamente exige um listener nativo do Hibernate
(`PreInsertEventListener`/`PreUpdateEventListener`/`PreDeleteEventListener`, sem precedente no
projeto) pra evitar um SELECT extra por escrita, peça grande o bastante pra não misturar com a
mudança de schema da Fase A.

- [x] **Soft delete nas entidades de negócio** — implementado (Fase A, ver
  `docs/guides/5_soft-delete.md`): `@SQLDelete` (reescreve o `DELETE` que `repository.delete(entity)`
  gerava pra um `UPDATE ... SET deleted_at = now()`) + `@SQLRestriction("deleted_at IS NULL")` em
  toda leitura, automático — zero mudança nos services que já chamavam `repository.delete(...)`.
  Escolhido em vez do `@Filter` que o `tenantFilter` já usa porque `@Filter` precisa ser ligado por
  sessão e `TenantScopedRepositoryImpl.findById()` já contorna esse mecanismo com uma `CriteriaQuery`
  manual — um soft-delete baseado em `@Filter` teria o mesmo buraco; `@SQLRestriction` não, por ser
  estático. Pegadinha real encontrada testando ao vivo: `@SQLRestriction` declarado só numa
  `@MappedSuperclass` não é herdado pelas subclasses nesta versão do Hibernate (6.4.4.Final) — precisou
  ser repetido em cada entidade concreta (documentado no guia).
  - **Onde está**: `Client`, `Company`, `Person`, `Professional`, `Event`, `AccountReceivable`/
    `AccountReceivableMovement`, `User`.
  - **Onde não está** (deliberado): `Notification`/`NotificationDeadLetter`/`UserToken` (já têm
    retenção automática, ver item acima), `MailConfig`/`Profile`/`Resource`/`Module`/
    `ProfilePermission` (configuração, não histórico).
  - Índices únicos (`User.email`, `Client`/`Professional` por pessoa+empresa,
    `AccountReceivable` por código+parcela+empresa) convertidos pra parciais
    (`WHERE deleted_at IS NULL`) — senão um registro soft-deletado bloquearia pra sempre recriar
    outro com a mesma chave.
  - **Evolução**: os `*DeletionValidator` manuais (um por entidade, hard-coded) foram substituídos por
    `core/hibernate/ForeignKeyDeletionGuard.java` — `PreDeleteEventListener` genérico que lê
    `ON DELETE RESTRICT`/`CASCADE` direto do `information_schema`, sem código por entidade. Efeito
    consciente: bloqueia excluir `User`/`Company` quase sempre (`created_by`/`updated_by`/`company_id`
    aparecem em quase toda tabela) — resolvido com um flag `fl_active` novo em `users`/`company`
    (mesmo papel que `Person.flActive` já tinha), a via normal de "remover" um dos dois no dia a dia.
    Também resolve `CASCADE` (que tinha o mesmo problema do `RESTRICT` — parou de disparar sozinho):
    cascateia soft delete/remoção física recursivamente pela árvore de FKs. Ver guia pro mecanismo
    completo, incluindo o tópico "Desempenho" (índices deliberadamente não criados por ora).
  - De quebra: `GlobalExceptionHandler` ganhou handlers dedicados pra `BusinessException` (409) e
    `ResourceNotFoundException` (404) — antes os dois caíam no catch-all de 500 — e pra
    `TransactionSystemException` (desembrulha a causa, necessário porque o guard lança de dentro de um
    listener que só roda no commit da transação).
  - Tentativas de eliminar `@SQLDelete`/`@SQLRestriction` repetidos nas 8 entidades: meta-anotação e
    `@SoftDelete` nativo do Hibernate (checado até a versão mais recente, 7.4) esbarraram em
    limitações reais e foram revertidas; trigger no Postgres foi descartado por decisão consciente
    (duplicaria a fonte de verdade entre banco e código). **`@SQLDelete` acabou eliminado mesmo
    assim**: o `ForeignKeyDeletionGuard` (já existia pra RESTRICT/CASCADE) passou a fazer o `UPDATE`
    da própria entidade e vetar o `DELETE` físico do Hibernate — sem usar a anotação `@SoftDelete`
    nativa, então sem a trava de `LAZY` que a derrubou. Só `@SQLRestriction` continua por entidade —
    tentativa de automatizar também esse lado via `Interceptor`/`StatementInspector` (reescrita de SQL
    texto) foi pesquisada e descartada: sem precedente de uso seguro na comunidade, e o modo de falha
    (vazamento silencioso de dado excluído) é pior que o de esquecer uma anotação. Duas redes de
    segurança em vez disso: `SoftDeletableEntitiesTest` garante via reflection que nenhuma entidade
    soft-deletável fica sem `@SQLRestriction` correto nem reintroduz `@SQLDelete`;
    `SoftDeleteBehaviorIntegrationTest` prova o comportamento real contra Postgres (cria + deleta cada
    uma das 8 entidades, confirma que some da leitura mas continua existindo fisicamente) — não
    depende de qual mecanismo está por trás. Ver guia pro relato completo.

- [ ] **Tabela `audit_log` genérica** (`table`, `record_id`, `old_value`, `new_value`), em vez de
  Hibernate Envers (que cria uma tabela `_AUD` por entidade auditada — mais mágico, mais pesado de
  consultar; foge do estilo explícito que o resto do projeto já segue). Desenho sugerido:
  ```sql
  CREATE TABLE audit_log (
      id           BIGSERIAL PRIMARY KEY,
      ds_table     VARCHAR(100) NOT NULL,   -- "client", "account_receivable", etc.
      nr_record_id BIGINT       NOT NULL,
      tp_operation VARCHAR(10)  NOT NULL,   -- INSERT/UPDATE/DELETE
      ds_old_value JSONB,                   -- null em INSERT
      ds_new_value JSONB,                   -- null em DELETE
      company_id   BIGINT,
      created_by   BIGINT,
      created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  );
  ```
  Encaixe técnico: um `EntityListener` genérico (`@PrePersist`/`@PreUpdate`/`@PreRemove`), no mesmo
  espírito do `AuditingEntityListener` que já popula `created_by`/`updated_by` hoje — reaproveitando
  o mesmo `AuditorAware` (`JpaAuditingConfig`) que já resolve "quem é o usuário atual". `JSONB`
  guarda o snapshot inteiro sem precisar de uma coluna por campo, e ainda dá pra consultar
  (`old_value->>'email'`) quando precisar.
  - **Onde vale mais**: as mesmas entidades de negócio acima, **mais** `Profile`/`ProfilePermission`
    (rastrear "quem mudou a permissão de quem" — complementa a trilha de auditoria de segurança
    citada acima).
  - Combinado com soft delete, o `audit_log` guarda a exclusão como mais um evento
    (`tp_operation = 'DELETE'`, `old_value` = snapshot antes de excluir) — juntas, as duas peças
    respondem "o que existia, quem mudou o quê e quando, e quem excluiu" sem precisar recorrer a
    backup de banco pra investigar um incidente.
  - **Trade-off**: mais uma tabela que cresce indefinidamente — soma com o item de retenção/limpeza
    logo acima (definir por quanto tempo guardar auditoria antes de arquivar/purgar).

### 📨 Mensageria & confiabilidade (complementa `docs/guides/1_messaging-and-websocket.md`)

- [x] **Retenção de dead-letters já resolvidos** — implementado (`DataRetentionService.
  purgeOldResolvedDeadLetters`, mesma rotina `@Scheduled` diária que já limpa `notification`/
  `user_token`): dead-letter resolvido há mais de `deadLetterRetentionDays` dias é removido.
- [x] **Circuit breaker pro SMTP** — implementado (Resilience4j, `docs/guides/4_circuit-breaker-smtp.md`):
  um circuito por empresa/config (`email-smtp-<companyId|base>`) em volta do envio real no
  `EmailListener`. Depois de falhas suficientes (config em `application.yml`), passa a rejeitar novas
  tentativas na hora (`CallNotPermittedException`) em vez de gastar um timeout de rede a cada
  mensagem — retry+DLQ da Parte 6 continua intacto, só decide *se* vale tentar, não *quantas vezes*.
  Timeout explícito de SMTP (5s) também adicionado, sem o qual uma chamada real podia pendurar bem
  mais que isso. Observável em `/actuator/circuitbreakers`/`circuitbreakerevents`.
- [ ] **Confirmação de entrega de e-mail** — hoje é fire-and-forget (`EmailService.sendTemplated`);
  não há tratamento de bounce/reject do provedor SMTP, nem registro de "foi entregue de verdade".
- [ ] **Preferências de notificação por usuário** — hoje todo usuário recebe toda notificação
  endereçada a ele, sem opção de opt-out por tipo ou de agrupar em digest (resumo periódico em vez de
  um e-mail por evento).
- [ ] **Reaproveitar o padrão fila + STOMP pra outros canais** — o mesmo desenho de
  `NotificationPublisher`/`NotificationListener`/`useStompSubscription` (ver Parte 5 do guia de
  mensageria) já foi pensado pra generalizar pra SMS, push notification, ou qualquer evento
  assíncrono futuro (ex: exportação de PDF) sem mudar a infraestrutura de WebSocket.

### 🧪 Testes (complementa `docs/guides/3_testing-strategy.md`)

- [ ] **Testes de integração com banco de verdade** — a suíte atual (47 testes) é só unitária, com
  Mockito, sem tocar banco. Testcontainers (Postgres real, efêmero, por execução) cobriria coisas que
  só aparecem com JPA/Hibernate de verdade: mapeamento de coluna, o `tenantFilter` do Hibernate,
  constraints de banco (índices únicos parciais do `mail_config`, por exemplo).
- [x] **CI rodando a suíte em cada PR** — ver "🚀 Infraestrutura & Deploy" acima.
- [x] **Teste de burst do pipeline RabbitMQ (parcial)** — `scripts/rabbitmq-burst-test.sh`: publica
  um volume de mensagens direto na fila (bypassando a API HTTP), com uma fração falhando de
  propósito, e confirma que toda mensagem termina em exatamente um lugar (sucesso ou dead-letter),
  sem perda. Deliberadamente **não** é benchmark de throughput/latência (p95/p99) — sem número-alvo
  definido pro negócio, medir contra nada seria teatro; isso valida robustez, não performance.
  **Achado real**: o listener de notificação roda com concorrência 1 (nenhum
  `spring.rabbitmq.listener.simple.concurrency` configurado) — cada mensagem que falha trava essa
  única thread pelos ~3s inteiros do backoff de retry antes de desistir. 150 mensagens com 20% de
  falha (30 falhando) levaram ~100s pra drenar (~1,5 msg/s sob essa carga). Ainda não é um problema
  pro volume esperado hoje, mas é o primeiro limite conhecido de throughput do sistema — vale
  revisitar (`concurrency`/`max-concurrency` no listener) se o volume real de notificações crescer.
- [ ] **Teste de carga/performance de verdade** (throughput sustentado, latência p95/p99, muitas
  conexões WebSocket simultâneas) — ainda não medido. Só faz sentido com um número-alvo definido pro
  negócio (ex: "aguentar X notificações/min"); sem isso, seria benchmark sem referência.

### 📘 API & Documentação

- [ ] **Paginação real em `GET /api/v1/notifications`** — hoje retorna só as últimas 50 (ver seção
  "Limitações conhecidas" abaixo); falta se um dia existir uma tela de histórico completo.
- [ ] **Política de versionamento/depreciação de API** — hoje só existe `/api/v1/`, sem nenhuma
  estratégia documentada pro dia em que um `/v2` for necessário (quanto tempo o v1 continua no ar,
  como avisar consumidores, etc).
- [ ] **Revisão de completude do Swagger/OpenAPI** — o `@Operation(summary = ...)` está presente na
  maioria dos endpoints, mas vale uma passada conferindo se todos os controllers têm descrição e
  exemplos de payload consistentes.

### 🖥️ Frontend

Itens exclusivos de front (lint, a11y, i18n, error boundary, performance, testes E2E, etc.) ficam em
`docs/ROADMAP.md` do repositório `cliente-facil-next` — cobre só o que é específico do front, sem
repetir nada deste documento.

### 🏢 Negócio / Multi-tenant

- [ ] **Self-service signup de empresa** — hoje toda empresa/usuário é criada por um admin já
  existente (ver `docs/product/1_business-rules.md`); sem um funil de "criar minha conta" público,
  todo cliente novo depende de alguém internamente cadastrar.
- [ ] **Cotas por empresa** (limite de usuários, notificações/mês, etc) — só relevante se o modelo de
  negócio for baseado em planos/tiers pagos.
- [ ] **Billing/assinatura** — nenhuma integração de cobrança existe hoje; só relevante se o produto
  for comercializado como SaaS pago.

### 🛠️ Qualidade de código / DX

- [x] **Formatador/linter no backend** — Spotless configurado (`removeUnusedImports`, indentação,
  trailing whitespace, newline final — deliberadamente sem reformatter opinativo tipo
  google-java-format, que geraria um diff gigante não relacionado a nada). Ligado à fase `validate`,
  então já roda em todo `mvn compile`/`test`/CI sem precisar de step separado. Codebase inteira já
  passada por `spotless:apply` (99 arquivos, diff pequeno — a maioria só newline final faltando).
- [ ] **Pre-commit hooks** (lint, format, testes rápidos antes do commit) — não existe em nenhum dos
  dois repositórios.
- [ ] **Reavaliar editar nome/e-mail do próprio usuário** na tela de conta — adiado por decisão (ver
  `docs/product/1_business-rules.md`), não descartado; revisitar se a demanda aparecer.

---

## Regras de negócio para módulos futuros

Levantamento pros módulos já arquitetados mas ainda não (ou só parcialmente) implementados:
**Subscription**, **Order/Product**, e as partes de **Event/EventService** e **Financial** que ainda
não existem. O sistema descrito — agenda de serviços que gera financeiro, venda de produto que
também gera financeiro, tudo por trás de uma assinatura SaaS — é um desenho muito próximo do que
softwares como Trinks, Booksy, Fresha, Vagaro e Mindbody já resolveram pra esse nicho (salão,
clínica, barbearia, estúdio, consultório, personal trainer). Esta seção puxa os padrões que esse
tipo de sistema **amplamente adota**, cruzados com o que já existe no código — pra você validar o que
faz sentido pro seu negócio, não pra implementar tudo de uma vez.

Marcações usadas: **[JÁ EXISTE]** quando conferi que a base já está no código (evita redesenhar o que
já foi decidido); o resto é ideia nova.

### 📅 Agenda (`Event`/`EventService`)

**[JÁ EXISTE]** — vale mais maduro do que um primeiro módulo costuma ser: `Event` já tem ciclo de
status completo (`SCHEDULED`/`CONFIRMED`/`IN_PROGRESS`/`COMPLETED`/`CANCELLED`/`MISSED` — já modela
no-show), `EventTypeEnum` distingue atendimento de cliente vs. compromisso pessoal
(`APPOINTMENT`/`SERVICE`/`PERSONAL`), e `EventService` já vincula evento → cliente → profissional →
`AccountReceivable`.

- [x] **Conflito de horário (double-booking)** — implementado (`EventScheduleValidator`, checagem via
  `EventServiceRepository.existsOverlapping`): ao criar/editar um `Event` do tipo `SERVICE`, valida que
  o profissional não tem outro evento não cancelado sobrepondo o mesmo intervalo (`dtStart`/`dtEnd`),
  antes de qualquer persistência. Eventos `CANCELLED` não bloqueiam o horário; um evento em edição não
  conflita consigo mesmo (`excludeEventId`). Front não precisou de nenhuma mudança: o pipeline
  genérico de erro já existente (`http.util.ts` lê `data.message` do 409 → `ApiError` →
  `useApiMutation.onError` → toast) já propaga a mensagem de conflito automaticamente pro formulário
  de evento. Verificado ao vivo contra a API real: criar evento sobreposto pro mesmo profissional
  retorna 409 com a mensagem amigável; horários encostados (fim de um = início do outro) não
  conflitam; editar um evento mantendo seu próprio horário não dispara falso positivo.
- [ ] **Horário de funcionamento** (da empresa e por profissional) + **bloqueio de agenda** (férias,
  folga, atestado) — pra não deixar agendar fora do expediente.
- [ ] **Buffer entre atendimentos** (tempo de preparo/limpeza entre um evento e outro).
- [ ] **Múltiplos serviços por atendimento ("comanda")** — hoje `EventService` é **1:1** com `Event`
  (um agendamento = um serviço = um profissional = um financeiro). Praticamente todo concorrente
  permite mais de um serviço na mesma visita (ex: corte + barba, cada um podendo ter um profissional
  diferente), fechando numa cobrança só. Se isso importar pro seu negócio, vale desenhar antes de
  `Order`/`Financial` crescerem em cima do modelo atual — ver seção "Order/Product" abaixo.
- [ ] **Duração padrão por serviço** — um catálogo de serviços (nome, duração, preço padrão) calcularia
  `dtEnd` automaticamente a partir de `dtStart` + duração. Hoje não encontrei uma entidade "Serviço"
  catalogável — `EventService` parece ser só o vínculo do agendamento em si, não um catálogo.
- [ ] **Recorrência** (repetir toda semana/mês no mesmo horário) — comum pra personal trainer,
  fisioterapia, etc.
- [ ] **Lista de espera** quando não há horário disponível no dia desejado.
- [ ] **Confirmação de agendamento pelo cliente, sem login** (link com token) — reaproveita
  exatamente o padrão de token de uso único já implementado pra confirmação de e-mail/reset de senha
  (`UserTokenService`, ver `docs/guides/2_authentication.md`).
- [ ] **Lembrete automático** (D-1, H-1) — reaproveita a fila+e-mail já pronta (`EmailService`); só
  precisaria de um job agendado (`@Scheduled`) publicando o lembrete perto do horário.
- [ ] **Política de cancelamento/no-show** (prazo mínimo pra cancelar sem taxa, taxa de no-show).
- [ ] **Comissão do profissional configurável por serviço** — % ou valor fixo, aplicado quando o
  financeiro do evento é pago.
- [ ] **Agendamento self-service pelo cliente** (uma tela pública, sem passar pela administração) —
  fora do escopo administrativo atual; grande parte da atração desse tipo de sistema pro dono do
  negócio é o cliente final poder marcar sozinho, 24/7.

### 💰 Financeiro (`AccountReceivable`/`AccountReceivableMovement`)

**[JÁ EXISTE]** — mais maduro do que uma primeira versão costuma ser: parcelamento (`nrInstallment`),
saldo (`vlBalance`), vencimento (`daDue`), status completo (`PENDING`/`PARTIALLY_PAID`/`PAID`/
`OVERDUE`/`CANCELLED`), múltiplas formas de pagamento já modeladas
(`CASH`/`PIX`/`DEBIT_CARD`/`CREDIT_CARD`/`BANK_TRANSFER`/`CHECK`/`BOLETO`/`OTHER`), e estorno
(`AccountReceivableMovement.reversalAccountReceivableMovement`, auto-referência pro movimento
revertido).

- [ ] **Contas a pagar** — hoje só existe o lado "a receber". Despesas (aluguel, fornecedor, comissão
  a pagar pro profissional) não têm onde entrar; é a metade que falta pra um financeiro completo.
- [ ] **Caixa** (abertura/fechamento diário, resumo de entradas por forma de pagamento) — padrão
  "PDV" comum em quem atende presencialmente.
- [ ] **Comissão de profissional calculada automaticamente** — quando o `AccountReceivable` do
  evento é pago, gerar (ou ao menos calcular) o valor de comissão daquele profissional — vira insumo
  direto de "Contas a pagar" acima.
- [ ] **Pacotes pré-pagos** (cliente compra 10 sessões de uma vez, cada agendamento "consome" uma) —
  padrão muito comum em salão/academia/clínica de estética. Vira uma conta a receber paga
  antecipadamente, com "saldo de sessões" em vez de saldo em dinheiro.
- [ ] **Vale-presente (gift card)** e **cupom de desconto**.
- [ ] **Emissão de nota fiscal** (NFS-e pra serviço, NF-e pra produto) — bem relevante no Brasil,
  normalmente via integração com um provedor terceiro (Focus NFe, NFE.io, PlugNotas).
- [ ] **Régua de cobrança automática** (notificar o cliente perto do vencimento, e de novo se
  vencer) — reaproveita a mesma infra de notificação já pronta.
- [ ] **Conciliação bancária** (importar extrato, bater com os movimentos) — mais avançado, só
  relevante se o volume justificar.
- [ ] Consertar `AccountReceivable.dsObservation`, hoje tipado `LocalDateTime`
  (`entity/AccountReceivable.java`) — quase certamente devia ser `String`, comparando com
  `AccountReceivableMovement.dsObservations` (plural, tipado `String` corretamente ali). Não é bem
  "regra de negócio", é um bug que apareceu revisando o código pra este levantamento.

### 🛒 Order/Product (ainda não implementado)

- [ ] **Controle de estoque** (quantidade atual, alerta de estoque mínimo, custo médio).
- [ ] **Movimentação de estoque** (entrada por compra, saída por venda, ajuste/perda) — mesmo
  espírito de `AccountReceivableMovement`: um `Product` tem saldo, e cada `ProductMovement` altera
  esse saldo com um motivo.
- [ ] **Produto como insumo vs. produto vendido** — ex: tinta de cabelo consumida durante o
  atendimento (desconta do estoque, não gera linha de venda pro cliente) vs. produto comprado pelo
  cliente pra levar pra casa (desconta do estoque **e** gera venda). São dois fluxos diferentes que
  compartilham o mesmo cadastro de produto.
- [ ] **Comissão de venda de produto** — separada da comissão de serviço.
- [ ] **Preço de custo vs. preço de venda** (margem).
- [ ] **Fornecedor** (`Supplier`) — módulo natural de existir junto com estoque/compra.
- [ ] **Kits/combos de produtos**.
- [ ] **Unificar `Order` + `EventService` numa "comanda"/venda única** — o padrão de mercado (Trinks,
  Booksy, etc): o cliente chega, o profissional realiza o(s) serviço(s), a recepção adiciona
  produto(s) levados na mesma visita, e tudo fecha numa cobrança só (um `AccountReceivable`, várias
  linhas). Hoje `EventService` gera seu próprio financeiro 1:1; se "comanda" fizer sentido pro seu
  negócio, vale desenhar `Order` já pensando nisso, em vez de `Order` e `EventService` cada um
  gerando financeiro pro seu próprio lado, separadamente.
  - **Fluxo sugerido (exemplo de uso)**: cliente termina o corte e no balcão pede também uma pomada
    e um boné. O atendente usa uma tela de produtos (cadastro de produto + grupo/categoria — já
    mapeado no diagrama como `product`/`product_category`) e vai adicionando itens a um "carrinho".
    Dois pontos de entrada fariam sentido: uma tela de produtos dedicada, ou — mais intuitivo pro
    balcão, sem trocar de tela — um modal de produtos aberto direto de dentro do próprio
    evento/agendamento. Ao finalizar o atendimento (`EventService`), uma opção "Incluir produtos do
    carrinho" junta os itens à mesma comanda: viram `order_product` de uma `Order` vinculada ao
    mesmo `AccountReceivable` do evento, numa cobrança só. O "carrinho" em si provavelmente não
    precisa de tabela própria — é estado efêmero (client-side ou de sessão) até a finalização; só o
    que for confirmado nesse momento vira `order`/`order_product` de verdade. Ainda é ideia, não
    desenho fechado: o carrinho também poderia existir sem estar ligado a nenhum evento (venda de
    produto avulsa, sem atendimento), caso em que só geraria a `Order`/`AccountReceivable` sozinha.

### 👤 Person/Client/Professional

**[JÁ EXISTE]** — bom desenho de base: `Client` e `Professional` são "papéis" sobre `Person` (FK
simples), o que já permite a mesma pessoa ser cliente **e** profissional sem duplicar cadastro.

- [ ] **Histórico de atendimentos do cliente** — mais relatório que modelo novo, já dá pra montar em
  cima de `Event`/`EventService` como estão.
- [ ] **Preferências/observações do cliente** (profissional preferido, alergias, observação da
  última visita).
- [ ] **Ficha de anamnese/prontuário** — se o nicho for estética/saúde, costuma ser esperado.
- [ ] **Programa de fidelidade** (pontos por valor gasto, cashback).
- [ ] **Campanha de aniversário automática** — reaproveita e-mail/notificação já prontos.
- [ ] **Bloqueio de novo agendamento pra cliente inadimplente** — regra de negócio ligando
  Financeiro ↔ Agenda (checagem parecida com a de `AuthService.login` checar e-mail confirmado antes
  de liberar login: aqui seria "tem conta vencida?" antes de liberar novo agendamento).
- [ ] **Especialidades por profissional** (nem todo profissional faz todo serviço) — não encontrei
  vínculo entre `Professional` e um catálogo de serviços; depende de "Duração padrão por serviço"
  (seção Agenda) existir primeiro.
- [ ] **Múltiplos profissionais no mesmo atendimento** (ex: cabeleireiro + auxiliar) — hoje
  `EventService` só linka um `Professional`.

### 🏢 Company

- [ ] **Múltiplas unidades/filiais por empresa** — `Company` hoje parece ser só "a empresa", sem um
  nível "unidade" abaixo. Uma rede com mais de um endereço físico precisaria disso pra separar
  agenda/estoque/caixa por unidade.
- [ ] **Horário de funcionamento por unidade** (depende do item acima).
- [ ] **Catálogo de serviços da empresa** (nome, duração padrão, preço, categoria) — pré-requisito
  pra várias ideias da seção Agenda (duração automática, especialidade por profissional).

### 💳 Subscription

Diferente dos outros módulos: isso não é sobre o negócio do seu cliente (dono do salão/clínica) — é
sobre como **você** cobra ele por usar o sistema.

- [ ] **Planos com limites** (nº de usuários, nº de agendamentos/mês, nº de unidades).
- [ ] **Trial gratuito** com prazo definido.
- [ ] **Cobrança recorrente via gateway** — Stripe, Pagar.me, Asaas, Iugu (os últimos três com bom
  suporte a PIX/boleto, comuns no mercado brasileiro de SaaS).
- [ ] **Bloqueio de acesso por inadimplência da assinatura** — mais um portão no
  `AuthService.login`, no mesmo espírito do portão de e-mail confirmado que já existe: "a empresa
  está em dia com a assinatura?" antes de liberar login.
- [ ] **Upgrade/downgrade de plano** com cobrança pró-rata.
- [ ] **Add-ons pagos à parte** (ex: WhatsApp, emissão de nota fiscal) — módulos do próprio roadmap
  aqui virando features pagas incrementais.

### 🔔 Notificações & Comunicação

Infra já pronta (RabbitMQ + STOMP + e-mail, ver `docs/guides/1_messaging-and-websocket.md`); falta a
camada de regra de negócio disparando por cima dela:

- [ ] Lembrete de agendamento (D-1, H-1).
- [ ] Confirmação de agendamento pelo cliente sem precisar logar (mesmo padrão de token de
  `confirm-email`/`reset-password`).
- [ ] Campanha de aniversário, régua de cobrança (já citados acima — reunidos aqui porque são todos
  "a mesma infra, gatilhos de negócio diferentes").
- [ ] **Integração com WhatsApp** — o canal mais usado nesse nicho no Brasil; Trinks/Booksy vivem
  disso pra confirmação/lembrete (taxa de abertura muito maior que e-mail). Encaixaria no mesmo
  padrão de fila+listener já usado pro e-mail (`EmailPublisher`/`EmailListener`), só trocando o "quem
  entrega" no fim — arquitetura já pronta pra isso, só falta o canal novo.

### 📊 Relatórios / BI

- [ ] Faturamento por período/profissional/serviço/produto.
- [ ] Taxa de ocupação da agenda.
- [ ] Ticket médio.
- [ ] Taxa de retenção/recorrência de cliente.
- [ ] Serviços/produtos mais vendidos.
- [ ] Comissão a pagar por profissional, por período (consome "Contas a pagar" + "Comissão
  configurável", ambos na seção Financeiro).

### 🔐 Permissões

- [ ] **Permissão por unidade**, não só por empresa — só relevante se "múltiplas unidades" avançar.
- [ ] **Perfil "profissional" só enxerga a própria agenda** — hoje `EVENT_VIEW` parece ser
  tudo-ou-nada por empresa (mesmo padrão de permissão simples que o resto do sistema usa); um
  profissional comum normalmente não deveria ver a agenda de outro profissional da mesma empresa.

### Como esses módulos se conectam

Pra visualizar o fluxo ponta a ponta que o sistema já mira, com o que falta grifado:

```
Cliente agenda (Event) ──► Profissional atende (EventService) ──► gera Financeiro (AccountReceivable)
                                      │                                      │
                                      ▼                                      ▼
                          [FALTA] comanda com produtos          [JÁ EXISTE] parcelamento,
                          (Order + EventService juntos)         múltiplas formas de pagamento,
                                      │                          estorno
                                      ▼
                          [FALTA] baixa de estoque (Product)

Empresa paga assinatura (Subscription) ──► [FALTA] acesso ao sistema condicionado a isso
```

O ponto de maior alavancagem, se tivesse que escolher um: **unificar `Order` e `EventService` numa
comanda só antes de `Order` ser implementado do zero** — depois que os dois módulos existirem cada um
gerando financeiro separadamente, juntar os dois fica bem mais caro do que desenhar certo desde o
início.

---

## Limitações conhecidas (trade-offs aceitos)

Trade-offs aceitos conscientemente — cada um tem uma razão documentada, não é esquecimento. Nenhuma
delas impede o uso real dos recursos — a lista existe pra deixar claro o que foi decidido
conscientemente, pra revisitar se/quando importar.

### Mensageria / tempo real / e-mail

- **`GET /api/v1/notifications` não pagina** — retorna só as últimas 50. Suficiente pro sino de
  notificações (modal simples); um histórico completo precisaria do padrão de busca paginada já usado
  em outras entidades do projeto. Fica pra quando existir uma tela de histórico de verdade.
- **`ws-ticket` não é atado à sessão/IP de quem pediu** — qualquer processo que capture o ticket
  dentro da janela de 30s consegue usá-lo uma vez. Suficiente para o risco atual do projeto.
- **Config base de e-mail é protegida pela mesma permissão da config da empresa**
  (`MAIL_CONFIG_MANAGE`) — não existe um papel "super-admin" cross-tenant no projeto ainda; qualquer
  empresa com essa permissão consegue alterar o envio de e-mails do sistema inteiro.
- **Configurar um SMTP real** (base ou por empresa) é uma ação do usuário, não do código — em dev,
  tudo aponta pro MailHog por padrão.

### Templates de e-mail

- **`EmailTemplateVariablesTest` extrai variáveis do `.html` por regex simples**, não parseia OGNL/
  SpringEL de verdade — pega `${nome}` e o primeiro identificador de `${nome.propriedade}`. Suficiente
  pros templates atuais (todos variáveis simples); um template que precisasse de navegação de
  propriedade aninhada (`${objeto.campo}`) ainda funcionaria em produção normalmente, só o teste
  compararia pelo nome de `objeto`, não pelo caminho completo.

### Autenticação

- **Sem rate limit** em `/auth/forgot-password` nem `/auth/login` — alguém pode tentar várias vezes
  seguidas sem bloqueio. Vale revisitar antes de produção de verdade (rate limit por IP/e-mail,
  captcha).
- **Tokens antigos não são invalidados ao emitir um novo** — pedir recuperação de senha duas vezes
  deixa dois links válidos simultaneamente (cada um ainda de uso único, com TTL curto). Não é um
  risco alto dado o TTL de 1h.
- **Editar o próprio e-mail/nome continua fora da tela de conta** — decisão explícita, não limitação
  técnica: `User.name` é só um rótulo de conta (o dado real vive em `Person`); e-mail é o
  identificador de login e trocar exigiria um fluxo de reconfirmação. Avaliado e adiado por ora.

### Soft delete (ver `docs/guides/5_soft-delete.md`)

- **A proteção referencial hoje é genérica, via `ForeignKeyDeletionGuard`** (lê `ON DELETE RESTRICT`/
  `CASCADE` do `information_schema`, não código hard-coded por entidade) — substituiu os 4 validators
  manuais de uma rodada anterior. Efeito: **excluir um `User` ou uma `Company` fica bloqueado quase
  sempre** (`created_by`/`updated_by` e `company_id` aparecem em quase toda tabela) — de propósito,
  decisão consciente pra um sistema com dado financeiro. A via normal de "remover" um dos dois do dia
  a dia é o flag `fl_active` (`users`/`company`), não a exclusão.
- **Sem índice dedicado nas colunas de FK que o guard consulta, por enquanto** — decisão consciente
  (volume de exclusões é baixo, ver tópico "Desempenho" no guia) a revisitar se isso virar gargalo de
  verdade. `company_id` em si (usado também pelo `tenantFilter`, não só pelo guard) tem o mesmo gap,
  mas é maior/anterior a esta funcionalidade.
- **`AccountReceivable`/`AccountReceivableMovement` ganharam a coluna/mecanismo de soft delete, mas
  não têm `service`/`controller` próprio ainda** — hoje só são alcançáveis via `EventService`, que já
  usa `AccountReceivableValidator`. Vira relevante quando um CRUD dedicado for construído (ver seção
  "Financeiro" acima) — nesse dia, reaproveitar o validador existente.

### Geral

- **`useHasAuthority` é só uma camada de UX**, nunca a fonte de verdade de permissão — esconder um
  botão no client não impede ninguém com acesso a `curl`/DevTools de chamar o endpoint direto. A
  proteção real é sempre o `@PreAuthorize` no backend.
