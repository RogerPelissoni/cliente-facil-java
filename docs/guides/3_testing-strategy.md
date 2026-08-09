# 🧪 Testes automatizados: o que foi mapeado e por quê

Até `EmailTemplateVariablesTest` (Parte 10 de `1_messaging-and-websocket.md`), o projeto tinha só o
smoke test padrão do Spring Boot (`ClientefacilApplicationTests`, só confere que o contexto sobe).
Esta rodada varreu o sistema procurando lógica que valesse a pena testar de verdade — **não** é uma
meta de cobertura, é uma lista deliberadamente curta de pontos onde uma falha silenciosa custaria
caro (segurança, dinheiro perdido em SMTP mal configurado, dado vazando entre empresas) ou onde já
apareceu um bug real nesta sessão. CRUD simples (Cliente, Empresa, Pessoa, Evento — sem branch de
lógica, só mapear DTO → entidade → repositório) ficou de fora de propósito: testar isso é testar o
Spring Data/Hibernate, não o código do projeto.

## O que foi mapeado

| Classe testada | Por quê | O que quebraria silenciosamente sem o teste |
|---|---|---|
| `UserTokenService` | Peça de segurança mais sensível do projeto até aqui (ver `2_authentication.md`) | Token expirado/já usado/tipo errado aceito por engano; hash igual ao token cru |
| `AuthService` (`login`) | Três portões (usuário existe / senha bate / e-mail confirmado) em sequência | Um portão pulado silenciosamente libera login sem confirmar e-mail, por exemplo |
| `AuthService` (forgot/reset/confirm) | Delegação pro `UserTokenService` + link do e-mail montado certo | Link de reset apontando pra URL errada, sem nenhum erro visível até alguém clicar |
| `MailConfigService.resolveEffectiveConfig` | Fallback empresa → base, usado em **toda** mensagem da fila de e-mail | Company inativa não cair pra base = e-mail do sistema inteiro para de sair |
| `MailConfigService.testDraft` (resolução de senha) | Já teve um bug real aqui nesta sessão (draft sem usuário exigindo senha à toa) | Regressão exatamente desse bug, sem ninguém notar até testar manualmente |
| `NotificationDeadLetterListener.extractDeathInfo` | Parsing mais defensivo do projeto (header `x-death`, formato não garantido pelo broker) | Uma refatoração faz o parsing lançar exceção onde antes só teria valor `null` — e como está dentro de um listener de DLQ, quebraria justamente a peça que audita falhas |
| `AuthorizationSeeder` (ordem de boot) | Bug real desta sessão: sem `@Order`, admin podia nascer sem nenhuma permissão | Alguém remove o `@Order` numa limpeza de imports e ninguém percebe até um deploy "de azar" |
| `UserService` (guard clauses) | Reenviar confirmação pra quem já confirmou; trocar senha sem validar a atual | Guard clause removida por engano numa refatoração |
| `EmailListener`/`NotificationListener` (sentinela de simulação) | Contrato documentado no javadoc: rejeitar *antes* de gastar uma tentativa real | Botão "Simular falha" do painel admin passa a consumir SMTP/DB de verdade |

## Padrões usados

- **Mockito puro, sem subir Spring** (`@ExtendWith(MockitoExtension.class)`, sem `@SpringBootTest`) —
  os candidatos mapeados são lógica de serviço isolável; testar assim roda em milissegundos, sem
  precisar de banco. `EmailTemplateVariablesTest` (Parte 10) é a exceção que confirma a regra: não
  usa Mockito porque não testa lógica de serviço, testa um contrato entre arquivo `.html` e classe
  Java.
- **`SecurityContextHolder` de verdade em vez de mockar `SecurityUtil` estaticamente**
  (`MailConfigServiceTest.authenticateAsCompany`): monta um `AuthenticatedUser` real e autentica no
  contexto de segurança de verdade pro teste do escopo `COMPANY`. Mais fiel ao comportamento real do
  que teria sido usar `Mockito.mockStatic`, e evita a complexidade extra de mocking estático — só
  não esquecer o `SecurityContextHolder.clearContext()` no `@AfterEach`, senão um teste vaza
  autenticação pro próximo.
- **Visibilidade relaxada pra `package-private` só onde compensa** (`NotificationDeadLetterListener.
  extractDeathInfo`/`DeathInfo`, de `private` pra pacote): evita ter que montar um `Message` completo
  e mockar as outras 4 dependências da classe só pra exercitar uma função de parsing que não usa
  nenhuma delas. A API pública da classe não muda — só o pacote de teste ganha acesso.
- **Descoberta automática por classpath scan, não lista manual** — já documentado na Parte 10 de
  `1_messaging-and-websocket.md` (`EmailTemplateVariablesTest`); o mesmo raciocínio motivou manter os
  outros testes pequenos e focados em vez de um "mega teste" por classe.

## Validado que os testes realmente pegam erro (não só "passam")

Dois exemplos verificados de propósito nesta rodada, quebrando o código de verdade e confirmando a
falha antes de reverter:

- Removi o `@Order(Ordered.HIGHEST_PRECEDENCE)` do `AuthorizationSeeder` →
  `AuthorizationSeederOrderTest` falhou imediatamente, apontando exatamente o valor esperado vs. o
  real.
- (Parte 10, mesma prática) Renomear um campo de `PasswordResetTemplate` → `EmailTemplateVariablesTest`
  falhou apontando o `.html` e o record em conflito.

## Como rodar

```bash
./mvnw test                                    # suíte inteira
./mvnw test -Dtest=UserTokenServiceTest         # uma classe
./mvnw test -Dtest=EmailTemplateVariablesTest   # a suíte da Parte 10
```

47 testes no total desta rodada (mais o smoke test padrão), todos em milissegundos — nenhum sobe
Spring, nenhum precisa do Docker Compose no ar.
