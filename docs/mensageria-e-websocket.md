# 📨 Mensageria (RabbitMQ) + Tempo real (STOMP/WebSocket): Notificações

Guia de estudo sobre a implementação de mensageria assíncrona (RabbitMQ) e comunicação em tempo real
(WebSocket/STOMP) no Cliente Fácil, usando como caso de uso real um sistema de **notificações
persistidas por usuário**.

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
> complexidade adicional.

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
`@PreAuthorize` no backend (que já existia antes disso e não mudou). `useHasAuthority` só evita
oferecer, na interface, uma ação que o backend já ia recusar.

## Como uma feature futura reaproveitaria isso (ex: exportação de PDF)

1. Endpoint dispara `NotificationPublisher`-like: publica um payload numa fila própria (`pdf-export`,
   seguindo o mesmo padrão de `RabbitMQConfig`/`NotificationMessageDTO`/`NotificationPublisher`).
2. Um listener novo consome, processa (gera o PDF), salva o resultado, e chama
   `messagingTemplate.convertAndSendToUser(userId, "/queue/pdf-export", resultado)` — **zero mudança**
   em `WebSocketConfig` ou `StompAuthChannelInterceptor`.
3. No front, `useStompSubscription("/user/queue/pdf-export", callback)` — **zero mudança** em
   `StompProvider`.

## Limitações conhecidas (propositais, documentadas)

- **Sem retry/DLQ no RabbitMQ** — mensagens que falham no consumo voltam pra fila indefinidamente
  (requeue automático do Spring AMQP em caso de exceção no listener).
- **`GET /api/v1/notifications` não pagina** — retorna só as últimas 50; suficiente para um sino de
  notificações, mas um histórico completo precisaria do padrão de busca paginada já usado em outras
  entidades do projeto (`SearchRequest`/`SpecificationBuilder`).
Nenhuma dessas limitações impede o uso real do recurso — a diferença desta versão para a v1 é que a
tabela `notification`, a persistência e o roteamento por usuário via STOMP **já são produção-viáveis**;
só a autenticação do WebSocket (item 1) tem uma melhoria natural pendente.
