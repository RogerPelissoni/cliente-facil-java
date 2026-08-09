# Ideias de regras de negócio — módulos futuros

Levantamento pros módulos já arquitetados mas ainda não (ou só parcialmente) implementados:
**Subscription**, **Order/Product**, e as partes de **Event/EventService** e **Financial** que ainda
não existem. O sistema descrito — agenda de serviços que gera financeiro, venda de produto que
também gera financeiro, tudo por trás de uma assinatura SaaS — é um desenho muito próximo do que
softwares como Trinks, Booksy, Fresha, Vagaro e Mindbody já resolveram pra esse nicho (salão,
clínica, barbearia, estúdio, consultório, personal trainer). Este documento puxa os padrões que esse
tipo de sistema **amplamente adota**, cruzados com o que já existe no código — pra você validar o que
faz sentido pro seu negócio, não pra implementar tudo de uma vez.

Marcações usadas: **[JÁ EXISTE]** quando conferi que a base já está no código (evita redesenhar o que
já foi decidido); o resto é ideia nova.

---

## 📅 Agenda (`Event`/`EventService`)

**[JÁ EXISTE]** — vale mais maduro do que um primeiro módulo costuma ser: `Event` já tem ciclo de
status completo (`SCHEDULED`/`CONFIRMED`/`IN_PROGRESS`/`COMPLETED`/`CANCELLED`/`MISSED` — já modela
no-show), `EventTypeEnum` distingue atendimento de cliente vs. compromisso pessoal
(`APPOINTMENT`/`SERVICE`/`PERSONAL`), e `EventService` já vincula evento → cliente → profissional →
`AccountReceivable`.

- [ ] **Conflito de horário (double-booking)** — hoje não vi validação impedindo o mesmo profissional
  ter dois eventos sobrepostos. É a regra mais básica de qualquer sistema de agenda.
- [ ] **Horário de funcionamento** (da empresa e por profissional) + **bloqueio de agenda** (férias,
  folga, atestado) — pra não deixar agendar fora do expediente.
- [ ] **Buffer entre atendimentos** (tempo de preparo/limpeza entre um evento e outro).
- [ ] **Múltiplos serviços por atendimento ("comanda")** — hoje `EventService` é **1:1** com `Event`
  (um agendamento = um serviço = um profissional = um financeiro). Praticamente todo concorrente
  permite mais de um serviço na mesma visita (ex: corte + barba, cada um podendo ter um profissional
  diferente), fechando numa cobrança só. Se isso importar pro seu negócio, vale desenhar antes de
  `Order`/`Financial` crescerem em cima do modelo atual — ver seção "Order/Product" abaixo.
- [ ] **Duração padrão por serviço** — um catálogo de serviços (nome, duração, preço padrão) calcularia
  `dtEnd` automaticamente a partir de `dtStart` + duração. Hoje não encontrei uma entidade "Serviço"
  catalogável — `EventService` parece ser só o vínculo do agendamento em si, não um catálogo.
- [ ] **Recorrência** (repetir toda semana/mês no mesmo horário) — comum pra personal trainer,
  fisioterapia, etc.
- [ ] **Lista de espera** quando não há horário disponível no dia desejado.
- [ ] **Confirmação de agendamento pelo cliente, sem login** (link com token) — reaproveita
  exatamente o padrão de token de uso único já implementado pra confirmação de e-mail/reset de senha
  (`UserTokenService`, ver `2_authentication.md`).
- [ ] **Lembrete automático** (D-1, H-1) — reaproveita a fila+e-mail já pronta (`EmailService`); só
  precisaria de um job agendado (`@Scheduled`) publicando o lembrete perto do horário.
- [ ] **Política de cancelamento/no-show** (prazo mínimo pra cancelar sem taxa, taxa de no-show).
- [ ] **Comissão do profissional configurável por serviço** — % ou valor fixo, aplicado quando o
  financeiro do evento é pago.
- [ ] **Agendamento self-service pelo cliente** (uma tela pública, sem passar pela administração) —
  fora do escopo administrativo atual; grande parte da atração desse tipo de sistema pro dono do
  negócio é o cliente final poder marcar sozinho, 24/7.

## 💰 Financeiro (`AccountReceivable`/`AccountReceivableMovement`)

**[JÁ EXISTE]** — mais maduro do que uma primeira versão costuma ser: parcelamento (`nrInstallment`),
saldo (`vlBalance`), vencimento (`daDue`), status completo (`PENDING`/`PARTIALLY_PAID`/`PAID`/
`OVERDUE`/`CANCELLED`), múltiplas formas de pagamento já modeladas
(`CASH`/`PIX`/`DEBIT_CARD`/`CREDIT_CARD`/`BANK_TRANSFER`/`CHECK`/`BOLETO`/`OTHER`), e estorno
(`AccountReceivableMovement.reversalAccountReceivableMovement`, auto-referência pro movimento
revertido).

- [ ] **Contas a pagar** — hoje só existe o lado "a receber". Despesas (aluguel, fornecedor, comissão
  a pagar pro profissional) não têm onde entrar; é a metade que falta pra um financeiro completo.
- [ ] **Caixa** (abertura/fechamento diário, resumo de entradas por forma de pagamento) — padrão
  "PDV" comum em quem atende presencialmente.
- [ ] **Comissão de profissional calculada automaticamente** — quando o `AccountReceivable` do
  evento é pago, gerar (ou ao menos calcular) o valor de comissão daquele profissional — vira insumo
  direto de "Contas a pagar" acima.
- [ ] **Pacotes pré-pagos** (cliente compra 10 sessões de uma vez, cada agendamento "consome" uma) —
  padrão muito comum em salão/academia/clínica de estética. Vira uma conta a receber paga
  antecipadamente, com "saldo de sessões" em vez de saldo em dinheiro.
- [ ] **Vale-presente (gift card)** e **cupom de desconto**.
- [ ] **Emissão de nota fiscal** (NFS-e pra serviço, NF-e pra produto) — bem relevante no Brasil,
  normalmente via integração com um provedor terceiro (Focus NFe, NFE.io, PlugNotas).
- [ ] **Régua de cobrança automática** (notificar o cliente perto do vencimento, e de novo se
  vencer) — reaproveita a mesma infra de notificação já pronta.
- [ ] **Conciliação bancária** (importar extrato, bater com os movimentos) — mais avançado, só
  relevante se o volume justificar.
- [ ] Consertar `AccountReceivable.dsObservation`, hoje tipado `LocalDateTime`
  (`entity/AccountReceivable.java`) — quase certamente devia ser `String`, comparando com
  `AccountReceivableMovement.dsObservations` (plural, tipado `String` corretamente ali). Não é bem
  "regra de negócio", é um bug que apareceu revisando o código pra este levantamento.

## 🛒 Order/Product (ainda não implementado)

- [ ] **Controle de estoque** (quantidade atual, alerta de estoque mínimo, custo médio).
- [ ] **Movimentação de estoque** (entrada por compra, saída por venda, ajuste/perda) — mesmo
  espírito de `AccountReceivableMovement`: um `Product` tem saldo, e cada `ProductMovement` altera
  esse saldo com um motivo.
- [ ] **Produto como insumo vs. produto vendido** — ex: tinta de cabelo consumida durante o
  atendimento (desconta do estoque, não gera linha de venda pro cliente) vs. produto comprado pelo
  cliente pra levar pra casa (desconta do estoque **e** gera venda). São dois fluxos diferentes que
  compartilham o mesmo cadastro de produto.
- [ ] **Comissão de venda de produto** — separada da comissão de serviço.
- [ ] **Preço de custo vs. preço de venda** (margem).
- [ ] **Fornecedor** (`Supplier`) — módulo natural de existir junto com estoque/compra.
- [ ] **Kits/combos de produtos**.
- [ ] **Unificar `Order` + `EventService` numa "comanda"/venda única** — o padrão de mercado (Trinks,
  Booksy, etc): o cliente chega, o profissional realiza o(s) serviço(s), a recepção adiciona
  produto(s) levados na mesma visita, e tudo fecha numa cobrança só (um `AccountReceivable`, várias
  linhas). Hoje `EventService` gera seu próprio financeiro 1:1; se "comanda" fizer sentido pro seu
  negócio, vale desenhar `Order` já pensando nisso, em vez de `Order` e `EventService` cada um
  gerando financeiro pro seu próprio lado, separadamente.

## 👤 Person/Client/Professional

**[JÁ EXISTE]** — bom desenho de base: `Client` e `Professional` são "papéis" sobre `Person` (FK
simples), o que já permite a mesma pessoa ser cliente **e** profissional sem duplicar cadastro.

- [ ] **Histórico de atendimentos do cliente** — mais relatório que modelo novo, já dá pra montar em
  cima de `Event`/`EventService` como estão.
- [ ] **Preferências/observações do cliente** (profissional preferido, alergias, observação da
  última visita).
- [ ] **Ficha de anamnese/prontuário** — se o nicho for estética/saúde, costuma ser esperado.
- [ ] **Programa de fidelidade** (pontos por valor gasto, cashback).
- [ ] **Campanha de aniversário automática** — reaproveita e-mail/notificação já prontos.
- [ ] **Bloqueio de novo agendamento pra cliente inadimplente** — regra de negócio ligando
  Financeiro ↔ Agenda (checagem parecida com a de `AuthService.login` checar e-mail confirmado antes
  de liberar login: aqui seria "tem conta vencida?" antes de liberar novo agendamento).
- [ ] **Especialidades por profissional** (nem todo profissional faz todo serviço) — não encontrei
  vínculo entre `Professional` e um catálogo de serviços; depende de "Duração padrão por serviço"
  (seção Agenda) existir primeiro.
- [ ] **Múltiplos profissionais no mesmo atendimento** (ex: cabeleireiro + auxiliar) — hoje
  `EventService` só linka um `Professional`.

## 🏢 Company

- [ ] **Múltiplas unidades/filiais por empresa** — `Company` hoje parece ser só "a empresa", sem um
  nível "unidade" abaixo. Uma rede com mais de um endereço físico precisaria disso pra separar
  agenda/estoque/caixa por unidade.
- [ ] **Horário de funcionamento por unidade** (depende do item acima).
- [ ] **Catálogo de serviços da empresa** (nome, duração padrão, preço, categoria) — pré-requisito
  pra várias ideias da seção Agenda (duração automática, especialidade por profissional).

## 💳 Subscription

Diferente dos outros módulos: isso não é sobre o negócio do seu cliente (dono do salão/clínica) — é
sobre como **você** cobra ele por usar o sistema.

- [ ] **Planos com limites** (nº de usuários, nº de agendamentos/mês, nº de unidades).
- [ ] **Trial gratuito** com prazo definido.
- [ ] **Cobrança recorrente via gateway** — Stripe, Pagar.me, Asaas, Iugu (os últimos três com bom
  suporte a PIX/boleto, comuns no mercado brasileiro de SaaS).
- [ ] **Bloqueio de acesso por inadimplência da assinatura** — mais um portão no
  `AuthService.login`, no mesmo espírito do portão de e-mail confirmado que já existe: "a empresa
  está em dia com a assinatura?" antes de liberar login.
- [ ] **Upgrade/downgrade de plano** com cobrança pró-rata.
- [ ] **Add-ons pagos à parte** (ex: WhatsApp, emissão de nota fiscal) — módulos do próprio roadmap
  aqui virando features pagas incrementais.

## 🔔 Notificações & Comunicação

Infra já pronta (RabbitMQ + STOMP + e-mail, ver `1_messaging-and-websocket.md`); falta a camada de
regra de negócio disparando por cima dela:

- [ ] Lembrete de agendamento (D-1, H-1).
- [ ] Confirmação de agendamento pelo cliente sem precisar logar (mesmo padrão de token de
  `confirm-email`/`reset-password`).
- [ ] Campanha de aniversário, régua de cobrança (já citados acima — reunidos aqui porque são todos
  "a mesma infra, gatilhos de negócio diferentes").
- [ ] **Integração com WhatsApp** — o canal mais usado nesse nicho no Brasil; Trinks/Booksy vivem
  disso pra confirmação/lembrete (taxa de abertura muito maior que e-mail). Encaixaria no mesmo
  padrão de fila+listener já usado pro e-mail (`EmailPublisher`/`EmailListener`), só trocando o "quem
  entrega" no fim — arquitetura já pronta pra isso, só falta o canal novo.

## 📊 Relatórios / BI

- [ ] Faturamento por período/profissional/serviço/produto.
- [ ] Taxa de ocupação da agenda.
- [ ] Ticket médio.
- [ ] Taxa de retenção/recorrência de cliente.
- [ ] Serviços/produtos mais vendidos.
- [ ] Comissão a pagar por profissional, por período (consome "Contas a pagar" + "Comissão
  configurável", ambos na seção Financeiro).

## 🔐 Permissões

- [ ] **Permissão por unidade**, não só por empresa — só relevante se "múltiplas unidades" avançar.
- [ ] **Perfil "profissional" só enxerga a própria agenda** — hoje `EVENT_VIEW` parece ser
  tudo-ou-nada por empresa (mesmo padrão de permissão simples que o resto do sistema usa); um
  profissional comum normalmente não deveria ver a agenda de outro profissional da mesma empresa.

---

## Como esses módulos se conectam

Pra visualizar o fluxo ponta a ponta que o sistema já mira, com o que falta grifado:

```
Cliente agenda (Event) ──► Profissional atende (EventService) ──► gera Financeiro (AccountReceivable)
                                      │                                      │
                                      ▼                                      ▼
                          [FALTA] comanda com produtos          [JÁ EXISTE] parcelamento,
                          (Order + EventService juntos)         múltiplas formas de pagamento,
                                      │                          estorno
                                      ▼
                          [FALTA] baixa de estoque (Product)

Empresa paga assinatura (Subscription) ──► [FALTA] acesso ao sistema condicionado a isso
```

O ponto de maior alavancagem, se tivesse que escolher um: **unificar `Order` e `EventService` numa
comanda só antes de `Order` ser implementado do zero** — depois que os dois módulos existirem cada um
gerando financeiro separadamente, juntar os dois fica bem mais caro do que desenhar certo desde o
início.
