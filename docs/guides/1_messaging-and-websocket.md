# 📨 Mensageria (RabbitMQ) + Tempo real (STOMP/WebSocket): Notificações

Guia de estudo sobre a implementação de mensageria assíncrona (RabbitMQ) e comunicação em tempo real
(WebSocket/STOMP) no Cliente Fácil, usando como caso de uso real um sistema de **notificações
persistidas por usuário**. Regras de negócio, limitações conhecidas e roadmap de todo o projeto ficam
centralizados em `docs/product/`, não aqui.

**Fluxo implementado:**

```
[Frontend] --POST /api/v1/notifications/test--> [Backend]
                                                      |
                                                      v
                                          RabbitMQ (exchange -> binding -> queue)
                                                      |
                                                      v
                                    [Backend] NotificationListener consome
                                                      |
                                                      v
                                    1) Persiste no banco (tabela notification)
                                    2) Publica via STOMP em /queue/notifications
                                       endereçado ao usuário destinatário
                                                      |
                                                      v
                        [Frontend] recebe em tempo real (se conectado) e/ou busca
                        via GET /api/v1/notifications (sempre reflete o que está salvo)
```

O ponto central continua o mesmo: a requisição HTTP que **publica** a mensagem responde
imediatamente (`202 Accepted`), sem esperar o processamento. Quem processa (o listener) é notificado
pelo RabbitMQ de forma assíncrona, persiste o resultado, e avisa o navegador em tempo real — mas agora
**nada se perde**: se o usuário não estiver com a página aberta no momento, a notificação continua lá
quando ele voltar (`GET /api/v1/notifications`).

---

# 🐇 Parte 1 — RabbitMQ (sem mudanças de conceito desde a primeira versão)

## Conceitos básicos

| Termo | O que é |
|---|---|
| **Producer** | Quem publica a mensagem. Não conhece a fila, só o Exchange. |
| **Exchange** | Recebe a mensagem do producer e decide para qual(is) fila(s) encaminhar. |
| **Queue (fila)** | Onde a mensagem fica guardada até um Consumer processá-la. |
| **Binding** | A "ligação" entre Exchange e Queue, associada a uma routing key. |
| **Consumer** | Quem lê e processa a mensagem da fila. |

Fluxo: `Producer -> Exchange -> (routing key / binding) -> Queue -> Consumer`.

## Onde está implementado

- `docker-compose.yml`: serviço `rabbitmq` (`rabbitmq:3.13-management-alpine`), painel web em
  `http://localhost:15672` (`guest`/`guest`).
- `core/config/RabbitMQConfig.java`: exchange/fila/binding (`clientefacil.notification.*`) +
  conversor JSON (Jackson).
- `messaging/NotificationMessageDTO.java`: payload que trafega na fila (`userId`, `type`, `title`,
  `message`).
- `messaging/NotificationPublisher.java` (Producer): `rabbitTemplate.convertAndSend(...)`,
  fire-and-forget.
- `messaging/NotificationListener.java` (Consumer): `@RabbitListener`, persiste via
  `NotificationService` e repassa para a Parte 2 (STOMP).

---

# 🗄️ Parte 2 — A tabela `notification`

Um sistema de notificação "amplamente adotado no mercado" (GitHub, Laravel Notifications, a maioria
dos SaaS com sino de notificação) converge para os mesmos campos essenciais — foi isso que se buscou
aqui, adaptado às convenções já existentes neste projeto (prefixos `tp_`/`ds_`/`dt_`, como em
`account_receivable` e `event`):

| Coluna | Por quê |
|---|---|
| `user_id` | Toda notificação é para **um** destinatário específico (FK para `users`, `ON DELETE CASCADE`). |
| `tp_type` (enum: `INFO/SUCCESS/WARNING/ERROR`) | Categoriza a notificação para exibição (cor/ícone). Mesma nomenclatura que o `sonner` (biblioteca de toast já usada no front) já usa — consistência visual "de graça". |
| `tp_status` (enum: `UNREAD/READ/ARCHIVED`) | Estado de leitura. `ARCHIVED` existe para o dia em que fizer sentido "arquivar" sem excluir (não usado ainda). |
| `ds_title` / `ds_message` | Título curto + corpo da notificação. |
| `dt_read` | Timestamp de quando foi lida (nullable). Complementa `tp_status`: permite responder "quando" além de "se". |
| `company_id`, `created_at`, etc. | Auditoria/multi-tenant padrão do projeto (`AbstractAuditableTenantEntity`, como `User`/`Event`). |

Migrations: `V13_1__create_notification_type_enum.sql`, `V13_2__create_notification_status_enum.sql`,
`V13_3__create_notification_table.sql`. Entidade: `entity/Notification.java`. DTO exposto pela API:
`dto/NotificationResponse.java` — única DTO do projeto que expõe `createdAt` (as demais não expõem
campos de auditoria), porque "quando chegou" é essencial para a UX de uma notificação.

**Endpoints** (`controller/NotificationController.java`, todos exigem JWT como qualquer outro endpoint
da API):
- `GET /api/v1/notifications` — últimas 50 do usuário autenticado.
- `PATCH /api/v1/notifications/read-all` — marca todas como lidas.
- `PATCH /api/v1/notifications/{id}/read` — marca uma notificação específica como lida.
- `DELETE /api/v1/notifications/{id}` — remove uma notificação.
- `POST /api/v1/notifications/test` — dispara uma notificação de teste para si mesmo
  (`{"content": "...", "type": "SUCCESS"}`, `type` é opcional, default `INFO`) — é o gatilho manual
  para validar o fluxo inteiro sem precisar de uma feature de negócio real ainda.
- `POST /api/v1/notifications/send` — dispara notificações para uma lista de usuários específicos
  (`{"userIds": [1, 2], "title": "...", "message": "...", "type": "WARNING"}`) — ver Parte 4.

`{id}/read` e `DELETE /{id}` verificam, no `NotificationService`, que a notificação pertence ao
usuário autenticado (`entity.getUser().getId().equals(currentUserId())`) antes de agir — senão
devolvem `403` (`AccessDeniedException`), não `404`, porque o recurso existe, só não é seu.

---

# 🔌 Parte 3 — Tempo real: por que STOMP em vez de WebSocket "cru"

A primeira versão deste recurso usava um `WebSocketHandler` puro, específico para notificações
(mantinha um `Map<userId, Set<Session>>` na mão). Funcionava, mas **não escalava**: cada nova feature
de tempo real (ex: avisar quando uma exportação de PDF processada em fila termina) exigiria duplicar
toda aquela lógica de gestão de sessão num novo handler.

**STOMP** (Simple Text-Oriented Messaging Protocol) resolve isso: é um protocolo de mensageria que roda
*por cima* de uma única conexão WebSocket, permitindo múltiplos "destinos" lógicos multiplexados nela
— como ter várias "portas" numa única conexão. O Spring já traz suporte pronto
(`spring-boot-starter-websocket` inclui `spring-messaging`).

Com isso, uma feature nova (ex: exportação de PDF) não precisa de endpoint, handler nem gestão de
conexão própria — só:
1. Publica em `/queue/pdf-export` via `SimpMessagingTemplate.convertAndSendToUser(userId, "/queue/pdf-export", payload)`.
2. O front assina `/user/queue/pdf-export` com o mesmo `useStompSubscription` já usado por notificações.

## Onde está implementado

### Backend

#### `core/config/WebSocketConfig.java`
Infra **genérica**, não específica de notificação:
```java
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    registerStompEndpoints(...)     // um único endpoint: "/ws"
    configureMessageBroker(...)     // habilita "/queue" (por usuário) e "/topic" (broadcast, não usado ainda)
    configureClientInboundChannel(...) // registra o interceptor de autenticação (abaixo)
}
```

#### `core/security/StompAuthChannelInterceptor.java` — a peça que resolve a autenticação
O upgrade HTTP inicial de WebSocket (`GET /ws`) **não** carrega o header `Authorization` — a API nativa
do navegador não permite headers customizados nessa etapa (por isso `/ws/**` é `permitAll` no
`SecurityConfig`, igual na v1). A diferença é que, uma vez a conexão WebSocket aberta, o **cliente
STOMP manda um frame `CONNECT`**, que é uma mensagem de aplicação comum — essa sim pode carregar
headers arbitrários, inclusive `Authorization: Bearer <jwt>`.

É esse frame que o `StompAuthChannelInterceptor` autentica, com a mesma lógica que o `JwtFilter` já usa
para requisições REST (`JwtService` + `AuthenticatedUserService`). Se válido, associa um
`StompPrincipal(userId)` à sessão STOMP — é esse principal que o Spring usa depois para resolver
`convertAndSendToUser(userId, destino, payload)` até a conexão certa.

> Isso corrige, de forma definitiva, a limitação de segurança que a v1 deste recurso tinha documentado
> como aceitável-mas-não-ideal (autenticação via query param não verificado). Agora a autenticação é
> real, com o mesmo JWT usado no resto da API.

#### `messaging/NotificationListener.java`
```java
messagingTemplate.convertAndSendToUser(
    String.valueOf(notification.userId()),
    "/queue/notifications",
    notification
);
```
Nota: como o broker do Spring já serializa o objeto para JSON automaticamente, não precisamos mais
converter manualmente (a v1, com WebSocket cru, precisava de um `ObjectMapper` explícito).

### Frontend (`cliente-facil-next`)

#### `GET /api/ws-token` (`src/app/api/ws-token/route.ts`)
O JWT fica em cookie `httpOnly` (inacessível ao JS do navegador) — mesma razão documentada na v1. Como
a autenticação acontece no frame STOMP CONNECT (que o JS do navegador monta e envia), o JavaScript
precisa ter *algum* token em mãos.

A versão inicial deste endpoint devolvia o JWT completo (24h de validade) ao navegador — funcionava,
mas expunha um token de vida longa a qualquer script rodando na página, sem necessidade. Foi corrigido
para um esquema de **ticket efêmero**:

1. Esta rota, rodando no servidor Next.js, usa o JWT (só ela tem acesso ao cookie) para chamar
   `GET /api/v1/auth/ws-ticket` no backend — endpoint autenticado que gera um token opaco aleatório
   (`UUID`), guardado em memória (`core/security/WsTicketService.java`) associado ao `userId`, válido
   por **30 segundos** e **de uso único**.
2. A rota devolve só esse ticket ao navegador (`{ ticket }`) — nunca o JWT.
3. O `StompProvider` manda o ticket como `Authorization: Bearer <ticket>` no frame CONNECT.
4. `StompAuthChannelInterceptor` chama `wsTicketService.consume(ticket)`: se existir e não tiver
   expirado, remove da memória (por isso "uso único") e retorna o `userId` associado — sem precisar
   validar assinatura JWT nem consultar o banco, já que o ticket *é* a prova de autenticação, emitida
   por um endpoint que já exigiu o JWT completo.

Se alguém capturar esse ticket (ex: via um XSS pontual), a janela de exploração é de segundos e só serve
para uma única conexão — bem diferente de vazar um JWT de 24h reutilizável em qualquer chamada da API.
Validado manualmente: reconectar com um ticket já consumido resulta numa sessão STOMP sem `Principal`
associado (conecta, mas nunca recebe nada endereçado a um usuário).

> Trade-off que permanece, documentado por completude: o ticket ainda não está atado à sessão/IP de
> quem o pediu (qualquer processo que capture o ticket dentro da janela de 30s consegue usá-lo uma vez).
> Suficiente para o risco atual do projeto; endurecer mais isso teria retorno decrescente frente à
> complexidade adicional (catalogado em `docs/product/2_known-limitations.md`).

#### `src/shared/providers/StompProvider.tsx` — infra genérica reutilizável
Abre **uma única conexão STOMP** para toda a área autenticada (`dashboard/layout.tsx`), usando
`@stomp/stompjs` (conecta via `WebSocket` nativo, sem precisar de `sockjs-client`). Expõe via Context
um `subscribe(destino, callback)` que:
- registra o callback e, se a conexão já estiver pronta, assina o destino imediatamente;
- se a conexão ainda não estiver pronta, guarda o callback e assina automaticamente assim que
  `onConnect` disparar;
- suporta múltiplos assinantes no mesmo destino e desfaz a assinatura de verdade só quando o último
  assinante sai.

Isso é o que qualquer feature futura vai reutilizar sem escrever uma linha de gestão de conexão.

#### `src/shared/hooks/useStompSubscription.ts` — hook genérico
```ts
useStompSubscription("/user/queue/notifications", (message) => { ... });
```
Assina no mount, desfaz no unmount. É este hook, e não o `StompProvider` diretamente, que cada feature
deve usar.

#### `src/modules/notification/*`
- `notification.type.ts` / `notification.api.ts` / `notification.hooks.ts`: `useNotifications()`
  (`GET /notifications`), `useMarkAllNotificationsAsRead()`, `useMarkNotificationAsRead()`,
  `useDeleteNotification()`, `useSendNotification()` — todos via `@tanstack/react-query`.
- `NotificationBell.tsx`: usa `useNotifications()` para a lista (fonte de verdade = banco) e
  `useStompSubscription("/user/queue/notifications", ...)` só para saber **quando invalidar** essa
  query — ou seja, o WebSocket aqui não carrega dados diretamente para a tela, só avisa "tem
  novidade, busque de novo". Isso evita manter duas fontes de verdade (uma lista em memória vinda do
  WS e outra vinda do banco) — só existe uma: o banco, sempre.
- `NavBarComponent.tsx` só renderiza `<NotificationBell />` — toda a lógica de notificação foi extraída
  para o módulo, mantendo o componente de navegação enxuto.

## Como testar o ciclo completo

1. Subir tudo: `docker compose up -d`
2. Abrir `http://localhost:3000`, logar (`admin@admin.com` / `123456`)
3. Pelo Swagger (`http://localhost:8080/swagger-ui/index.html`, `Authorize` com o token do login),
   chamar `POST /api/v1/notifications/test` com `{"content": "...", "type": "SUCCESS"}`
4. O sino no `NavBarComponent` atualiza sozinho, sem refresh — clicar mostra a notificação colorida
   pelo `type` escolhido
5. Dar refresh na página: a notificação continua lá (diferente da v1, que era efêmera) — prova de que
   está persistida, não só entregue ao vivo

---

# 📋 Parte 4 — Modal completa: ler, excluir e disparo manual para usuários específicos

A modal de notificações deixou de ser só uma lista somente-leitura: cada item agora tem ações
individuais, e existe uma segunda modal para compor e disparar notificações para usuários escolhidos.

## Ações por item (`NotificationBell.tsx`)

- **Marcar como lida** (ícone de check, só aparece em itens `UNREAD`): `PATCH /notifications/{id}/read`.
- **Excluir** (ícone de lixeira, sempre visível): `DELETE /notifications/{id}`.

Ambas chamam `NotificationService` no backend, que primeiro confere que a notificação pertence ao
usuário autenticado (`findOwnedEntity`) antes de agir — ver Parte 2.

## Picker de destinatários: `GET /api/v1/users/key-value`

Para popular o multi-select da modal de envio, foi adicionado um endpoint enxuto em `UserController`
que retorna só `{id: nome}` (`Map<Long, String>`), seguindo o mesmo padrão `keyValue()` já usado em
`PersonService`/`ProfileService`/`CompanyService`/etc. para alimentar dropdowns em outras telas do
projeto (`repository.keyValue()` com uma projeção JPQL `select id, name`, sem carregar a entidade
inteira).

Diferença importante: os outros `keyValue()` do projeto são sempre embutidos dentro de uma resposta de
"screen" (`UserScreenResponse`, etc.) e exigem a autoridade da tela correspondente (`USER_VIEW`).
Este é exposto como endpoint próprio e **sem** `@PreAuthorize("USER_VIEW")` — de propósito: expõe só
id+nome (nada sensível como email, role ou empresa), então qualquer usuário autenticado pode usá-lo
para escolher um destinatário, sem precisar da permissão administrativa de "ver usuários".

> A listagem já vem filtrada por empresa automaticamente (isolamento multi-tenant do projeto via
> `AbstractAuditableTenantEntity`/`TenantFilterAspect`, que também vale para `User`) — não é possível
> selecionar, e portanto notificar, um usuário de outra empresa pela UI.

## Disparo manual: `POST /api/v1/notifications/send`

```json
{ "userIds": [1, 2], "title": "Manutenção", "message": "Sistema entrará em manutenção às 22h", "type": "WARNING" }
```

O controller simplesmente publica uma `NotificationMessageDTO` por `userId` na mesma fila que
`/notifications/test` já usa — não existe conceito de "notificação em massa" no RabbitMQ aqui, é N
mensagens individuais, cada uma seguindo o fluxo normal (fila → listener → persiste → STOMP).

**Permissão dedicada**: diferente do picker (`/users/key-value`, que só expõe id+nome), disparar
notificações para outras pessoas é uma ação com efeito real sobre terceiros — por isso o endpoint
exige `@PreAuthorize("hasAuthority('NOTIFICATION_SEND')")`. Foi adicionado um novo valor em
`domain/config/ResourceEnum.java` (`NOTIFICATION_SEND`), seguindo exatamente o padrão já usado por
todo o resto do projeto (`EVENT_VIEW`, `USER_CREATE`, etc.):

- `AuthorizationSeeder` cria automaticamente o `Resource` correspondente no banco a cada start da
  aplicação (sincroniza `ResourceEnum` ↔ tabela `resource`).
- `MainSeeder` concede automaticamente todo `Resource` ainda não atribuído ao perfil "Admin" — por
  isso o usuário `admin@admin.com` já tem essa permissão sem nenhum passo manual.
- Para outros perfis, a permissão precisa ser concedida explicitamente pela tela de Perfis
  (`ProfilePermission`), como qualquer outra autoridade do sistema.

Validado manualmente revogando e restaurando a permissão do perfil Admin em banco: sem
`NOTIFICATION_SEND`, o endpoint responde `403`; com ela, `202`, confirmando que o `@PreAuthorize`
está de fato bloqueando quem não tem a autoridade.

**Atualização**: o botão "Enviar notificação" (em `NotificationBell.tsx`) agora só aparece para quem
tem `NOTIFICATION_SEND` — ver Parte 5, que generaliza isso para qualquer botão/ação do sistema, não só
este.

## `SendNotificationModal.tsx`

Modal separada (aberta por um botão dentro da modal de notificações — "simplifica o fluxo", como pedido)
com: lista de usuários com `Switch` por linha (mesmo padrão visual de `ProfilePermissionTable`, reaproveitado
aqui para seleção múltipla), campo de tipo (`Select`, mesmas 4 opções de `tpType`), título e mensagem.
Usa estado local simples (`useState`), não `react-hook-form`/`zod` como os formulários de CRUD do
projeto — decisão deliberada: isso não é um formulário de entidade persistida no front, é um "compor e
disparar" de uma ação pontual, então a infraestrutura mais pesada de formulário não se paga aqui.

---

# 🔑 Parte 5 — Permissões no front-end (`useHasAuthority`)

Até aqui, nenhuma tela do projeto verificava autoridade no client-side: o `@PreAuthorize` do backend
era a única proteção, e um usuário sem permissão só descobria isso ao tentar a ação (toast de erro
com o `403`). Funcional, mas ruim de UX — oferecer um botão que sempre vai falhar para aquele usuário.

Isso foi generalizado (não é específico de notificação) porque o mesmo problema existe em qualquer
tela do sistema com ações restritas por `@PreAuthorize`.

## Backend: `GET /api/v1/auth/me` agora retorna `authorities`

```json
{ "id": 1, "email": "admin@admin.com", "authorities": ["USER_VIEW", "EVENT_CREATE", "NOTIFICATION_SEND", ...] }
```

`AuthController.me()` mapeia `AuthenticatedUser.getAuthorities()` (a mesma lista que o Spring Security
já usa para avaliar `@PreAuthorize` em cada requisição) para uma lista de strings. Nenhuma fonte de
dado nova: é literalmente a mesma informação que o backend já carregava por requisição, só exposta.

## Frontend: `src/modules/auth/*`

- `auth.api.ts` / `auth.hooks.ts`: `useCurrentUser()` (`useQuery` sobre `/auth/me`, cacheado pelo
  `@tanstack/react-query` como qualquer outra query do projeto — sem Context/Provider dedicado, porque
  o react-query já dá o compartilhamento entre componentes que um Context daria aqui).
- `useHasAuthority(authority: string): boolean` — o hook que qualquer componente usa:
  ```tsx
  const canSend = useHasAuthority("NOTIFICATION_SEND");
  {canSend && <Button>Enviar notificação</Button>}
  ```
  Enquanto `/auth/me` ainda não respondeu, retorna `false` (falha fechado — some por padrão até
  confirmar que pode, evita um flash do botão aparecendo e sumindo).

## Onde já está em uso

`NotificationBell.tsx`: o botão "Enviar notificação" só renderiza com `NOTIFICATION_SEND`. Validado
manualmente revogando/restaurando a permissão em banco e conferindo `/api/proxy/auth/me` através do
proxy do Next.js (não só direto no backend) — a lista de authorities muda corretamente nos dois
sentidos.

## Limitação que continua valendo

Isso é **só uma camada de UX**, não de segurança — esconder um botão no client não impede ninguém com
acesso a `curl`/DevTools de chamar o endpoint diretamente. A proteção de verdade continua sendo o
`@PreAuthorize` no backend (catalogado em `docs/product/2_known-limitations.md`).

## Como uma feature futura reaproveitaria isso (ex: exportação de PDF)

1. Endpoint dispara `NotificationPublisher`-like: publica um payload numa fila própria (`pdf-export`,
   seguindo o mesmo padrão de `RabbitMQConfig`/`NotificationMessageDTO`/`NotificationPublisher`).
2. Um listener novo consome, processa (gera o PDF), salva o resultado, e chama
   `messagingTemplate.convertAndSendToUser(userId, "/queue/pdf-export", resultado)` — **zero mudança**
   em `WebSocketConfig` ou `StompAuthChannelInterceptor`.
3. No front, `useStompSubscription("/user/queue/pdf-export", callback)` — **zero mudança** em
   `StompProvider`.

---

# 🔁 Parte 6 — Retry + Dead Letter Queue no RabbitMQ

Antes desta parte, se `NotificationListener.receive()` lançasse uma exceção por qualquer motivo (bug,
mensagem malformada, banco fora do ar por um instante), o comportamento padrão do Spring AMQP era
reenfileirar a mensagem e tentar de novo **imediatamente, para sempre** — um loop infinito martelando o
listener e o broker, sem nenhuma visibilidade de que algo está errado.

## O que foi configurado

**Retry com backoff** (`application.yml`, `spring.rabbitmq.listener.simple.retry`): até 3 tentativas
por mensagem (a 1ª imediata, depois 1s antes da 2ª e 2s antes da 3ª — `initial-interval: 1000` ×
`multiplier: 2`) — dá tempo de uma falha transitória (ex: uma reconexão de banco) se resolver
sozinha, sem martelar imediatamente.

**Dead Letter Queue** (`RabbitMQConfig.java`): se mesmo após as 3 tentativas a mensagem continuar
falhando, `default-requeue-rejected: false` faz o Spring rejeitá-la sem reenfileirar. A fila principal
(`clientefacil.notification.queue`) tem os argumentos `x-dead-letter-exchange` /
`x-dead-letter-routing-key` apontando para uma exchange e fila dedicadas
(`clientefacil.notification.dlx` / `clientefacil.notification.queue.dlq`) — é o próprio RabbitMQ que
redireciona a mensagem rejeitada pra lá, sem nenhum código adicional no lado da aplicação.

Resultado: mensagens problemáticas ficam visíveis e paradas na DLQ (inspecionável em
`http://localhost:15672` → Queues → `clientefacil.notification.queue.dlq`) em vez de desaparecer
silenciosamente ou reprocessar pra sempre. Reprocessamento automático continua fora do escopo — mas a
Parte 7 abaixo cobre o que consome essa fila para dar visibilidade a quem chega lá.

## Como testar

Publicar uma mensagem que não corresponde ao formato de `NotificationMessageDTO` diretamente na
exchange (ex: via `http://localhost:15672` → Exchanges → `clientefacil.notification.exchange` →
Publish message, routing key `clientefacil.notification`, payload
`{"userId": "não-é-um-número", "type": "INFO", "title": "x", "message": "y"}`) força uma falha de
desserialização. Após ~3s (1s + 2s de backoff entre as 3 tentativas), a mensagem sai da fila
principal e aparece na DLQ.

> Validado manualmente exatamente assim: a fila principal voltou a `0` mensagens e a DLQ recebeu a
> mensagem malformada, confirmando que o loop infinito anterior não acontece mais.

---

# 🪦 Parte 7 — Consumindo a DLQ: auditoria em banco + alerta em tempo real

A DLQ do RabbitMQ sozinha resolve "não perder a mensagem", mas tem três limites: não é consultável
junto com o resto dos dados da aplicação, não tem retenção/alerta embutido, e não sobrevive a um reset
do ambiente (ex: `docker compose down -v`, que é um fluxo normal em dev). Por isso o padrão de mercado
para lidar com uma DLQ (o *Dead Letter Channel* de Enterprise Integration Patterns) normalmente soma
três peças: a fila em si (retenção/replay — já tínhamos), **um consumer dedicado da DLQ** que registra o
que aconteceu, e **alerta** de quem precisa saber. As duas últimas são o que esta parte adiciona.

## `notification_dead_letter` — auditoria mínima

Migration `V13_4__create_notification_dead_letter_table.sql` + `entity/NotificationDeadLetter.java`:
tabela simples, **sem `company_id`/tenant** (é um log operacional da infraestrutura de mensageria, não
um dado de negócio de uma empresa específica), com o payload cru da mensagem morta, o motivo (`reason`
do header `x-death` que o próprio RabbitMQ adiciona: `rejected`, `expired`, etc.), quantas vezes já foi
parar numa DLQ (`count`), quando, e `dt_resolved` (nulo = pendente — mesmo padrão de `dt_read` em
`notification`). Quem resolveu e quando fica em `updated_by`/`updated_at` (herdados de
`AbstractAuditableEntity`): resolver é a única mutação possível depois que o registro é criado, então
não precisa de uma coluna dedicada `resolved_by`.

## `messaging/NotificationDeadLetterListener.java`

```java
@RabbitListener(queues = RabbitMQConfig.NOTIFICATION_DLQ)
public void receive(Message message) { ... }
```

Dois detalhes de design que valem a pena registrar:

- **Consome `org.springframework.amqp.core.Message` (envelope cru), não `NotificationMessageDTO`.**
  A mensagem já demonstrou que pode estar malformada — foi por isso que ela chegou até aqui — então
  reaproveitar a mesma desserialização automática que originalmente falhou só criaria uma segunda
  chance de falhar por conversão. Lendo o `Message` cru, o body vira uma `String` sem risco de erro de
  parsing, e o parsing do header `x-death` é feito manualmente e de forma defensiva (`try/catch`),
  porque o formato desse header não é garantido pelo broker.
- **Não republica pela mesma fila/exchange de notificações.** Cogitamos reaproveitar 100% o pipeline
  existente (`NotificationPublisher` → fila → `NotificationListener`) para gerar uma notificação de
  alerta, mas isso criaria um risco real: se a causa da falha for sistêmica (ex: banco fora do ar), a
  própria notificação de alerta cairia na DLQ de novo, gerando outro alerta, em loop. Por isso o alerta
  é emitido **direto** via `SimpMessagingTemplate`, sem passar pelo RabbitMQ de novo.

## Alerta em tempo real: broadcast, não por usuário

```java
public static final String DEAD_LETTER_DESTINATION = "/topic/system/dead-letters";
messagingTemplate.convertAndSend(DEAD_LETTER_DESTINATION, new NotificationDeadLetterAlert(...));
```

Até agora só usamos destinos `/queue/...` (`convertAndSendToUser`, 1 para 1 — ver Parte 3). Esse é o
primeiro uso de `/topic` (já habilitado no broker desde o início, em `WebSocketConfig`): broadcast, para
qualquer sessão STOMP conectada que assine esse destino. `NotificationDeadLetterAlert` carrega só
metadados (id do registro, motivo, contagem, horário) — nunca o conteúdo original da notificação
(título/mensagem), que pertence a um usuário específico e pode ser sensível.

Validado manualmente com um cliente STOMP avulso (script Node com `@stomp/stompjs`, mesmo pacote do
frontend) publicando uma mensagem malformada e confirmando: linha em `notification_dead_letter`, log
estruturado (`ERROR`) e o alerta chegando no `/topic/system/dead-letters` em tempo real.

## Duas permissões novas: `DEAD_LETTER_VIEW` e `DEAD_LETTER_RESOLVE`

Mesmo padrão de `NOTIFICATION_SEND` (Parte 4): entradas em `ResourceEnum`, sincronizadas
automaticamente pelo `AuthorizationSeeder` e concedidas ao perfil "Admin" pelo `MainSeeder`. Duas em vez
de uma porque são capacidades diferentes — dá pra alguém enxergar o painel sem poder encerrar um
registro. `DEAD_LETTER_VIEW` também é o que decide **quem recebe o alerta** (próximo tópico) e se o item
de menu "Administração → Mensageria (DLQ)" aparece no front (`useHasAuthority`, `menu.ts`).

## Notificação real para quem administra a mensageria

Além do broadcast (que só serve quem está com o painel aberto no momento), `NotificationDeadLetterListener`
também gera uma notificação de verdade — aparece no sino, sobrevive a um refresh — para todo usuário com
`DEAD_LETTER_VIEW`:

```java
List<Long> adminUserIds = userRepository.findUserIdsByResourceSignature(ResourceEnum.DEAD_LETTER_VIEW.getSignature());
// para cada um: notificationService.create(...) + messagingTemplate.convertAndSendToUser(...)
```

Mesma ressalva da Parte 3: isso **não** passa pelo RabbitMQ (evita o loop já explicado acima) — é uma
chamada direta a `NotificationService`, igual o `NotificationListener` do fluxo normal faz, só que sem
depender da fila para chegar até lá. É best-effort (`try/catch` ao redor de tudo): se não houver nenhum
usuário com a permissão, ou a busca falhar, o processamento da DLQ em si não é afetado — só fica sem
avisar ninguém, o registro em `notification_dead_letter` continua existindo normalmente.

## Painel administrativo (`/dashboard/admin/dead-letters`)

Segue o padrão de listagem já estabelecido no projeto (`DefaultSearchRequest` +
`SpecificationBuilder`/`SortBuilder` no backend, `useTableState` + `DataTable` no front — o mesmo usado
em Empresas, Clientes etc.), com dois acréscimos:

- **Cards de resumo** (`NotificationDeadLetterStats.tsx`) — pendentes, resolvidos, últimas 24h, vindos
  de `GET /notifications/dead-letters/stats`. Pensado para crescer: cada novo "insight" vira só mais um
  campo em `NotificationDeadLetterStatsResponse` e mais um item no array de cards, sem mexer no resto.
- **Filtro de status pendente/resolvido** via `IS_NULL`/`IS_NOT_NULL` em `dtResolved` — o helper genérico
  do front (`makeSearchRequest`, sempre `LIKE`) não cobria isso, então `notificationDeadLetter.api.ts`
  monta os `FilterRequest` manualmente em vez de usar `createCrudApi`. Aproveitando isso, também corrigi
  o tipo `FilterOperator` do front (`src/shared/types/form.type.ts`), que estava com nomes que não
  batiam com o enum real do backend (`EQUALS`/`GREATER_THAN`/... em vez de `EQ`/`GT`/...) — não
  quebrava nada porque nada além do `LIKE` hardcoded era usado até agora, mas teria mascarado um erro
  silencioso no primeiro filtro novo que precisasse de outro operador.

Ação "Resolver" na tabela: `PATCH /notifications/dead-letters/{id}/resolve`, só visível para quem tem
`DEAD_LETTER_RESOLVE` (`useHasAuthority`) e só para itens ainda pendentes.

---

# 📧 Parte 8 — Serviço de e-mail (SMTP dinâmico por empresa, iniciado pelo dead-letter)

A Parte 7 deixou documentado como limitação: o alerta de dead-letter só existia dentro do app (sino +
broadcast), ninguém era avisado se estivesse deslogado. Esta parte resolve isso implementando um
serviço de e-mail genérico — não amarrado só a dead-letter, pensado desde já para outros usos futuros
(recuperação de senha, etc), com dead-letter sendo só o primeiro consumidor real.

## `mail_config` — SMTP multi-tenant com fallback pra config base

Cada empresa pode ter sua própria configuração de SMTP, mas nem todo e-mail pertence a uma empresa
(ex: alerta de dead-letter é infraestrutura do sistema, não de um tenant). Por isso `company_id` em
`mail_config` é **nullable**: `NULL` = "config base" do sistema (usada por e-mails sem empresa
associada), preenchido = config própria de uma empresa, que tem prioridade sobre a base quando existe
(`MailConfigService.resolveEffectiveConfig`). Não por acaso isso espelha exatamente o mecanismo de
fallback que o `tenantFilter` do Hibernate já usa (`company_id = :companyId OR company_id IS NULL`,
ver `AbstractAuditableTenantEntity`) — uma config base fica automaticamente visível pra qualquer
empresa autenticada sem lógica de query extra.

Migrations: `V14_1__create_mail_encryption_type_enum.sql`, `V14_2__create_mail_config_table.sql`
(índices únicos parciais garantem no máximo 1 config base e 1 por empresa). Entidade:
`entity/MailConfig.java`. A senha SMTP (`ds_password`) é criptografada com AES-256/GCM
(`core/crypto/MailPasswordConverter`, um `@Converter` JPA) — precisa ser reversível (diferente de
senha de usuário, com hash), já que o `EmailListener` precisa da senha em claro pra autenticar no
SMTP. Chave via `MAIL_CONFIG_ENCRYPTION_KEY` (mesmo padrão do `jwt.secret`).

## Fila própria de e-mail, separada da fila de notificações

`core/config/EmailRabbitMQConfig.java` cria um exchange/fila/DLX/DLQ dedicados
(`clientefacil.email.*`), com o mesmo padrão de retry+DLQ da Parte 6. Deliberadamente **não**
reaproveita a fila de notificações: o alerta de dead-letter já é gerado a partir do
`NotificationDeadLetterListener`, então se o envio de e-mail também passasse pela fila/DLQ de
notificação, uma falha sistêmica (ex: SMTP fora do ar) poderia realimentar esse mesmo pipeline. Filas
separadas mantêm os dois domínios de infraestrutura independentes.

- `messaging/EmailMessageDTO.java` / `EmailPublisher.java`: producer fire-and-forget, mesmo espírito
  de `NotificationPublisher`.
- `messaging/EmailListener.java` (consumer): resolve a config efetiva
  (`MailConfigService.resolveEffectiveConfig`), monta um `JavaMailSenderImpl` **dinâmico** a cada
  envio (não é um bean fixo — é isso que permite cada empresa ter seu próprio SMTP), renderiza o
  template Thymeleaf e envia. Qualquer exceção aciona o retry+DLQ padrão do Spring AMQP.
- `messaging/EmailDeadLetterListener.java`: consome a DLQ de e-mail. Reaproveita a mesma tabela/
  entidade `notification_dead_letter` da Parte 7 (ganhou a coluna `tp_origin`,
  `NOTIFICATION`/`EMAIL`, editada direto na migration `V13_4` já que o projeto ainda não estava em
  produção) em vez de criar uma tabela dedicada — auditoria idêntica, mesmo tratamento defensivo do
  header `x-death`. Também não republica outro e-mail pra avisar da própria falha (evita loop); o
  aviso fica restrito ao broadcast `/topic/system/dead-letters` já existente.

## `EmailService` — API reutilizável

`service/EmailService.java` expõe `sendTemplated(companyId, to, subject, template, variables)` —
qualquer feature do projeto usa isso pra disparar e-mail, sem lidar com RabbitMQ diretamente.
Templates em `resources/templates/email/*.html` (Thymeleaf): `dead-letter-alert.html` (primeiro
consumidor real) e `test-email.html` (usado pela rota de teste).

`NotificationDeadLetterListener` (Parte 7) foi atualizado: além da notificação em tempo real e do
registro em banco, agora também chama `emailService.sendTemplated(null, adminEmail, ...)` pra cada
usuário com `DEAD_LETTER_VIEW` que tenha e-mail cadastrado — best-effort, mesma filosofia do resto do
método.

## Endpoints (`controller/MailConfigController.java`)

CRUD da config base e da config da empresa autenticada, mais uma rota de teste (mesmo espírito de
`POST /notifications/test`):

- `GET/PUT/DELETE /api/v1/mail-configs/base` — config do sistema (`company_id IS NULL`)
- `GET/PUT/DELETE /api/v1/mail-configs/company` — config da empresa autenticada
- `POST /api/v1/mail-configs/test` — `{"scope": "BASE"|"COMPANY", "to": "..."}`, dispara um e-mail de
  teste com a config resolvida (`202 Accepted`)

Duas permissões (`MAIL_CONFIG_VIEW`, `MAIL_CONFIG_MANAGE`) em vez do padrão CRUD de 4 usado pelas
entidades normais do projeto — `mail_config` é um recurso singleton (config base + no máximo 1 por
empresa), não uma lista, então criar/editar são a mesma ação de "salvar a configuração".

> Trade-off documentado por completude: a config base é protegida pela mesma `MAIL_CONFIG_MANAGE` da
> config da própria empresa, não por um papel "super-admin" cross-tenant (que o projeto ainda não
> modela) — qualquer empresa com essa permissão consegue alterar o envio de e-mails do sistema.
> Aceitável na fase atual (pré-produção); catalogado em `docs/product/2_known-limitations.md` e
> `docs/product/3_roadmap.md`.

## Ambiente de dev: MailHog

`docker-compose.yml` ganhou o serviço `mailhog` (SMTP mock, UI em `http://localhost:8025`) — qualquer
e-mail enviado pela aplicação em dev cai lá, não é entregue de verdade. `MainSeeder` cria a config
base automaticamente no primeiro start (se nenhuma existir) apontando pro MailHog, sem autenticação
nem TLS. Pra receber de verdade num e-mail real, configure um SMTP de verdade via
`PUT /api/v1/mail-configs/base` (ou `/company`).

---

# ⚙️ Parte 9 — Tela de Configurações (front) + simulação de falha sob demanda

A Parte 8 fechou o backend de e-mail mas deixou o front sem nenhuma tela pra usar: nem a empresa
conseguia configurar seu próprio SMTP, nem o usuário tinha como trocar a própria senha. Esta parte
fecha essas duas pontas e ainda dá um jeito de provar, sob demanda, que o pipeline de retry+DLQ+
alerta (Partes 6 e 7) está funcionando de verdade — sem precisar esperar uma falha real acontecer.

## Botão de engrenagem → `/dashboard/settings`

Um ícone (`Settings`, lucide-react) ao lado do `<NotificationBell />` no header
(`app/dashboard/layout.tsx`), sempre visível — leva pra uma tela com dois cards.

## Card "Configurações Gerais" — troca de senha self-service

Não existia nenhum endpoint pra isso: o único update de usuário era o admin (`PUT
/api/v1/users/{id}`), que não pede senha atual e não é o que um usuário comum deveria poder chamar
pra si mesmo. Novo endpoint `PATCH /api/v1/users/me/password`
(`UserController`/`UserService.changeMyPassword`) — sem `@PreAuthorize` de recurso (estar autenticado
já é a permissão, id resolvido via `SecurityUtil`, mesmo espírito de `/auth/me`), exige a senha atual
(`passwordEncoder.matches`) antes de aceitar a troca. Front: `modules/settings/*`.

## Card "Configurações de E-mail" — só a config da própria empresa

`modules/mailConfig/*`, visível só com `MAIL_CONFIG_VIEW` (campos e botões de salvar/testar somem
sem `MAIL_CONFIG_MANAGE`). Gerencia só `/mail-configs/company` — a config base (sistema,
cross-tenant) ficou de fora de propósito: não faz sentido misturar "configuração da minha empresa"
com uma config que afeta o sistema inteiro na mesma tela de settings pessoal. Base continua fora do
card por design — ganhou tela administrativa própria em `/dashboard/admin/mail-config` (ver
`docs/guides/2_authentication.md`, que documenta essa tela — foi implementada junto com a rodada de
autenticação), reaproveitando o mesmo `MailConfigCard` com um prop `scope`.

### "Testar Conexão" com dados ainda não salvos

O `POST /mail-configs/test` da Parte 8 só testa a config **já persistida** — não servia pro botão
"Testar Conexão" do formulário, que precisa validar o que está digitado *agora*, antes de salvar.
Novo endpoint síncrono `POST /mail-configs/test-draft` (`MailConfigDraftTestRequest` →
`MailConfigService.testDraft`): monta um `MailConfig` **transiente** (nunca persistido) com os dados
do formulário e envia de verdade, na hora — resposta 200/erro já na própria chamada HTTP, em vez do
202 fire-and-forget do `/test`. Senha em branco no draft segue a mesma regra do save ("mantém a já
salva"), resolvida a partir do scope (`resolvePersistedPassword`) — mas só é exigida se o formulário
tiver um usuário preenchido; sem usuário (ex: MailHog, sem autenticação) o teste segue sem senha
nenhuma, mesma regra de "auth só se tiver username" que o envio de verdade já aplica.

A lógica de montar/enviar e-mail (JavaMailSenderImpl, template Thymeleaf) foi extraída do
`EmailListener` pro novo `EmailSenderService`, reaproveitado tanto pelo fluxo assíncrono (fila) quanto
por esse teste síncrono — sem duplicar a montagem do e-mail em dois lugares.

## Painel `admin/dead-letters` — simular falha sob demanda

Faltava uma forma de provar que o retry+DLQ+alerta (Partes 6 e 7) está 100% funcional sem esperar uma
falha real (SMTP fora do ar, bug em produção) acontecer. Dois botões novos na página (`FlaskConical`/
`Mail`, gated por uma permissão nova `DEAD_LETTER_TEST`) publicam uma mensagem com um valor-sentinela
que o listener correspondente reconhece e rejeita de propósito, **antes** de tentar qualquer trabalho
real:

- `NotificationListener.SIMULATED_FAILURE_MESSAGE` — checado no início de `receive(...)`.
- `EmailListener.SIMULATE_FAILURE_VARIABLE` — uma chave no `variables` do `EmailMessageDTO`, checada
  antes de resolver config ou tocar em SMTP.

Deliberado não adicionar um campo novo em `NotificationMessageDTO`/`EmailMessageDTO` pra isso — esses
records são consumidos em vários pontos do sistema; um valor-sentinela dentro dos campos que já
existem (`message`/`variables`) tem o mesmo efeito sem mexer no contrato. A falha esgota os 3 retries
normalmente (~3s, `spring.rabbitmq.listener.simple.retry`) e cai na DLQ pelo caminho já existente —
o front reinvalida a listagem ~4s depois do disparo pra já mostrar o registro novo sem refresh manual.
Confirmado nesta rodada: a falha de origem `NOTIFICATION` dispara o alerta por e-mail (`DEAD_LETTER_VIEW`),
a de origem `EMAIL` não (evita loop, comportamento já documentado na Parte 8).

---

## Regras de negócio, limitações e roadmap

Movidos pra `docs/product/` (pasta única pra esse tipo de conteúdo em todo o projeto, não só
mensageria) — evita a mesma informação ficando desatualizada em dois lugares:

- `docs/product/1_business-rules.md` — regras de negócio (multi-tenant, permissões, retry/DLQ,
  alerta por e-mail, etc.).
- `docs/product/2_known-limitations.md` — trade-offs aceitos conscientemente.
- `docs/product/3_roadmap.md` — o que ainda falta.

A tabela `notification`, a persistência, o roteamento por usuário via STOMP (autenticado via
ws-ticket), as permissões dedicadas, o retry/DLQ do RabbitMQ, o painel administrativo de dead
letters e o serviço de e-mail (Parte 8) **já são produção-viáveis** — falta só configurar um SMTP
real (ver roadmap).
