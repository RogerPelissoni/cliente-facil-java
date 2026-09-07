# Limitações conhecidas

Trade-offs aceitos conscientemente — cada um tem uma razão documentada, não é esquecimento.
Consolidado do que estava espalhado pelas seções "Limitações conhecidas"/"Pendências" de
`docs/guides/1_messaging-and-websocket.md` e `docs/guides/2_authentication.md`. Nenhuma delas impede o uso real dos
recursos — a lista existe pra deixar claro o que foi decidido conscientemente, pra revisitar se/quando
importar.

## Mensageria / tempo real / e-mail

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

## Templates de e-mail

- **`EmailTemplateVariablesTest` extrai variáveis do `.html` por regex simples**, não parseia OGNL/
  SpringEL de verdade — pega `${nome}` e o primeiro identificador de `${nome.propriedade}`. Suficiente
  pros templates atuais (todos variáveis simples); um template que precisasse de navegação de
  propriedade aninhada (`${objeto.campo}`) ainda funcionaria em produção normalmente, só o teste
  compararia pelo nome de `objeto`, não pelo caminho completo.

## Autenticação

- **Sem rate limit** em `/auth/forgot-password` nem `/auth/login` — alguém pode tentar várias vezes
  seguidas sem bloqueio. Vale revisitar antes de produção de verdade (rate limit por IP/e-mail,
  captcha).
- **Tokens antigos não são invalidados ao emitir um novo** — pedir recuperação de senha duas vezes
  deixa dois links válidos simultaneamente (cada um ainda de uso único, com TTL curto). Não é um
  risco alto dado o TTL de 1h.
- **Editar o próprio e-mail/nome continua fora da tela de conta** — decisão explícita, não limitação
  técnica: `User.name` é só um rótulo de conta (o dado real vive em `Person`); e-mail é o
  identificador de login e trocar exigiria um fluxo de reconfirmação. Avaliado e adiado por ora.

## Soft delete (ver `docs/guides/5_soft-delete.md`)

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
  usa `AccountReceivableValidator`. Vira relevante quando um CRUD dedicado for construído (ver
  `docs/product/4_future-business-rules.md`) — nesse dia, reaproveitar o validador existente.

## Geral

- **`useHasAuthority` é só uma camada de UX**, nunca a fonte de verdade de permissão — esconder um
  botão no client não impede ninguém com acesso a `curl`/DevTools de chamar o endpoint direto. A
  proteção real é sempre o `@PreAuthorize` no backend.
