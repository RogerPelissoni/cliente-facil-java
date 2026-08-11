# ⚡ Circuit breaker no envio de e-mail (SMTP)

Complementa a Parte 6 (Retry + Dead Letter Queue) e a Parte 8 (serviço de e-mail) de
`docs/guides/1_messaging-and-websocket.md` — leia essas duas antes desta, os conceitos abaixo partem
delas. Este doc cobre uma pergunta que ficou em aberto no roadmap: **e se o SMTP configurado por uma
empresa cair? Hoje é só retry+DLQ — cada mensagem, individualmente, martela até 3 tentativas antes de
desistir, mesmo sabendo (pelas mensagens anteriores) que o SMTP está fora.**

## O problema, em concreto

O `EmailListener` (consumer da fila de e-mail) já tinha retry+DLQ desde a Parte 6: se o envio falhar,
tenta de novo até 3 vezes com backoff crescente (1s, 2s, 4s) antes de desistir e cair na DLQ. Isso
resolve "o que fazer com uma mensagem que falhou", mas não resolve um problema anterior: **cada
mensagem nova continua tentando o SMTP do zero, mesmo que as últimas 10 tenham falhado por estar fora
do ar.**

Dois efeitos concretos disso, sem circuit breaker:

1. **Tempo desperdiçado por mensagem.** Sem timeout configurado, `JavaMailSenderImpl` usava o timeout
   padrão do socket do sistema operacional pra descobrir que uma conexão não vai responder — podendo
   passar bem de um minuto por tentativa, vezes 3 tentativas, por mensagem. Com um SMTP fora do ar e
   várias mensagens na fila (ex: uma leva de recuperação de senha depois de um pico de uso), isso
   soma rápido.
2. **A única thread do listener fica presa nisso.** O teste de burst do RabbitMQ já documentou (ver
   `docs/product/3_roadmap.md`, seção Testes) que o listener de notificação roda com concorrência 1 —
   o de e-mail é igual. Enquanto uma tentativa de SMTP está pendurada, nenhuma outra mensagem da fila
   é processada, nem as de empresas cujo SMTP está funcionando normalmente.

O circuit breaker ataca os dois: depois de falhas suficientes, para de tentar de verdade (resolve o
efeito 2 quase imediatamente) — e, para as tentativas que ainda chegam a acontecer, um timeout
explícito de SMTP (novo, ver abaixo) garante que elas falhem rápido em vez de pendurar (resolve o
efeito 1).

## O padrão: Circuit Breaker

Mesma ideia de um disjuntor elétrico — o nome não é força de expressão. Um circuit breaker de software
fica observando o resultado das últimas chamadas a uma dependência externa (aqui, o envio SMTP) e
alterna entre três estados:

- **`CLOSED`** (fechado = corrente passa): estado normal. Toda chamada é tentada de verdade contra o
  SMTP, e o resultado (sucesso/falha) é contado.
- **`OPEN`** (aberto = corrente cortada): abriu porque falhas recentes demais se acumularam. Toda
  chamada nova é **rejeitada na hora**, sem sequer tentar o SMTP — lança
  `CallNotPermittedException` em vez de esperar uma resposta (ou timeout) de rede.
- **`HALF_OPEN`** (testando): depois de um tempo em `OPEN`, o circuito deixa passar um número limitado
  de chamadas reais pra "sondar" se o SMTP voltou. Se essas poucas chamadas forem bem, volta pra
  `CLOSED`; se ainda falharem, volta pra `OPEN` e espera mais um pouco.

```
   ┌─────────┐   falhas acima do limiar    ┌────────┐
   │ CLOSED  │ ───────────────────────────▶│  OPEN  │
   │ (normal)│                              │(corta) │
   └────┬────┘                              └───┬────┘
        ▲                                       │ espera wait-duration-in-open-state
        │        sondas OK                       ▼
        │   ┌─────────────────────────────┐
        └───│         HALF_OPEN            │
            │ (deixa passar N chamadas)     │
            └───────────────┬───────────────┘
                             │ sondas falham
                             ▼
                           OPEN de novo
```

**Por que isso é diferente de simplesmente "menos retry"**: retry lida com uma falha **pontual** numa
mensagem específica (ex: um timeout isolado). Circuit breaker lida com uma falha **sistêmica** que se
repete entre mensagens diferentes (ex: o SMTP inteiro fora do ar) — os dois se complementam, não se
substituem. É por isso que o retry+DLQ da Parte 6 continua existindo exatamente como estava: o circuit
breaker só decide *se vale a pena tentar de verdade*; quem decide *quantas vezes tentar uma mensagem
específica* continua sendo o Spring AMQP.

## Onde está implementado

Biblioteca: [Resilience4j](https://resilience4j.readme.io/) (`resilience4j-spring-boot3`) — escolhida
por ser o padrão de fato no ecossistema Spring Boot pra esse tipo de proteção (o antigo
Netflix Hystrix está descontinuado), leve, e com integração pronta com o Actuator que o projeto já
usa.

### `messaging/EmailListener.java` — onde a proteção é aplicada

O envio de verdade (`emailSenderService.send(...)`) passa a rodar dentro de
`circuitBreaker.executeCallable(...)`:

```java
MailConfig config = mailConfigService.resolveEffectiveConfig(message.companyId());
CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName(config));

try {
    circuitBreaker.executeCallable(() -> {
        emailSenderService.send(config, message.to(), message.subject(), message.template(), message.variables());
        return null;
    });
} catch (CallNotPermittedException e) {
    log.warn("Circuito de e-mail aberto para '{}' — envio abortado sem tentar o SMTP...", circuitBreaker.getName());
    throw e; // ainda aciona o retry+DLQ de sempre pra ESTA mensagem
}
```

Importante: `CallNotPermittedException` (circuito `OPEN`) **continua propagando** pro
`@RabbitListener`, então o retry+DLQ do Spring AMQP continua acontecendo normalmente pra essa
mensagem — só que cada uma das 3 tentativas agora é instantânea (rejeitada localmente) em vez de
esperar uma resposta de rede que sabidamente não vai vir.

### Um circuito por empresa, não um circuito global

```java
private String circuitBreakerName(MailConfig config) {
    return "email-smtp-" + (config.getCompanyId() != null ? config.getCompanyId() : "base");
}
```

Cada empresa pode ter seu próprio SMTP (`mail_config`, ver Parte 8) — o SMTP da empresa A cair não
deve impedir e-mail da empresa B, nem da config base do sistema. `CircuitBreakerRegistry.
circuitBreaker(nome)` cria a instância sob demanda na primeira vez que um nome é pedido (não precisa
declarar cada empresa antecipadamente) e reaproveita a mesma instância (com seu próprio histórico e
estado) em toda chamada seguinte com o mesmo nome.

### `EmailSenderService.java` — timeout explícito de SMTP

```java
props.put("mail.smtp.connectiontimeout", "5000");
props.put("mail.smtp.timeout", "5000");
props.put("mail.smtp.writetimeout", "5000");
```

Sem isso, mesmo uma chamada que passa pelo circuito (`CLOSED` ou sondando em `HALF_OPEN`) podia
pendurar por muito mais tempo que os 5s acima — o timeout é o que garante que uma chamada real falhe
rápido o bastante pra não travar a thread do listener nem atrasar a decisão do circuito.

### `application.yml` — configuração do circuito

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
        record-exceptions:
          - org.springframework.mail.MailException
```

| Parâmetro | Valor | Por quê |
|---|---|---|
| `sliding-window-type` | `COUNT_BASED` | Conta as últimas N *chamadas*, não uma janela de *tempo* — mais previsível com volume baixo/irregular de e-mail (uma janela de tempo fixa podia nunca acumular chamada suficiente pra decidir algo). |
| `sliding-window-size` | 10 | Últimas 10 chamadas entram na conta da taxa de falha. |
| `minimum-number-of-calls` | 5 | Não decide nada com menos de 5 chamadas — evita abrir o circuito por coincidência (ex: 2 falhas em 2 tentativas) logo após reiniciar a aplicação. |
| `failure-rate-threshold` | 50% | Metade ou mais das últimas chamadas falhando = o SMTP está mesmo fora, não foi um problema pontual. |
| `wait-duration-in-open-state` | 30s | Tempo mínimo aberto antes de voltar a sondar — curto o bastante pra detectar rápido um SMTP que caiu e voltou, longo o bastante pra não martelar um que ainda está fora. |
| `permitted-number-of-calls-in-half-open-state` | 3 | Ao sondar se o SMTP voltou, só deixa passar 3 chamadas reais antes de decidir fechar ou reabrir — não volta a martelar em cheio de uma vez. |
| `automatic-transition-from-open-to-half-open-enabled` | `true` | O próprio Resilience4j agenda a transição `OPEN → HALF_OPEN` sozinho (thread interna), sem precisar que uma mensagem nova chegue pra "descobrir" que já pode testar de novo. |
| `record-exceptions` | `MailException` | Só falha de SMTP de verdade (conexão recusada, timeout, autenticação, envio rejeitado — tudo que o Spring embrulha em `MailException`) conta pro circuito. Um bug de template Thymeleaf, por exemplo, continua falhando a mensagem normalmente (retry+DLQ), mas **não** é tratado como "o SMTP está fora" — não faria sentido abrir o circuito por um erro que não se resolve sozinho só porque o circuito reabriu depois. |

Qualquer empresa nova (`email-smtp-<companyId>`) usa automaticamente essa config `default` — não
precisa declarar uma entrada por empresa em `instances:`.

## Observabilidade: `/actuator/circuitbreakers`

O módulo Resilience4j-Spring-Boot3 registra automaticamente dois endpoints novos no Actuator (mesma
proteção `SYSTEM_METRICS_VIEW` dos demais, ver `SecurityConfig`):

- `GET /actuator/circuitbreakers` — estado atual (`CLOSED`/`OPEN`/`HALF_OPEN`) e métricas (taxa de
  falha, número de chamadas) de cada circuito já criado.
- `GET /actuator/circuitbreakerevents` — histórico de transições de estado e chamadas
  permitidas/rejeitadas, útil pra investigar depois do fato ("quando esse circuito abriu?").

Não existe um circuito "email-smtp-\<id\>" até a primeira mensagem daquela empresa/base ser
processada — são criados sob demanda, não na inicialização.

## Como testar

1. Aponte a config base (ou de uma empresa) pra um host que não existe/recusa conexão (ex:
   `dsHost: "smtp-que-nao-existe.invalid"`) via `PUT /api/v1/mail-configs/base`.
2. Dispare e-mails repetidos (ex: `POST /api/v1/notifications/test` várias vezes, ou o botão "Testar
   Conexão" da tela de Configurações) — depois de ~5 falhas, o circuito abre.
3. Confira em `GET /actuator/circuitbreakers` que `email-smtp-base` (ou `email-smtp-<companyId>`)
   está em `OPEN`.
4. Nos logs, as próximas tentativas mostram o warning "Circuito de e-mail aberto para... abortado sem
   tentar o SMTP" — sem qualquer tentativa real de conexão.
5. Aponte a config de volta pro MailHog (`PUT /api/v1/mail-configs/base`) e espere os 30s de
   `wait-duration-in-open-state` — o próximo e-mail já sonda em `HALF_OPEN`, e volta a `CLOSED` se der
   certo.
6. Testes automatizados: `src/test/java/.../messaging/EmailListenerTest.java` cobre abertura do
   circuito após falhas reais, isolamento por empresa (SMTP de uma empresa não afeta outra), e que
   falhas fora da hierarquia `MailException` (ex: bug de template) não contam pro circuito.

## O que este trabalho **não** faz (roadmap)

- **Não substitui o retry+DLQ** — os dois continuam coexistindo, com responsabilidades diferentes
  (ver "Por que isso é diferente de simplesmente 'menos retry'" acima).
- **Não avisa proativamente que um circuito abriu** — hoje só aparece via `/actuator/circuitbreakers`
  (consulta manual) ou nos logs. Um alerta automático (reaproveitando o mesmo canal do alerta de
  dead-letter, Parte 7) quando um circuito abre seria uma evolução natural, mas fora do escopo desta
  rodada.
- **Não cobre nenhuma outra dependência externa** — só o envio de e-mail (RabbitMQ e Postgres não têm
  circuit breaker próprio hoje; o RabbitMQ já tem seu próprio retry/DLQ, e uma queda de Postgres
  derruba a aplicação inteira, não é algo que um circuito isolado resolveria).
