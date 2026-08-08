# Regras de negócio

Consolidado a partir do que já estava documentado em `docs/guides/1_messaging-and-websocket.md` e
`docs/guides/2_authentication.md`, mais o que ficou implícito no código dessas duas áreas. **Não é uma auditoria
completa do projeto** — cobre o que foi implementado/revisado até aqui (mensageria, tempo real,
e-mail, autenticação). Cadastros "normais" (Cliente, Empresa, Pessoa, Evento, etc.) seguem o padrão
CRUD-4-permissões já visível no próprio código e não estão detalhados aqui.

## Multi-tenant (isolamento por empresa)

- Toda entidade tenant-aware estende `AbstractAuditableTenantEntity`: `company_id` preenchido
  automaticamente no `@PrePersist` a partir do usuário autenticado, e leitura cross-tenant é
  bloqueada (`AccessDeniedException` no `@PostLoad`) — ninguém vê dado de outra empresa por acidente.
- **Exceção deliberada, o padrão "config base"**: quando `company_id` pode ser `NULL`, isso significa
  "escopo do sistema, não de uma empresa" — e fica automaticamente visível pra qualquer empresa via o
  `tenantFilter` do Hibernate (`company_id = :companyId OR company_id IS NULL`). Usado hoje só por
  `mail_config` (config base de SMTP), com no máximo 1 linha `company_id IS NULL` (índice único
  parcial) + no máximo 1 por empresa. A empresa tem prioridade sobre a base quando as duas existem.

## Permissões

- Recurso "normal" (Cliente, Empresa, Usuário, etc.): 4 permissões (`_VIEW`/`_CREATE`/`_UPDATE`/
  `_DELETE`), sincronizadas automaticamente na tabela `resource` a partir de `ResourceEnum`
  (`AuthorizationSeeder`, roda toda subida da aplicação).
- Recurso singleton (`mail_config`): só 2 permissões (`_VIEW`/`_MANAGE`) — criar e editar são a
  mesma ação de "salvar a configuração", não faz sentido um CRUD de 4 aqui.
- Recurso "ação pontual" sem entidade própria (disparar notificação, testar/simular DLQ): 1
  permissão dedicada por ação (`NOTIFICATION_SEND`, `DEAD_LETTER_TEST`, etc.), não um CRUD.
- O perfil "Admin" recebe **toda** permissão automaticamente (`MainSeeder`, a cada subida) — outros
  perfis precisam de concessão explícita na tela de Perfis.
- `useHasAuthority` no front é **só uma camada de UX** (esconde botão que ia dar 403) — a permissão
  de verdade é sempre imposta pelo `@PreAuthorize` no backend.

## Notificações

- Toda notificação pertence a exatamente **um** usuário destinatário. Marcar como lida/excluir exige
  que a notificação pertença a quem está pedindo (senão `403`, não `404` — o registro existe, só não
  é seu).
- Disparo manual pra outros usuários (`POST /notifications/send`) exige permissão dedicada
  (`NOTIFICATION_SEND`) — diferente do picker de destinatários (`GET /users/key-value`), que só
  expõe id+nome (nada sensível) e não exige nenhuma permissão especial, só estar autenticado. A
  listagem desse picker já vem filtrada por empresa automaticamente (isolamento multi-tenant) — não
  dá pra notificar alguém de outra empresa pela UI.
- `POST /notifications/test` dispara uma notificação de teste só pra si mesmo, sem permissão especial
  — é diferente de `/send`, que afeta terceiros.

## Mensageria: retry, Dead Letter Queue e alerta

- Mensagem que falha no processamento tenta de novo até 3 vezes com backoff crescente antes de cair
  na DLQ (`spring.rabbitmq.listener.simple.retry`).
- Toda mensagem morta é auditada em `notification_dead_letter` (`tp_origin`: `NOTIFICATION` ou
  `EMAIL`) e gera dois alertas: broadcast em tempo real (`/topic/system/dead-letters`, pra quem está
  com o painel aberto) e uma notificação real (sino) pra todo usuário com `DEAD_LETTER_VIEW`.
- **Alerta por e-mail só na origem `NOTIFICATION`, nunca em `EMAIL`** — regra deliberada pra evitar
  loop: se o próprio envio de e-mail é o que está falhando (ex: SMTP fora do ar), avisar por e-mail
  sobre essa falha realimentaria o mesmo problema.
- Fila de e-mail é separada da fila de notificação, mesmo padrão de retry/DLQ, pelo mesmo motivo:
  isolar os dois domínios de falha.

## E-mail (SMTP)

- Cada empresa pode ter sua própria config de SMTP (`mail_config`); e-mails sem empresa associada
  (ex: alerta de dead-letter) usam a config base do sistema.
- Senha de SMTP é **reversível** (AES-256/GCM, não hash) — precisa ser recuperada em claro pra
  autenticar no envio. Diferente da senha de usuário (hash, `BCrypt`), que nunca precisa ser lida de
  volta.
- "Testar Conexão" no formulário de config testa os dados **atuais do formulário** (mesmo não
  salvos ainda), não a config persistida — envio síncrono, sem passar pela fila.

## Autenticação e contas de usuário

- Não existe cadastro público — todo usuário é criado por um admin (`USER_CREATE`).
- **E-mail precisa ser confirmado antes do primeiro login.** O usuário recebe um link por e-mail na
  criação da conta (token de uso único, válido por 7 dias); um admin pode reenviar a qualquer momento
  enquanto não confirmado. O usuário seed (`admin@admin.com`) nasce já confirmado, pra não travar o
  bootstrap do sistema.
- Recuperação de senha é self-service (`/auth/forgot-password` → `/auth/reset-password`), token de
  uso único válido por 1 hora. O endpoint de pedido nunca revela se o e-mail existe ou não no sistema
  (evita enumeração de contas).
- Troca de senha pelo próprio usuário logado (`/dashboard/settings`) exige confirmar a senha atual.
- `User.name` é só um rótulo de conta — o dado "de verdade" da pessoa vive em `Person` (tela própria,
  `PERSON_UPDATE`). Editar o próprio e-mail/nome pela tela de conta está fora de escopo por decisão
  (ver `2_known-limitations.md`).

## Ver também

- `docs/guides/1_messaging-and-websocket.md` — como cada uma dessas regras foi implementada (RabbitMQ, STOMP,
  e-mail).
- `docs/guides/2_authentication.md` — como a autenticação foi implementada.
- `2_known-limitations.md`, `3_roadmap.md` — trade-offs aceitos e o que ainda falta.
