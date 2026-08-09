# Roadmap / próximos passos

Itens em aberto — nada aqui está bloqueado por código, são decisões/trabalho futuro. Parte é
continuação natural do que já existe (curto prazo); o resto é uma varredura mais ampla do que um
sistema deste tipo costuma precisar pra ficar robusto/consistente em produção de verdade — organizada
por tema pra você validar o que faz sentido pro seu negócio e o que não vale a pena agora. Nenhum
item aqui foi verificado como "impossível" ou "obrigatório" — são candidatos, não decisões.

Cada item tem uma nota de por que importa e, quando relevante, o que já foi conferido no código atual
(pra não sugerir algo que já existe).

---

## 🔐 Segurança

- [ ] **Rate limit / captcha em `/auth/login` e `/auth/forgot-password`** — hoje nada impede tentar
  várias vezes seguidas. Prioridade alta antes de expor a instância publicamente.
- [ ] **Bloqueio de conta após N tentativas de login falhas** — complementa o rate limit; hoje uma
  senha pode ser tentada indefinidamente (só limitado pela velocidade de quem tenta).
- [ ] **Invalidar tokens antigos de `user_token` ao emitir um novo** — pedir recuperação de senha
  duas vezes deixa dois links simultaneamente válidos (TTL curto reduz o risco, mas não zera).
- [ ] **Revogação de JWT** — hoje o token vale 24h (`jwt.expiration`) sem nenhum mecanismo de
  invalidar antes disso (nem logout revoga — só expira o cookie no navegador que pediu). Se um token
  vazar, fica válido até expirar sozinho. Opções: blocklist em Redis, tokens de vida mais curta +
  refresh token, ou aceitar o trade-off documentado.
- [ ] **Política de complexidade de senha** — hoje só exige mínimo de 6 caracteres
  (`ChangePasswordRequest`/`ResetPasswordRequest`), sem checar força.
- [ ] **Segredos hardcoded/com default fraco** — `jwt.secret` (`application.yml`) é um valor fixo no
  arquivo, **sem nenhuma variável de ambiente pra sobrescrever** hoje (diferente de
  `MAIL_CONFIG_ENCRYPTION_KEY`, que ao menos já segue o padrão `${MAIL_CONFIG_ENCRYPTION_KEY:default-fraco}`
  — melhor, mas ainda cai num valor conhecido se a env não for setada). Qualquer deploy real precisa
  de `jwt.secret` vindo de env/cofre de segredos, nunca do valor versionado no repositório; e o ideal
  é a aplicação recusar subir (não só logar um aviso) se rodar fora do perfil de dev com esses valores
  de exemplo ainda ativos.
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
- [ ] **Scan de vulnerabilidade de dependências** — sem Dependabot/`OWASP dependency-check`/Snyk
  configurado em nenhum dos dois repositórios hoje.
- [ ] **HTTPS/HSTS** — ambiente atual é só HTTP local (dev). Fora de escopo até existir um domínio
  de produção de verdade, mas vale não esquecer.
- [ ] Um papel "super-admin" cross-tenant pra proteger a config base de e-mail com permissão própria,
  em vez de reaproveitar `MAIL_CONFIG_MANAGE` (a mesma da config da empresa) — trade-off já
  documentado em `2_known-limitations.md`.

## 📊 Observabilidade

- [ ] **Log level fixo em DEBUG/TRACE pra qualquer ambiente** — `application.yml` tem só um arquivo,
  sem separação por perfil; hoje `org.hibernate.SQL: DEBUG` e `org.hibernate.orm.jdbc.bind: TRACE`
  valem mesmo com `SPRING_PROFILES_ACTIVE=docker`. Bom pra depurar agora, ruim (volume de log, e SQL
  com dado sensível indo pro log) se essa mesma config for parar em produção sem revisão.
- [ ] **Correlação de requisição (trace/correlation ID)** — hoje não dá pra seguir uma requisição
  específica através de vários logs (ex: request HTTP → mensagem na fila → e-mail enviado). MDC do
  SLF4J ou Micrometer Tracing resolveriam.
- [ ] **Métricas** — `spring-boot-starter-actuator` já está no classpath (`pom.xml`), mas sem
  `management.endpoints.web.exposure` configurado (só o `/actuator/health` básico funciona hoje).
  Métricas de verdade (profundidade de fila, latência por endpoint, taxa de erro) precisariam de
  Micrometer + Prometheus/Grafana.
- [ ] **Alerta além do e-mail de dead-letter** — hoje o único alerta automático é o e-mail/notificação
  de DLQ (Parte 7 do guia de mensageria). Um erro não tratado em qualquer outro lugar do sistema só
  aparece no `logs/application.log` — sem Sentry (ou similar) rastreando exceções em produção.
- [ ] **Dashboard de profundidade de fila / consumer lag do RabbitMQ** — hoje só dá pra ver isso
  manualmente no painel `http://localhost:15672`.

## 🚀 Infraestrutura & Deploy

- [ ] **Imagens Docker são só de desenvolvimento** — o `backend/Dockerfile` usa a imagem `maven`
  completa e roda `mvn spring-boot:run` (com devtools, hot-reload); o `frontend/Dockerfile` roda
  `pnpm dev`. Nenhum dos dois é uma imagem de produção (build multi-stage, JRE mínimo, usuário
  não-root, `next build` + `next start` em vez do dev server). Bloqueador real pra qualquer deploy
  de verdade.
- [x] **CI rodando a suíte em cada push/PR** — `.github/workflows/ci.yml` (nos dois repositórios)
  agora roda a suíte a cada push/PR (backend: `./mvnw test` com Postgres+RabbitMQ como `services` do
  job, mesmas imagens/credenciais do `docker-compose.yml`; front: lint + `tsc --noEmit` + `pnpm test`).
  **Deploy automatizado continua não existindo** — falta ainda além disso: imagem de produção (ver
  item abaixo) e o pipeline de deploy em si.
- [ ] **Separação de ambientes** (dev/staging/produção) — hoje só existe o `docker-compose.yml` de
  desenvolvimento, com um `application.yml` único.
- [ ] **Backup automatizado do Postgres** — hoje o banco é só um volume Docker local
  (`../cliente-facil-database`, bind mount), sem rotina de backup/restore documentada ou testada.
- [ ] **Gestão de segredos** (Vault, AWS Secrets Manager, Doppler, etc) — hoje segredos são só
  variáveis de ambiente lidas direto pelo Spring, sem rotação nem cofre centralizado. Só relevante se
  o projeto for pra um provedor de nuvem de verdade.

## 🗄️ Dados & Persistência

- [ ] **Retenção/limpeza de `notification`, `notification_dead_letter`, `user_token`** — todas
  crescem indefinidamente hoje, sem job de limpeza (ex: apagar notificações lidas há mais de 1 ano,
  tokens expirados há mais de 30 dias). Sem isso, essas tabelas só crescem.
- [ ] **LGPD**: direito ao esquecimento (anonimizar/excluir dados de uma pessoa/empresa que pediu),
  portabilidade de dados (exportar tudo que o sistema guarda sobre um usuário). Relevante assim que o
  sistema tiver usuários/empresas reais e não for só uso interno.

### Soft delete + tabela de auditoria genérica

Recomendado com convicção maior que os outros itens desta lista, especificamente pra este sistema:
já existe dado financeiro (`AccountReceivable`/`AccountReceivableMovement`), é multi-tenant B2B (cada
empresa eventualmente vai querer/precisar de histórico), e o projeto está pré-produção — o momento
mais barato de adicionar isso é agora (depois, com dado real em produção, vira migração + backfill,
não só desenho).

- [ ] **Soft delete nas entidades de negócio.** Encaixa no padrão que já existe: `AbstractAuditableEntity`/
  `AbstractAuditableTenantEntity` já usa Hibernate `@Filter` pra isolamento de tenant
  (`company_id = :companyId OR company_id IS NULL`) — soft delete é o mesmo mecanismo, mais um
  filtro. Forma idiomática no Hibernate: `@SQLDelete` (reescreve o `DELETE` que o Hibernate geraria
  pra um `UPDATE ... SET dt_deleted_at = now()`) + `@SQLRestriction`/`@Where`
  (`dt_deleted_at IS NULL` em toda leitura, automático) — praticamente zero mudança nos ~11 services
  que hoje só fazem `repository.delete(entity)`.
  - **Onde vale**: `Client`, `Company`, `Person`, `Professional`, `Event`, `AccountReceivable`/
    `AccountReceivableMovement`, `User` — entidades de negócio com relacionamento e histórico que
    importa.
  - **Onde não vale**: `Notification` (já é volume alto/efêmero), `NotificationDeadLetter` (log
    operacional, não dado de negócio), `UserToken` (token de segurança — quanto antes sumir de
    verdade, melhor), `MailConfig`/`Resource`/`Module`/`ProfilePermission` (configuração, não
    histórico de negócio a preservar).
  - **Trade-off**: índices únicos (`Client` por documento, `User.email`) precisam virar parciais
    (`WHERE dt_deleted_at IS NULL`), senão não dá pra recriar um registro com o mesmo
    e-mail/documento depois de "excluir" o antigo. Mesma disciplina de "não esquecer o filtro" que o
    `tenantFilter` já exige hoje.

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

## 📨 Mensageria & confiabilidade (complementa `1_messaging-and-websocket.md`)

- [ ] **Retenção de dead-letters já resolvidos** — `notification_dead_letter` não tem limpeza
  automática; registros resolvidos há muito tempo continuam ocupando a tabela pra sempre.
- [ ] **Circuit breaker pro SMTP** — hoje é só retry+DLQ (Parte 6); se o SMTP configurado cair, cada
  mensagem de e-mail individualmente martela 3 tentativas antes de desistir, em vez de "aprender" que
  o SMTP está fora e falhar rápido pelas próximas mensagens até o circuito reabrir (Resilience4j
  cobre isso).
- [ ] **Confirmação de entrega de e-mail** — hoje é fire-and-forget (`EmailService.sendTemplated`);
  não há tratamento de bounce/reject do provedor SMTP, nem registro de "foi entregue de verdade".
- [ ] **Preferências de notificação por usuário** — hoje todo usuário recebe toda notificação
  endereçada a ele, sem opção de opt-out por tipo ou de agrupar em digest (resumo periódico em vez de
  um e-mail por evento).
- [ ] **Reaproveitar o padrão fila + STOMP pra outros canais** — o mesmo desenho de
  `NotificationPublisher`/`NotificationListener`/`useStompSubscription` (ver Parte 5 do guia de
  mensageria) já foi pensado pra generalizar pra SMS, push notification, ou qualquer evento
  assíncrono futuro (ex: exportação de PDF) sem mudar a infraestrutura de WebSocket.

## 🧪 Testes (complementa `3_testing-strategy.md`)

- [ ] **Testes de integração com banco de verdade** — a suíte atual (47 testes) é só unitária, com
  Mockito, sem tocar banco. Testcontainers (Postgres real, efêmero, por execução) cobriria coisas que
  só aparecem com JPA/Hibernate de verdade: mapeamento de coluna, o `tenantFilter` do Hibernate,
  constraints de banco (índices únicos parciais do `mail_config`, por exemplo).
- [x] **CI rodando a suíte em cada PR** — ver "🚀 Infraestrutura & Deploy" acima.
- [ ] **Teste de carga/performance** — nunca foi medido quantas notificações/e-mails por segundo o
  sistema aguenta, nem o comportamento sob muitas conexões WebSocket simultâneas.

## 📘 API & Documentação

- [ ] **Paginação real em `GET /api/v1/notifications`** — hoje retorna só as últimas 50 (ver
  `2_known-limitations.md`); falta se um dia existir uma tela de histórico completo.
- [ ] **Política de versionamento/depreciação de API** — hoje só existe `/api/v1/`, sem nenhuma
  estratégia documentada pro dia em que um `/v2` for necessário (quanto tempo o v1 continua no ar,
  como avisar consumidores, etc).
- [ ] **Revisão de completude do Swagger/OpenAPI** — o `@Operation(summary = ...)` está presente na
  maioria dos endpoints, mas vale uma passada conferindo se todos os controllers têm descrição e
  exemplos de payload consistentes.

## 🖥️ Frontend

Itens exclusivos de front (lint, a11y, i18n, error boundary, performance, testes E2E, etc.) saíram
daqui — ver `docs/product/1_roadmap.md` no repositório `cliente-facil-next`, que cobre só o que é
específico do front, sem repetir nada deste documento.

## 🏢 Negócio / Multi-tenant

- [ ] **Self-service signup de empresa** — hoje toda empresa/usuário é criada por um admin já
  existente (ver `1_business-rules.md`); sem um funil de "criar minha conta" público, todo cliente
  novo depende de alguém internamente cadastrar.
- [ ] **Cotas por empresa** (limite de usuários, notificações/mês, etc) — só relevante se o modelo de
  negócio for baseado em planos/tiers pagos.
- [ ] **Billing/assinatura** — nenhuma integração de cobrança existe hoje; só relevante se o produto
  for comercializado como SaaS pago.

## 🛠️ Qualidade de código / DX

- [ ] **Formatador/linter no backend** — não existe Spotless/Checkstyle configurado no `pom.xml`
  hoje; o estilo de código é mantido só por convenção manual.
- [ ] **Pre-commit hooks** (lint, format, testes rápidos antes do commit) — não existe em nenhum dos
  dois repositórios.
- [ ] **Reavaliar editar nome/e-mail do próprio usuário** na tela de conta — adiado por decisão (ver
  `1_business-rules.md`), não descartado; revisitar se a demanda aparecer.
