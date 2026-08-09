# 🔐 Autenticação: login, confirmação de e-mail e recuperação de senha

Este doc cobre o fluxo de autenticação do zero: login (JWT), confirmação de e-mail obrigatória pra
usuários criados por um admin, e recuperação de senha self-service. Mensageria/e-mail (RabbitMQ,
`mail_config`, dead-letter) e o `ws-ticket` do STOMP estão em `docs/guides/1_messaging-and-websocket.md` — este
doc só entra nesses assuntos no ponto exato em que a autenticação depende deles (envio dos e-mails de
confirmação/recuperação). Regras de negócio, limitações conhecidas e roadmap de todo o projeto ficam
centralizados em `docs/product/`, não aqui.

## 🔑 Login (JWT)

`POST /api/v1/auth/login` (`AuthController`/`AuthService`, público — ver `SecurityConfig`): busca o
usuário por e-mail, confere a senha (`BCryptPasswordEncoder`), confere que o e-mail está confirmado
(ver seção seguinte) e devolve um JWT (`JwtService`, `jwt.secret`/`jwt.expiration` em
`application.yml`). O front guarda o token num cookie `httpOnly` (`src/app/api/login/route.ts`) — o
JS do navegador nunca tem acesso direto a ele.

`GET /api/v1/auth/me` devolve id/e-mail/authorities de quem está logado (usado por `useCurrentUser`/
`useHasAuthority` no front) sem expor o JWT ao JS.

## 🛡️ Rate limit, bloqueio de conta e segredos

Três camadas de proteção em `/auth/login` e `/auth/forgot-password`, cada uma cobrindo um ângulo
diferente:

- **Rate limit** (`RateLimiter`, sliding window log em memória) — no máximo 5 tentativas de login e
  3 pedidos de recuperação de senha por e-mail, por minuto/15 minutos respectivamente. A chave é o
  **e-mail do request, não o IP** — o backend só enxerga o proxy same-origin do Next.js como origem
  de rede (`api/login/route.ts`/`api/forgot-password/route.ts` fazem o fetch do lado do servidor),
  então rate-limitar por IP juntaria todo mundo que passa por ali no mesmo balde. Checado *antes* de
  olhar se o e-mail existe, nos dois endpoints — senão a própria resposta (429 só quando existe vs.
  nunca) vira mais um jeito de descobrir quais e-mails têm conta. Excedeu o limite → `429 Too Many
  Requests`. Reseta sozinho ao sair da janela, sem precisar de nenhuma ação.
- **Bloqueio de conta** (`nr_failed_login_attempts`/`dt_locked_until` em `users`) — 5 senhas erradas
  seguidas bloqueiam a conta por 15 minutos; qualquer login com senha certa zera o contador. Diferente
  do rate limit acima: conta só falhas (não toda tentativa), persiste no banco (sobrevive a restart da
  aplicação) e é por conta, não por janela de tempo — protege contra quem espalha as tentativas ao
  longo de várias janelas de rate limit pra escapar dele.
- **Segredos de exemplo travados fora de dev** (`SecretConfigurationGuard`) — `jwt.secret` e
  `mail.config.encryption-key` têm um valor de exemplo hardcoded no `application.yml`, sobrescrito via
  `JWT_SECRET`/`MAIL_CONFIG_ENCRYPTION_KEY`. Esse componente roda no boot e recusa subir a aplicação
  (não só loga um aviso) se algum dos dois ainda estiver no valor de exemplo **e** o perfil ativo
  parecer produção (`prod`/`production`/`staging`) — em dev, docker local, test, ou sem nenhum perfil
  definido, continua só um aviso no log, sem travar nada do fluxo atual.

Mensagem de "usuário não encontrado" e "senha errada" foi unificada em "Credenciais inválidas" nessa
mesma rodada — mensagens diferentes por esse motivo específico também são uma forma de enumeração de
usuários (descobrir quais e-mails têm conta tentando logar com eles).

Toda senha nova (criação de usuário, troca self-service, reset via link) passa por `@StrongPassword`
(`core/validation/`) — mínimo 8 caracteres, com letra e número. Uma anotação só, reaproveitada nos
três DTOs (`UserRequest`/`ChangePasswordRequest`/`ResetPasswordRequest`) em vez de repetir a regra em
cada um.

## 📧 Confirmação de e-mail

Não existe cadastro público no sistema — todo usuário é criado por um admin (`POST /api/v1/users`,
`USER_CREATE`). Ainda assim, o e-mail precisa ser confirmado antes do primeiro login: previne login
com um e-mail digitado errado pelo admin (a pessoa nunca teria acesso à própria conta) e dá uma
oportunidade de definir/trocar a senha por um canal que só o dono do e-mail acessa.

### Como funciona

- `UserService.create()`: depois de salvar o usuário, dispara `sendConfirmationEmail` — gera um
  token (`UserTokenService.issue`, tipo `EMAIL_CONFIRMATION`, validade de 7 dias — convite pode
  demorar a ser aberto) e manda o template `email-confirmation.html` com o link
  `{app.frontend-url}/auth/confirm-email?token=...`. **Best-effort**: se o envio falhar, loga um
  warning mas não impede a criação do usuário (mesma filosofia de
  `NotificationDeadLetterListener.notifyAdmins`, ver `docs/guides/1_messaging-and-websocket.md`,
  Parte 8).
- `AuthService.login()`: se `user.dtEmailConfirmedAt == null`, login é recusado com uma mensagem
  explicando o motivo — front mostra essa mensagem inline na tela de login (`api/login/route.ts`
  agora propaga a mensagem real do backend, em vez de um texto fixo genérico).
- `POST /api/v1/auth/confirm-email` (público, `ConfirmEmailRequest{token}`): consome o token
  (`UserTokenService.consume`) e seta `dtEmailConfirmedAt = now()`. Front:
  `src/app/auth/confirm-email/page.tsx` — lê `?token=` da URL e confirma sozinho, sem formulário.
- **Reenviar confirmação**: na tela `Usuários` (`/dashboard/user`), qualquer linha cujo e-mail ainda
  não foi confirmado mostra um badge "Pendente" + botão "Reenviar confirmação"
  (`POST /api/v1/users/{id}/resend-confirmation`, `USER_UPDATE` — reaproveita a permissão de edição
  de usuário existente, não é um recurso novo). Recusa se o e-mail já estiver confirmado.
- O usuário `admin@admin.com` seedado pelo `MainSeeder` já nasce com `dtEmailConfirmedAt` preenchido
  — sem isso o próprio bootstrap do sistema ficaria travado (não existe fluxo de confirmação pra
  rodar antes do primeiro usuário existir).

## 🔁 Recuperação de senha

Fluxo clássico de "esqueci minha senha", self-service, sem precisar de admin:

- `POST /api/v1/auth/forgot-password` (público, `ForgotPasswordRequest{email}`): se o e-mail existir,
  gera um token (`UserTokenService.issue`, tipo `PASSWORD_RESET`, validade de **1 hora** — mais curta
  que a de confirmação, por ser mais sensível) e manda `password-reset.html` com o link
  `{app.frontend-url}/auth/reset-password?token=...`. **Sempre responde 202**, exista ou não o
  e-mail — não dá pra usar esse endpoint pra descobrir quais e-mails têm conta no sistema (enumeração
  de usuários).
- `POST /api/v1/auth/reset-password` (público, `ResetPasswordRequest{token, newPassword}`): consome
  o token e troca a senha (`BCryptPasswordEncoder`). Front: `src/app/auth/reset-password/page.tsx`,
  lê `?token=` da URL, formulário de nova senha + confirmação.
- Link "Esqueci minha senha" na tela de login → `/auth/forgot-password`.

## 🎟️ `UserTokenService` — mecânica compartilhada

Confirmação de e-mail e recuperação de senha usam a **mesma tabela** (`user_token`,
`UserTokenTypeEnum.EMAIL_CONFIRMATION`/`PASSWORD_RESET`) e o mesmo serviço
(`service/UserTokenService.java`):

- `issue(user, type, ttl)`: primeiro invalida (marca `dtUsedAt = now()`) qualquer token do mesmo tipo
  ainda não usado pra esse usuário — pedir recuperação de senha duas vezes não deixa dois links
  simultaneamente válidos, só o mais recente funciona. Depois gera 32 bytes aleatórios
  (`SecureRandom`) → base64url — esse é o token **cru**, que vai no link do e-mail. Só o **hash
  SHA-256** dele é persistido (`ds_token_hash`); o cru nunca toca o banco.
- `consume(rawToken, type)`: acha pelo hash, confere `dtUsedAt == null` (uso único), `dtExpiresAt` no
  futuro e o `type` batendo; marca `dtUsedAt = now()`. Erro único e genérico ("Link inválido ou
  expirado") pros quatro motivos de falha (não existe, tipo errado, expirado, já usado/invalidado) —
  não vale a pena diferenciar pro chamador, e evita dar pista de qual é o problema.

Não é tenant-scoped (`AbstractAuditableEntity`, não `AbstractAuditableTenantEntity`) — é
infraestrutura de autenticação, não dado de negócio de uma empresa, mesmo espírito não-tenant de
`notification_dead_letter` (ver `docs/guides/1_messaging-and-websocket.md`, Parte 7).

## 🌐 `app.frontend-url`

Os links dos e-mails (`confirmUrl`/`resetUrl`) precisam apontar pro front, não pro backend. Nova
property `app.frontend-url` (`application.yml`, default `http://localhost:3000`, override via
`APP_FRONTEND_URL`) — **não** precisou de configuração extra no `docker-compose.yml`: diferente do
MailHog (só alcançável pelo hostname interno `mailhog` dentro da rede Docker), esse link é aberto no
navegador de quem recebeu o e-mail, fora da rede Docker — a porta publicada (`localhost:3000`) já é o
valor certo.

## 🛠️ Tela admin de config de e-mail (base do sistema)

Complementa `docs/guides/1_messaging-and-websocket.md` (Parte 8): o backend de `/mail-configs/base`
já existia, só faltava o front. `MailConfigCard` (usado no card "Configurações de E-mail" de `/dashboard/settings`) ganhou
um prop `scope: "BASE" | "COMPANY"` (default `COMPANY`) — mesmo componente, mesma permissão
(`MAIL_CONFIG_VIEW`/`MAIL_CONFIG_MANAGE`, igual ao backend), só troca o endpoint que consulta/salva.
Tela nova em `/dashboard/admin/mail-config` (menu "Administração").

## 🐛 Bug incidental corrigido nesta rodada

`AuthorizationSeeder` (sincroniza a tabela `resource` a partir de `ResourceEnum`) e o
`CommandLineRunner` do `MainSeeder` (concede as permissões default ao perfil Admin) não tinham ordem
garantida entre si — em bancos zerados, dependendo da ordem de registro dos beans, o `MainSeeder`
podia rodar primeiro e ver a tabela `resource` vazia, deixando o admin sem **nenhuma** permissão.
Corrigido com `@Order(Ordered.HIGHEST_PRECEDENCE)` no `AuthorizationSeeder`.

## Regras de negócio, limitações e roadmap

Movidos pra `docs/product/` (pasta única pra esse tipo de conteúdo em todo o projeto):

- `docs/product/1_business-rules.md` — regras de negócio (confirmação obrigatória, tokens de uso
  único, etc.).
- `docs/product/2_known-limitations.md` — trade-offs aceitos conscientemente (sem rate limit, tokens
  antigos não invalidados, edição de e-mail/nome fora de escopo, etc.).
- `docs/product/3_roadmap.md` — o que ainda falta.
