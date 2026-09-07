# 🗑️ Soft delete

Fase A do item "Soft delete + tabela de auditoria genérica" do roadmap
(`docs/product/3_roadmap.md`) — trocar DELETE físico por um flag de exclusão em algumas entidades de
negócio, feito agora porque o projeto ainda está pré-produção (o momento mais barato pra isso é antes
de existir dado real em jogo). A Fase B (`audit_log` genérica, capturando snapshot de
INSERT/UPDATE/DELETE) fica para uma rodada seguinte — não está implementada ainda.

## Por que

- Recuperar um registro apagado por engano hoje exige restaurar backup do banco inteiro.
- O sistema já guarda dado financeiro de verdade (`AccountReceivable`) — não é algo que devesse
  poder sumir sem deixar rastro.
- É multi-tenant B2B: cada empresa eventualmente quer/precisa de histórico do que já existiu.

## Entidades no escopo

`Client`, `Company`, `Person`, `Professional`, `Event`, `AccountReceivable`,
`AccountReceivableMovement`, `User`. Fora do escopo, propositalmente: `Notification`,
`NotificationDeadLetter`, `UserToken` (já têm limpeza automática via `DataRetentionService` — dado de
volume alto/efêmero, não histórico de negócio), `MailConfig`/`Profile`/`Resource`/`Module`/
`ProfilePermission` (configuração, não histórico), `EventService`/`EventOwner`/`EventMessage`/
`Person*` (address/phone/mail) — detalhe de outra entidade, não um registro independente.

## O mecanismo: `@SQLRestriction` na entidade + `ForeignKeyDeletionGuard` fazendo o UPDATE

Uma anotação do Hibernate por entidade (`@SQLRestriction`) + o `ForeignKeyDeletionGuard`
(`core/hibernate/`, já usado pra bloqueio RESTRICT e cascata CASCADE — ver seção mais abaixo) também
cuidando da própria exclusão:

- **`@SQLRestriction("deleted_at IS NULL")`** — aplicado automaticamente em cima de toda consulta que
  o Hibernate gera pra aquele tipo (JPQL, Criteria, `findById`, `existsBy...`), sem precisar de
  nenhum toggle.
- **`ForeignKeyDeletionGuard.onPreDelete`** — quando a tabela da entidade sendo excluída tem
  `deleted_at`, faz `UPDATE <tabela> SET deleted_at = now() WHERE id = ?` ele mesmo (via
  `JdbcTemplate`) e **veta** o `DELETE` físico que o Hibernate ia gerar (`onPreDelete` retornando
  `true`). **Zero mudança nos services**: quem já chamava `repository.delete(...)` continua chamando
  exatamente igual — só o que acontece por baixo é diferente.

```java
@Entity
@Table(name = "client")
@SQLRestriction(SoftDelete.NOT_DELETED)
public class Client extends AbstractSoftDeletableTenantEntity { ... }
```

Isso substitui o que antes era `@SQLDelete(sql = "UPDATE client SET deleted_at = now() WHERE id = ?")`
em cada entidade — ver "Terceira tentativa, essa deu certo" mais abaixo pro porquê e como isso escapa
da mesma trava que derrubou a segunda tentativa (`@SoftDelete` nativo).

`SoftDelete.NOT_DELETED` (`core/entity/SoftDelete.java`) é só a constante `"deleted_at IS NULL"`
centralizada — a condição é idêntica nas 8 entidades, fonte única em vez de literal repetido 8 vezes.
`SoftDeletableEntitiesTest` (`src/test/java/br/com/clientefacil/entity/`) garante via reflection que
toda entidade soft-deletável declara `@SQLRestriction` corretamente — e que ninguém reintroduz
`@SQLDelete` por hábito (ficaria redundante, o guard sempre veta antes dele rodar).

`core/entity/AbstractSoftDeletableEntity.java` (pra `Company`, que não é tenant-aware) e
`core/entity/AbstractSoftDeletableTenantEntity.java` (pra as outras 7) só acrescentam a coluna
`deletedAt`/`deleted_at` — `@SQLRestriction` fica em cada entidade concreta, não nessas superclasses
(ver "Pegadinha" abaixo pro porquê).

### Por que não reaproveitar o `@Filter` do `tenantFilter`?

O isolamento de tenant (`AbstractAuditableTenantEntity`) já usa `@FilterDef`/`@Filter`
(`condition = "(company_id = :companyId OR company_id IS NULL)"`), ligado/desligado por sessão via
`TenantFilterAspect`. Cogitei reaproveitar o mesmo padrão pro soft delete, mas dois motivos levaram a
`@SQLRestriction` em vez disso:

1. `@Filter` precisa ser explicitamente ligado por sessão. `@SQLRestriction` é estático — sempre
   ativo, sem depender de nenhum aspecto/interceptor rodando antes.
2. `TenantScopedRepositoryImpl.findById()` já contorna o `@Filter` do tenant com uma `CriteriaQuery`
   manual (não passa pelo mecanismo de filtro de sessão) — um soft-delete baseado em `@Filter` teria
   o mesmo buraco: `findById` de um registro soft-deletado voltaria a funcionar normalmente. Com
   `@SQLRestriction`, isso não acontece — testado ao vivo (ver "Como testar" abaixo), `GET /client/1`
   devolve 404 pra um Client soft-deletado mesmo passando pela `CriteriaQuery` customizada.

### Pegadinha real, encontrada testando ao vivo: `@SQLRestriction` não é herdado de `@MappedSuperclass`

Primeira tentativa: colocar `@SQLRestriction("deleted_at IS NULL")` uma vez só, na
`@MappedSuperclass` (`AbstractSoftDeletableTenantEntity`), pra não repetir em cada uma das 8
entidades — mesmo espírito do `@Filter` do tenant, que fica numa `@MappedSuperclass` sem problema.
**Não funcionou**: nesta versão do Hibernate (6.4.4.Final, a que o Spring Boot 3.2.5 traz), a
condição declarada só na `@MappedSuperclass` é silenciosamente ignorada pelas subclasses — sem erro
de compilação nem de startup, a query simplesmente sai sem o `WHERE deleted_at IS NULL`. Só foi
percebido no smoke test manual (ver abaixo): um Client soft-deletado continuava aparecendo na
listagem.

Correção: `@SQLRestriction` (junto com `@SQLDelete`, que já precisava ser por entidade mesmo, porque
a SQL literal referencia o nome da tabela) repetido em cada uma das 8 classes concretas. Resolvido —
confirmado via log de SQL do Hibernate (`org.hibernate.SQL` em DEBUG) que o `WHERE` passou a incluir
a condição depois da mudança.

### Segunda tentativa de automatizar, também testada e revertida: `@SoftDelete` nativo do Hibernate

Ter que declarar `@SQLDelete`+`@SQLRestriction` em cada uma das 8 entidades incomodou o suficiente
pra valer uma segunda tentativa de eliminar a repetição — desta vez usando
`org.hibernate.annotations.SoftDelete`, um recurso **nativo** do Hibernate desde a 6.4 (`@Incubating`)
feito exatamente pra isso: uma anotação só, cuida do delete→update e do filtro de leitura juntos.

O `@Target` dela inclui `PACKAGE` e `ANNOTATION_TYPE` — a documentação garante que dá pra empacotar
numa anotação própria (`@SoftDeletable`) e usar essa só, ou aplicar num `package-info.java` pra valer
pro pacote inteiro. **Não funcionou nesta versão (6.4.4.Final)**: testado ao vivo, uma entidade com
`@SoftDeletable` (a meta-anotação) continuava gerando `DELETE FROM ...` físico — o Hibernate
simplesmente não reconhecia a anotação por trás da meta-anotação, apesar do `@Target` permitir.
Isolado o problema aplicando `@SoftDelete` **direto** na entidade (sem meta-anotação): aí sim
funcionou (`deleted = true`, linha preservada) — confirma que o recurso em si funciona, só a
composição via anotação própria que não é suportada de verdade nesta versão.

Com isso, a única forma de usar `@SoftDelete` nativo seria repeti-lo direto em cada uma das 8
entidades — não resolvia o problema original. E mesmo assim tem uma trava **incondicional**,
encontrada só ao tentar (confirmado lendo o source de `ToOneAttributeMapping.java`):

```java
if (entityMappingType.getSoftDeleteMapping() != null) {
    if (getTiming() == FetchTiming.DELAYED) {   // = LAZY
        throw new UnsupportedMappingException(
            "To-one attribute (%s.%s) cannot be mapped as LAZY as its associated entity is defined with @SoftDelete");
    }
}
```

Nenhuma associação `@ManyToOne`/`@OneToOne` LAZY pode apontar pra uma entidade `@SoftDelete` — sem
flag pra desligar. No nosso schema isso afeta ~20 associações (`Person`/`User`/`Company` são
referenciados de quase todo lugar) — forçar todas pra `EAGER` trocaria "N+1 ocasional, resolvível com
`@EntityGraph` quando aparecer" por over-fetching sistemático em praticamente todo `SELECT` do
sistema. Descartado por desproporcional.

Cogitado também um trigger `BEFORE DELETE` no Postgres (substituiria só o `@SQLDelete`, sem essa
trava — triggers não são afetados pela restrição de `LAZY`, que é uma coisa do metamodelo do
Hibernate, não do banco). Descartado por decisão consciente: a regra de negócio ficaria partida entre
banco (trigger) e código (`@SQLRestriction`) — duas fontes de verdade pra uma coisa só, pior pra
raciocinar sobre o sistema do que ter as duas anotações juntas no mesmo lugar.

**A trava de `LAZY` não é bug da 6.4.4 — é decisão permanente do time do Hibernate**, confirmado
consultando a [thread oficial no fórum](https://discourse.hibernate.org/t/soft-delete-issue/9839)
(resposta de um mantenedor: *"we cannot rely on the foreign key constraint to ensure the existence
(or non-existence) of an associated entity"* — *"this is the main and only downside of this feature,
but it cannot work correctly without it"*). Checado também se a Hibernate 7.4 (latest stable em
[hibernate.org/orm/releases](https://hibernate.org/orm/releases/)) mudaria isso: adicionou uma
estratégia de `@SoftDelete` baseada em TIMESTAMP (resolveria o trade-off boolean-vs-timestamp que
discutimos), mas a trava de `LAZY` continua — é a mesma decisão de design, não uma limitação de
versão. E adotar Hibernate 7.x exigiria Spring Boot 4.0+ (upgrade de stack inteiro, Spring Framework
7), desproporcional pra resolver uma questão de anotação repetida.

**Conclusão desta rodada, depois de esgotar as alternativas *baseadas em anotação* dentro da API
pública do Hibernate**: nenhuma delas eliminava `@SQLDelete` sem custo desproporcional (meta-anotação
não funciona nesta versão, `@SoftDelete` nativo tem uma trava séria de performance permanente,
trigger duplica a fonte de verdade). A solução que funcionou (seção "Terceira tentativa" abaixo) não
é baseada em anotação nova nenhuma — reaproveita o `ForeignKeyDeletionGuard` que já existia por outro
motivo (bloqueio RESTRICT/cascata CASCADE) pra também fazer o `UPDATE` da própria entidade. Só
`@SQLRestriction` continua declarado por entidade — a leitura não tem um gancho de evento equivalente
ao de delete pra centralizar isso do mesmo jeito.

### Enquanto isso, uma rede de segurança: teste que torna impossível a declaração ficar errada

Antes da terceira tentativa (abaixo) resolver o `@SQLDelete`, criamos `SoftDeletableEntitiesTest`
(`src/test/java/br/com/clientefacil/entity/`) como paliativo pro risco que sobrava: alguém
copiar-colar errado numa entidade nova (nome de tabela errado no SQL, esquecer uma das duas
anotações) — exatamente o tipo de erro que não dá nem compilação nem erro de startup, só
comportamento errado descoberto depois (mesma categoria da pegadinha do `@MappedSuperclass` acima).
Varre `br.com.clientefacil.entity` via `ClassPathScanningCandidateComponentProvider` procurando
qualquer classe que estenda `AbstractSoftDeletableEntity`/`AbstractSoftDeletableTenantEntity` — não é
uma lista fixa das 8 entidades, então uma entidade nova que estenda a superclasse certa já entra na
checagem sozinha. Testado de propósito quebrando uma entidade pra confirmar que o teste realmente
pega — pegou, com mensagem apontando exatamente a entidade e a divergência. Mesmo espírito do já
existente `AuthorizationSeederOrderTest`: não prova que soft delete funciona (isso continua sendo
validado ao vivo contra Postgres), só garante que ninguém quebra a regra sem o build avisar.

Com a terceira tentativa abaixo, o teste ficou mais simples (só sobrou `@SQLRestriction` pra checar,
mais a ausência de `@SQLDelete`) — mas continua valendo a pena existir.

### Rede de segurança nível 2: `SoftDeleteBehaviorIntegrationTest`, o comportamento em si

`SoftDeletableEntitiesTest` prova que a **declaração** está certa (a anotação diz a coisa certa).
Depois de mais uma rodada tentando automatizar `@SQLRestriction` (Interceptor/`StatementInspector` —
ver "Quarta tentativa" abaixo) sem sucesso, criamos um segundo teste que prova o
**comportamento** em si, contra o Postgres de verdade: pra cada uma das 8 entidades, cria um
registro, chama `repository.delete(...)`, e confirma duas coisas — `findById` não encontra mais (via
Hibernate/`@SQLRestriction`) e a linha continua existindo fisicamente com `deleted_at` preenchido
(consultado via `JdbcTemplate` puro, sem depender do mesmo mecanismo sendo testado). Roda com
`@Transactional` (cada teste desfaz sozinho ao final — banco sempre limpo, sem precisar do reset
manual que o smoke test desta sessão exigiu duas vezes).

Vale a diferença: esse teste **não sabe nem se importa qual mecanismo está implementando** soft
delete — continua validando corretamente não importa se amanhã for `@SQLRestriction`+guard (hoje),
`@SoftDelete` nativo, ou qualquer outra coisa. É uma garantia mais forte que a do
`SoftDeletableEntitiesTest`, que quebraria (corretamente) se alguém trocasse de mecanismo e deixasse o
teste em si desatualizado.

Pegadinha real, encontrada escrevendo este teste: `findById()` chamado logo depois de
`repository.delete(...)`, **na mesma transação**, volta vazio mesmo sem nenhum SQL rodar — é
bookkeeping em memória do JPA (uma entidade removida no mesmo persistence context já conta como
ausente pro `EntityManager.find()`, por especificação, sem round-trip no banco). Sem forçar
`entityManager.flush()` (dispara o `PreDeleteEvent`/guard de verdade) e `entityManager.clear()`
(esvazia o cache de 1º nível, obrigando a consulta seguinte a ir no banco), o teste passaria mesmo se
o `ForeignKeyDeletionGuard` não tivesse feito nada — falso positivo silencioso, do mesmo jeito que os
outros bugs desta rodada.

### Terceira tentativa, essa deu certo: `ForeignKeyDeletionGuard` também faz o UPDATE, sem `@SQLDelete`

Depois de esgotar as alternativas baseadas em anotação (meta-anotação, `@SoftDelete` nativo — ambas
acima), voltamos a uma ideia mais simples: o `ForeignKeyDeletionGuard` (`core/hibernate/`, ver seção
mais abaixo) já faz exatamente essa troca — `UPDATE` em vez de `DELETE` — pros filhos em cascata, via
`JdbcTemplate` puro. Por que não fazer o mesmo pra entidade sendo excluída em si?

Faltava confirmar um detalhe antes de tentar de novo: `PreDeleteEventListener.onPreDelete` retornando
`true` **veta** o delete — mas o quê, exatamente, isso pula? Fomos na fonte
(`EntityDeleteAction.execute()`):

```java
final boolean veto = isInstanceLoaded() && preDelete();
...
if ( !isCascadeDeleteEnabled && !veto ) {
    persister.delete( id, version, instance, session );   // só isto é pulado com veto=true
}
if ( isInstanceLoaded() ) {
    postDeleteLoaded( id, persister, session, instance, ck );   // roda igual, vetado ou não
}
```

`veto` só pula a chamada que gera o `DELETE` físico — `postDeleteLoaded` (remove da persistence
context, evicta cache, dispara `@PostDelete`) roda do mesmo jeito, vetado ou não. Ou seja: vetar tem
exatamente o mesmo efeito colateral que `@SQLDelete` já tinha (a entidade sai da sessão do Hibernate
normalmente) — nenhum risco novo.

E o motivo pelo qual isso escapa da trava de `LAZY` que derrubou a segunda tentativa: a trava vive em
`ToOneAttributeMapping`, checando `entityMappingType.getSoftDeleteMapping() != null` — só é
preenchido quando o Hibernate processa a anotação `@SoftDelete` no boot. Como esta abordagem não usa
essa anotação (é só o guard fazendo `UPDATE` via SQL puro), esse campo nunca é preenchido pras nossas
8 entidades — a trava nunca dispara.

Implementado: `onPreDelete` extrai tabela/id do evento e delega pra `handleDelete(table, id)` — o
mesmo método que já validava RESTRICT e cascateava CASCADE agora também, ao final, faz
`UPDATE <tabela> SET deleted_at = now() WHERE id = ?` (se a tabela tiver essa coluna) e retorna
`true`. `@SQLDelete` saiu das 8 entidades — só `@SQLRestriction` continua necessário (a leitura não
tem gancho de evento equivalente ao de delete). Testado ao vivo depois da mudança: soft delete
continua preenchendo `deleted_at` sem apagar a linha, bloqueio RESTRICT e cascata CASCADE (física e
soft) continuam funcionando, boot limpo sem nenhum erro de mapeamento LAZY.

Consultamos até a versão mais recente do Hibernate (7.4, "latest stable" em
[hibernate.org/orm/releases](https://hibernate.org/orm/releases/)) antes de chegar nessa solução — a
trava de `LAZY` do `@SoftDelete` nativo é [decisão permanente do time do
Hibernate](https://discourse.hibernate.org/t/soft-delete-issue/9839), não corrigida em nenhuma
versão, então essa era a única rota realista pra eliminar o `@SQLDelete` sem forçar ~20 associações
pra `EAGER`.

### Quarta tentativa, essa não foi implementada: `Interceptor`/`StatementInspector` pro `@SQLRestriction`

Com `@SQLDelete` eliminado, sobrou só `@SQLRestriction` por entidade. A mesma técnica que resolveu o
`@SQLDelete` (trocar anotação por um gancho de evento do Hibernate) tem um equivalente pro lado de
leitura — `Interceptor.onPrepareStatement(String sql)` / `StatementInspector` — mas com uma diferença
importante: esse gancho entrega o **SQL final já em texto**, não uma árvore de query estruturada.
Injetar `AND deleted_at IS NULL` ali exigiria manipular a string SQL (regex ou parsing), não algo
processado com garantia pelo motor de query do Hibernate como `@SQLRestriction`/`@SQLDelete` são.

Pesquisado antes de tentar implementar (o modo de falha aqui é pior que os anteriores: um bug não dá
erro nenhum, só devolve dado excluído de volta, silenciosamente — o oposto do que soft delete existe
pra evitar):

- O uso documentado de `StatementInspector` na comunidade nunca é pra isso — o exemplo de referência
  ([Vlad Mihalcea](https://vladmihalcea.com/hibernate-statementinspector/)) usa pra logging e remover
  comentário de SQL antes de executar. Nenhum autor conhecido demonstra injeção de `WHERE`, nem
  discute os riscos disso.
- Fomos atrás do problema estruturalmente idêntico — filtro automático de multi-tenancy, que também
  precisa injetar uma condição em toda query. A resposta padrão da comunidade pra isso é `@Filter`/
  `@FilterDef` — a mesma técnica que já tínhamos testado e rejeitado pro soft delete (`findById`
  contorna, ver acima). Nenhum precedente de reescrita de SQL texto nem pro caso mais comum.
- Artigo dedicado a pegadinhas de soft delete no Hibernate
  ([jpa-buddy](https://jpa-buddy.com/blog/soft-deletion-in-hibernate-things-you-may-miss/)) não cita
  nenhuma técnica de centralização — conclui que "Hibernate doesn't provide" uma solução ideal.

**Descartado**: sem precedente de uso seguro, e o modo de falha (vazamento silencioso de dado
excluído) é estruturalmente pior que "esquecer uma anotação" (que já vira erro de build, ver
`SoftDeletableEntitiesTest` acima). `@SQLRestriction(SoftDelete.NOT_DELETED)` continua declarado por
entidade — em troca, reforçamos a rede de segurança com um teste de comportamento real
(`SoftDeleteBehaviorIntegrationTest`, acima) em vez de arriscar a automação.

### Rede de segurança nível 3: `SoftDeletableUniqueIndexesTest`, os índices únicos parciais

As duas redes de segurança acima cobrem a declaração (`@SQLRestriction`) e o comportamento do delete
em si — nenhuma cobre a terceira forma real de errar soft delete: uma constraint `UNIQUE` "cheia" numa
tabela que tem `deleted_at` (ver seção "Migrations" abaixo pro porquê isso é um problema). Diferente do
`ForeignKeyDeletionGuard` (RESTRICT/CASCADE resolvidos genericamente em runtime, lendo o schema), aqui
**não existe automação equivalente**: quais colunas formam a chave de unicidade de cada tabela é
decisão de negócio, não algo derivável do schema — não dá pra "gerar" isso sem uma camada nova
reescrevendo DDL, o que seria mágica de verdade (comportamento implícito, difícil de depurar) em vez do
`ForeignKeyDeletionGuard`, que só aplica uma regra já 100% descrita no schema (RESTRICT/CASCADE).

Por isso a solução aqui é a mesma dos outros dois níveis: manter a SQL explícita por migration (como já
era) e adicionar um teste (`SoftDeletableUniqueIndexesTest`,
`src/test/java/br/com/clientefacil/entity/`) que lê o catálogo do Postgres (`pg_index`) e falha o build
se alguém esquecer a convenção — não gera nem substitui nada, só verifica. Dois testes:

1. **Nenhum índice único não-parcial numa tabela soft-deletável** — `SELECT ... FROM pg_index WHERE
   indisunique AND NOT indisprimary AND indpred IS NULL AND <tabela tem deleted_at>` deve vir vazio. A
   PK entra como índice único também, por isso `indisprimary = false` na condição.
2. **Todo índice único parcial que existir numa tabela soft-deletável tem o predicado exato**
   `(deleted_at IS NULL)` — pega o caso de alguém escrever a condição errada (esquecer, inverter pra
   `IS NOT NULL`, etc.).

Testado ao vivo de propósito (mesmo espírito das pegadinhas documentadas acima): criada uma constraint
`UNIQUE` "cheia" fake numa tabela soft-deletável, confirmado que o teste falha apontando exatamente a
tabela/índice, removida a constraint, confirmado que volta a passar.

## Migrations

Projeto pré-produção → a coluna `deleted_at` entrou direto nas migrations de `CREATE TABLE`
originais (`V1_2`, `V2`, `V3_2`, `V9`, `V10`, `V11_2`, `V11_5`, `V12_3`), não como `ALTER TABLE` novo
— mantém o histórico limpo, como se soft delete sempre tivesse existido. Índices únicos que existiam
como constraint de coluna/tabela (email do `User`, `(person_id, company_id)` de `Client`/
`Professional`, `(ds_code, nr_installment, company_id)` de `AccountReceivable`) viraram **índices
únicos parciais** (`WHERE deleted_at IS NULL`) — sem isso, um registro soft-deletado bloquearia pra
sempre recriar outro com a mesma chave.

## Onde a proteção que o banco fazia sozinho virou o `ForeignKeyDeletionGuard`

> Esta seção descreve a segunda rodada da funcionalidade — a primeira versão usava 4 validators
> manuais (`PersonDeletionValidator` e afins), um por entidade, cada um hard-codando quais relações
> checar. Foram substituídos pelo mecanismo abaixo depois de uma observação direta: código manual não
> escala — cada FK nova exigiria lembrar de escrever/atualizar um validator, e nada garante que
> alguém lembre. O schema do banco já tem essa informação (`ON DELETE RESTRICT`/`CASCADE` de cada
> FK) — virou a fonte única de verdade.

Quase todo FK entre essas tabelas é `ON DELETE RESTRICT` ou `ON DELETE CASCADE` — antes do soft
delete, isso protegia/limpava sozinho: um `DELETE` físico falhava se algo ainda apontasse pro
registro (`RESTRICT`), ou arrastava os filhos junto (`CASCADE`). Com o soft delete (hoje: o próprio
`ForeignKeyDeletionGuard` vetando o `DELETE` e fazendo `UPDATE` no lugar, ver seção do mecanismo
acima), o `DELETE` nunca mais acontece de verdade — **nem o RESTRICT nem o CASCADE disparam mais**.

`core/hibernate/ForeignKeyDeletionGuard.java` (registrado como `PreDeleteEventListener` do Hibernate
em `core/config/HibernateListenerConfig.java` — sem precedente de listener custom no projeto antes
disso) reimplementa os dois papéis genericamente, lendo o `information_schema` do Postgres **uma vez
só** (schema não muda em runtime) pra montar dois mapas — tabela referenciada → lista de FKs
`RESTRICT`/`NO ACTION`, e tabela referenciada → lista de FKs `CASCADE` — mais o conjunto de tabelas
que têm `deleted_at`. Roda antes de **qualquer** exclusão do sistema, não só as 8 entidades
soft-deletáveis:

1. **RESTRICT/NO ACTION**: `SELECT EXISTS(...)` pra cada FK incidente; se algum retornar linha ativa,
   lança `BusinessException` (409) — mesmo papel que o banco fazia sozinho.
2. **CASCADE**: enumera os filhos e processa cada um recursivamente **antes** de decidir o que fazer
   com ele — se a tabela filha tem `deleted_at`, vira soft delete (`UPDATE ... SET deleted_at =
   now()`); se não tem, remoção física (`DELETE FROM ...`), exatamente o que o `CASCADE` do banco
   faria. Isso cobre cadeias de dois ou mais níveis automaticamente (ex.: `AccountReceivable` →
   `AccountReceivableMovement` → suas próprias reversões) sem precisar enumerar caso a caso.
3. **A própria entidade sendo excluída**: se a tabela dela tem `deleted_at`, o mesmo `UPDATE` acima
   e veta o `DELETE` físico do Hibernate — é o que substituiu `@SQLDelete` (ver "Terceira tentativa,
   essa deu certo" na seção do mecanismo).

Como a linha do "pai" nunca é fisicamente removida em nenhum nível dessa árvore (soft delete vira
`UPDATE`), não existe risco de violar FK por ordem de execução — qualquer `id` referenciado continua
existindo fisicamente o tempo todo. Se qualquer nó da árvore estiver bloqueado por `RESTRICT`, a
exceção aborta a transação inteira (nada fica parcialmente aplicado).

Efeito prático de trocar "código hard-coded" por "ler o schema": **`created_by`/`updated_by` (RESTRICT
em ~20 tabelas, apontando pra `users`) e `company_id` (RESTRICT em ~15 tabelas, apontando pra
`company`) passaram a bloquear de verdade** — na Fase A anterior, esses dois casos tinham sido
deixados de fora de propósito (ver histórico no fim desta seção). Isso significa que, na prática,
**excluir um `User` ou uma `Company` fica bloqueado quase sempre** — a partir do primeiro registro
criado por aquele usuário, ou da primeira linha de dado do tenant. Decisão consciente do usuário do
projeto: num sistema com dado financeiro, não se deve conseguir apagar um usuário/tenant que já tem
histórico — a via normal de "remover" um deles do dia a dia é **desativar**, não excluir.

### `fl_active` em `users` e `company`

Mesmo padrão que `Person.flActive` já usava antes desta rodada (flag de negócio, ortogonal à exclusão
— não é `@SQLRestriction`, não desaparece de nenhuma listagem sozinho). Adicionado a `users` e
`company` especificamente porque são as duas tabelas onde o `ForeignKeyDeletionGuard` bloqueia quase
universalmente (ver acima) — sem isso, não haveria via nenhuma pra "desligar" um usuário ou arquivar
um tenant. Alterna via o mesmo `PUT` que já existia (`PUT /api/v1/users/{id}`,
`PUT /api/v1/company/{id}`), nenhum endpoint novo. `Client`/`Professional` **não** ganharam o mesmo
flag: a única FK que bloqueia a exclusão deles (`event_service.client_id`/`professional_id`) é
ocasional, não quase-universal — soft delete direto continua sendo um caminho útil assim que os
agendamentos ativos terminarem.

### Histórico: por que a Fase A tinha deixado `created_by`/`updated_by`/`Company` de fora

A primeira versão (validators manuais) tinha excluído esses dois casos deliberadamente, pelo receio
de tornar impossível desativar a conta de alguém que já saiu da empresa, ou de tornar "apagar uma
empresa" impraticável. O `fl_active` acima resolve exatamente essa preocupação por outro caminho — em
vez de o validador escolher não bloquear, o guard bloqueia igual (é a verdade estrutural do schema) e
existe uma via de "desativar" que não esbarra na proteção. Resultado mais simples de auditar: uma
única regra genérica, sem exceção hard-coded pra decorar.

### `AccountReceivableValidator` continua existindo, sem mudança

Não é substituível pelo `ForeignKeyDeletionGuard`: a regra que ele aplica (não editar/remover título
com movimentação vinculada) não corresponde a nenhum FK `RESTRICT` real —
`account_receivable_movement.account_receivable_id` é `CASCADE` no banco, não `RESTRICT`. É uma regra
de negócio mais rígida que a integridade referencial, não um substituto pra ela — continua precisando
ser código explícito.

## Desempenho

- **Metadata (`information_schema`) é lida uma vez só**, cacheada em memória — nenhuma query de
  metadata roda por exclusão, só no primeiro delete depois de cada boot do backend.
- **O guard só participa de `DELETE`** — zero custo em `GET`/`POST`/`PUT`, que são o grosso do
  tráfego real do sistema.
- Cada nível da árvore de exclusão/cascata é 1 `EXISTS`/`SELECT id` por FK incidente na tabela —
  número pequeno e fixo (2 a 5 por entidade na prática), não cresce com o volume de dados em si.
- **Decisão consciente: sem índice dedicado nas colunas de FK que essas queries consultam, por
  enquanto.** Boa parte delas hoje vira `Seq Scan` (algumas caem numa constraint única composta onde
  a coluna não é a primeira, ex. `uk_event_owner_event_users (event_id, user_id)` não serve pra
  `WHERE user_id = ?` sozinho; outras não têm índice nenhum). Avaliado e descartado por ora: volume de
  exclusões é baixíssimo na prática, o impacto real de um `Seq Scan` ocasional numa tabela pequena é
  pequeno o bastante pra não justificar mexer em várias migrations por antecipação. **Isto é uma ação
  a fazer, não uma lacuna esquecida** — revisitar se/quando o volume de exclusões ou o tamanho de
  alguma dessas tabelas tornar isso um gargalo real (sintoma: `DELETE`/`EXPLAIN ANALYZE` lento nas
  tabelas que o guard consulta com mais frequência — `client`/`professional`/`account_receivable`/
  `users` por `person_id`; `event_service` por `client_id`/`professional_id`/`event_id`/
  `account_receivable_id`; `event_owner` por `user_id`).
- Tudo roda na mesma transação/conexão do `repository.delete(...)`, via `JdbcTemplate` participando da
  transação JPA ativa (comportamento padrão do Spring ao compartilhar `DataSource`) — nenhuma
  round-trip extra de commit, sem risco de estado parcial se algo falhar no meio.
- Nota à parte, fora do escopo desta funcionalidade: `company_id` (usado pelo `tenantFilter` em toda
  leitura tenant-scoped, não só pelo guard) também não tem índice dedicado na maioria das tabelas —
  gap preexistente, maior que este, não resolvido aqui.

## `GlobalExceptionHandler`: 404/409 de verdade

`BusinessException`/`ResourceNotFoundException` caíam no catch-all de `RuntimeException` (500) antes
da Fase A — a mensagem amigável já saía certa (o front lê `data.message` independente do status), só
o código HTTP estava errado. Agora: `BusinessException` → 409 (Conflict), `ResourceNotFoundException`
→ 404.

**Detalhe descoberto ao vivo com o `ForeignKeyDeletionGuard`**: como ele lança a exceção de dentro de
um `PreDeleteEventListener`, que só roda no *flush* do Hibernate (geralmente no commit da transação,
não na hora de `repository.delete(...)` em si), o Spring já embrulha a exceção original numa
`TransactionSystemException` ("Could not commit JPA transaction") antes dela chegar no
`@ExceptionHandler`. Sem tratar isso, ela caía no catch-all de 500 de novo — mesmo bug, causa
diferente. Adicionado `handleTransactionSystem`, que usa `getMostSpecificCause()` (desembrulha a
cadeia toda, não só um nível) pra achar a `BusinessException`/`ResourceNotFoundException` original e
delegar pro mesmo 409/404 de sempre.

## Como testar

1. `docker compose restart backend` — Flyway aplica as migrations editadas. **Se o banco de dev já
   tinha rodado essas migrations antes da edição**, precisa resetar primeiro (checksum não bate):
   `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` no Postgres, depois restart — deixa todas as
   migrations rodarem do zero.
2. Criar um `Client`, deletar (`DELETE /api/v1/client/{id}`) — confirmar que some de
   `GET /api/v1/client` e que `GET /api/v1/client/{id}` devolve 404.
3. `SELECT id, deleted_at FROM client WHERE id = ...` — confirmar que a linha continua existindo,
   com `deleted_at` preenchido (não foi apagada de verdade).
4. Criar de novo um `Client` com o mesmo `person_id`+`company_id` do que acabou de ser
   soft-deletado — confirma que o índice parcial substituiu a constraint global corretamente.
5. Tentar deletar um `Person` que ainda tem `Client` ativo vinculado — confirmar `409` com a
   mensagem do `ForeignKeyDeletionGuard` (bloqueio via RESTRICT). Deletar o `Client` primeiro e
   repetir — agora `204`.
6. Criar um `Event` com `eventService` (que cria `event_service`/`account_receivable` junto) e
   deletar o `Event` — confirmar que `event_service`/`event_owner`/`event_message` somem de verdade
   da tabela (`SELECT count(*)`, não só da listagem — são fisicamente removidos, CASCADE sem
   `deleted_at`) e que o `account_receivable` criado junto continua intacto (sem FK apontando pra
   `event`, não é afetado).
7. Tentar deletar um `User`/uma `Company` com algum dado próprio (`created_by`/`company_id`) —
   confirmar `409`. Em seguida, `PUT` no mesmo recurso com `flActive: false` — confirmar `200`, sem
   apagar nada.
