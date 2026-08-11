# Por onde começar a estudar

Passo a passo pra entender, na prática, tudo que foi implementado em mensageria (RabbitMQ),
tempo real (WebSocket/STOMP), e-mail e autenticação — nessa ordem, porque cada fase usa o que a
anterior já construiu. Cada fase tem **leitura** (o "por quê", nos guias) e **mão na massa** (rodar
de verdade e ver acontecer, não só ler).

Pré-requisito: ambiente no ar (`cd ../cliente-facil-docker && docker compose up -d`) e logado como
`admin@admin.com` / `123456` em `http://localhost:3000`. Painéis úteis pra deixar abertos no
navegador durante todo o estudo:

- RabbitMQ: `http://localhost:15672` (`guest`/`guest`)
- MailHog: `http://localhost:8025`
- Swagger: `http://localhost:8080/swagger-ui/index.html` (`Authorize` com o token do login)

---

## Fase 0 — Vocabulário de mensageria

**Leia**: `docs/guides/1_messaging-and-websocket.md`, Parte 1 (tabela de conceitos: Producer, Exchange,
Queue, Binding, Consumer).

**Faça**: abra o painel do RabbitMQ → aba **Exchanges** e **Queues**. Ache
`clientefacil.notification.exchange` e `clientefacil.notification.queue`. Não precisa entender tudo
ainda, só reconhecer visualmente as peças que o texto acabou de descrever.

**Checkpoint**: consegue explicar, com suas palavras, por que o producer nunca fala diretamente com a
fila?

---

## Fase 1 — Notificação de ponta a ponta (fila → banco → tempo real)

**Leia**: Partes 1-3 inteiras (RabbitMQ básico, a tabela `notification`, por que STOMP em vez de
WebSocket cru).

**Faça**:
1. Pelo Swagger, chame `POST /api/v1/notifications/test` com `{"content": "Meu primeiro teste", "type": "SUCCESS"}`.
2. **Antes** de olhar o front, vá no RabbitMQ → Queues → `clientefacil.notification.queue` e dê
   refresh rápido — se for rápido o suficiente, dá pra ver a mensagem passar (ela costuma ser
   consumida em milissegundos).
3. Agora olhe o sino no `http://localhost:3000/dashboard` — a notificação já está lá, sem refresh.
4. Dê um F5 na página. A notificação continua — ela não veio só "ao vivo", está persistida.

**Checkpoint**: se você desligasse o RabbitMQ bem no meio do passo 1 (entre o `202 Accepted` e o
consumo), o que aconteceria com a notificação quando o RabbitMQ voltasse? (Resposta: fica na fila,
esperando; nada se perde — é essa a vantagem de ser assíncrono via fila em vez de síncrono.)

---

## Fase 2 — Permissões: o que muda quem vê o quê

**Leia**: Parte 4 (picker de destinatários sem permissão especial vs. `/send` com
`NOTIFICATION_SEND`) e Parte 5 (`useHasAuthority` no front).

**Faça**:
1. `GET /api/v1/auth/me` pelo Swagger — repare na lista `authorities`.
2. Na tela de Perfis (`/dashboard/profile`), abra o perfil "Admin" e desmarque `NOTIFICATION_SEND`.
3. Volte pro dashboard: o botão "Enviar notificação" (dentro do sino) sumiu — sem precisar de F5 forçado,
   só reabrir a modal (o `useCurrentUser` já teria revalidado).
4. Tente chamar `POST /api/v1/notifications/send` mesmo assim pelo Swagger — `403`, mesmo sem o botão
   existir mais. Marque `NOTIFICATION_SEND` de volta no perfil.

**Checkpoint**: por que o passo 4 dá 403 mesmo tendo escondido o botão? (Ver `docs/product/2_known-limitations.md` — `useHasAuthority` é só UX.)

---

## Fase 3 — Quando dá errado: retry, Dead Letter Queue e alerta

Essa é a parte mais rica pra estudar, porque mistura RabbitMQ "de verdade" (motivo pelo qual filas de
mensagem existem: lidar com falha) com auditoria e alerta em tempo real.

**Leia**: Partes 6 e 7 inteiras.

**Faça** (o exercício mais valioso do documento — força uma falha de verdade e observa o RabbitMQ
reagir sozinho):
1. RabbitMQ → Exchanges → `clientefacil.notification.exchange` → **Publish message**. Routing key
   `clientefacil.notification`, payload:
   ```json
   {"userId": "não-é-um-número", "type": "INFO", "title": "x", "message": "y"}
   ```
2. Cronometre. Fique olhando `clientefacil.notification.queue` (aba Queues) — o contador de
   mensagens sobe pra 1 e, uns 3 segundos depois, volta pra 0.
3. Olhe `clientefacil.notification.queue.dlq` — ganhou 1 mensagem.
4. No front, `/dashboard/admin/dead-letters` — o registro apareceu na tabela, e (se você estava com a
   página aberta) viu o alerta chegar em tempo real também.
5. Repita o passo 1, mas dessa vez pelo caminho "de produção": use os botões "Simular falha
   (notificação)"/"Simular falha (e-mail)" na própria tela de dead-letters — mesma coisa acontecendo,
   só que sem precisar montar o payload malformado à mão (Parte 9 explica o mecanismo do
   valor-sentinela por trás desses botões).

**Checkpoint**: por que o consumer da DLQ (`NotificationDeadLetterListener`) lê `Message` cru em vez
de `NotificationMessageDTO`? E por que ele nunca republica um alerta pela mesma fila de notificação?
(As duas respostas estão na Parte 7 — é sobre evitar repetir o mesmo erro de novo.)

---

## Fase 4 — E-mail: SMTP dinâmico por empresa

**Leia**: Parte 8 inteira, depois Parte 9 (seção "Testar Conexão com dados ainda não salvos").

**Faça**:
1. `/dashboard/settings` → card "Configurações de E-mail" → preencha host `mailhog`, porta `1025`,
   deixe usuário/senha em branco, criptografia `Nenhuma` → **Testar Conexão** (não precisa salvar
   antes).
2. Confira em `http://localhost:8025` — o e-mail chegou, com os dados que você digitou (não com uma
   config antiga salva).
3. Agora clique **Salvar**, e teste de novo — repare que dessa vez também funciona, usando o que
   ficou persistido.
4. Vá em `/dashboard/admin/mail-config` (só aparece no menu "Administração" se seu perfil tiver
   `MAIL_CONFIG_VIEW`) — é a mesma tela, só que pra config **base** do sistema em vez da sua empresa.

**Checkpoint**: por que "Testar Conexão" não passa pela fila de e-mail (`EmailPublisher`), diferente
do envio real? (Resposta na Parte 9: precisa responder no mesmo request HTTP, síncrono.)

**Opcional, pra ir além**: `docs/guides/4_circuit-breaker-smtp.md` — o que acontece quando o SMTP
configurado cai de vez (não só uma falha isolada), e por que retry+DLQ sozinho não é suficiente pra
isso.

---

## Fase 5 — Autenticação: confirmação de e-mail e recuperação de senha

**Leia**: `docs/guides/2_authentication.md` inteiro (é mais curto que o de mensageria).

**Faça** (crie um usuário de teste do zero e viva o fluxo completo como se fosse ele):
1. `/dashboard/user` → criar um usuário novo com um e-mail seu de teste.
2. Tente logar com ele imediatamente (aba anônima) → bloqueado, com a mensagem explicando o motivo.
3. Veja o e-mail de confirmação no MailHog, copie o link, abra — confirmação feita.
4. Logue de novo com esse usuário → funciona agora.
5. Na tela `/dashboard/user` (logado como admin), repare no badge "Confirmado" que já apareceu nessa
   linha. Crie um segundo usuário e **não** confirme — o badge fica "Pendente" e o botão "Reenviar
   confirmação" aparece.
6. Com o segundo usuário (ainda não confirmado): vá em `/auth/forgot-password`, peça recuperação de
   senha, pegue o link no MailHog, defina uma senha nova.
7. Tente logar com a senha nova → ainda bloqueado (e-mail não confirmado)! Recuperação de senha e
   confirmação de e-mail são checagens **independentes** — resetar a senha não confirma o e-mail.

**Checkpoint**: por que `POST /auth/forgot-password` sempre responde `202`, mesmo pra um e-mail que
não existe no sistema? (Ver `docs/product/1_business-rules.md`, seção "Autenticação e contas de
usuário" — é uma decisão de segurança, não um detalhe técnico.)

---

## Fase 6 — Como os templates de e-mail são tipados

Depois de já ter visto 4 templates diferentes disparando (confirmação, recuperação de senha, teste,
alerta de DLQ), vale entender como o projeto garante que o Java realmente passa o que cada `.html`
espera — sem essa garantia, um typo numa chave de `Map` só aparece olhando o e-mail renderizado.

**Leia**: `docs/guides/1_messaging-and-websocket.md`, Parte 10.

**Faça** (mesmo espírito da Fase 3 — quebra de propósito e observa a rede de segurança pegar o erro):
1. Abra `src/main/java/br/com/clientefacil/messaging/template/PasswordResetTemplate.java` e renomeie
   `resetUrl` pra qualquer outro nome. Salve.
2. Rode `./mvnw test -Dtest=EmailTemplateVariablesTest` — o build compila normal (é só um parâmetro
   posicional), mas o teste falha, apontando exatamente o `.html` e o record em conflito.
3. Desfaça a mudança, rode de novo — volta a passar.
4. Abra `src/main/java/br/com/clientefacil/messaging/template/EmailConfirmationTemplate.java` e crie
   um record novo qualquer nesse mesmo pacote (pode ser uma cópia sem sentido, só pra ver) — rode o
   teste de novo: ele aparece na lista de testes descobertos automaticamente, sem você ter escrito
   nenhuma linha de teste nova.

**Checkpoint**: por que o teste instancia cada record com todos os campos `null` em vez de valores de
verdade? (Resposta na Parte 10: só os *nomes* dos campos importam pra essa checagem, não os valores.)

---

## Fase 7 — Fechando o quadro: por quê, não só como

Depois das fases acima (o "como"), vale a leitura mais rápida do "por quê" consolidado:

- `docs/product/1_business-rules.md` — as regras que guiaram as decisões de design em todas as fases
  anteriores, num lugar só.
- `docs/product/2_known-limitations.md` — cada trade-off que você provavelmente notou durante os
  exercícios (ex: ticket de WS não atado a IP, config base compartilhando permissão) já catalogado
  aqui, com o motivo.
- `docs/product/3_roadmap.md` — o que ainda falta, caso queira continuar implementando a partir daqui.

Nesse ponto, você já rodou de verdade cada peça do sistema — fila, retry, DLQ, tempo real, e-mail,
autenticação e a rede de segurança de tipos por trás dos templates — não só leu sobre elas.
