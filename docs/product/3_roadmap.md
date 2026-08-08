# Roadmap / próximos passos

Itens em aberto — nada aqui está bloqueado por código, são decisões/trabalho futuro. Consolidado das
seções "Pendências desta rodada" que estavam espalhadas por `docs/guides/1_messaging-and-websocket.md` (Partes 8
e 9) e `docs/guides/2_authentication.md`.

## Curto prazo

- [ ] Configurar um SMTP real (base e/ou por empresa) quando quiser e-mails saindo de verdade —
  `PUT /api/v1/mail-configs/base` ou `/company`, ou pela tela `/dashboard/admin/mail-config` /
  `/dashboard/settings`.
- [ ] Rate limit (por IP/e-mail) e/ou captcha em `/auth/login` e `/auth/forgot-password`, antes de ir
  pra produção de verdade.
- [ ] Invalidar tokens antigos (`user_token`) quando um novo é emitido pro mesmo usuário/tipo — hoje
  múltiplos pedidos de recuperação de senha deixam vários links simultaneamente válidos.

## Médio prazo

- [ ] Um papel "super-admin" cross-tenant pra proteger a config base de e-mail com uma permissão
  própria, em vez de reaproveitar `MAIL_CONFIG_MANAGE` (a mesma da config da empresa).
- [ ] Paginação de verdade em `GET /api/v1/notifications`, quando existir uma tela de histórico (hoje
  alimenta só o sino, últimas 50).
- [ ] Reavaliar permitir editar o próprio e-mail (com reconfirmação) e/ou nome na tela de conta —
  adiado por decisão, não descartado.

## Ideias / padrão pra reaproveitar

- Qualquer feature nova de "processar em fila + avisar em tempo real" (ex: exportação de PDF) pode
  seguir o mesmo padrão já validado por notificações, sem precisar mexer em `WebSocketConfig`,
  `StompAuthChannelInterceptor`, `StompProvider` ou no ticket de autenticação do WS — ver
  `docs/guides/1_messaging-and-websocket.md`, Parte 5 ("Como uma feature futura reaproveitaria
  isso"), pelo passo a passo.
