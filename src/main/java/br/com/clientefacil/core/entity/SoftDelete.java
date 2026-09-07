package br.com.clientefacil.core.entity;

// Condição usada em @SQLRestriction por toda entidade soft-deletável (ver
// AbstractSoftDeletableEntity/AbstractSoftDeletableTenantEntity) — centralizada aqui só pra não ter
// a mesma string literal repetida em cada uma das 8 entidades concretas (@SQLRestriction precisa
// ficar na classe concreta, não na @MappedSuperclass: testado ao vivo e confirmado que essa versão
// do Hibernate, 6.4.4.Final, ignora silenciosamente a condição quando declarada só na superclasse —
// ver docs/guides/5_soft-delete.md). @SQLDelete não usa essa constante: a SQL de cada entidade
// referencia o nome da própria tabela, então não tem parte comum pra extrair.
//
// Chegamos aqui depois de tentar o @SoftDelete nativo do Hibernate (6.4+) pra eliminar as duas
// anotações de uma vez — funciona pro delete/leitura em si, mas tem uma trava incondicional: nenhuma
// associação @ManyToOne/@OneToOne LAZY pode apontar pra uma entidade @SoftDelete (Hibernate lança
// UnsupportedMappingException no boot). No nosso schema isso afetava ~20 associações (Person/User/
// Company são referenciados de quase todo lugar) — forçar todas pra EAGER seria um risco de
// over-fetching desproporcional ao ganho de uma anotação a menos. Revertido. Ver
// docs/guides/5_soft-delete.md pro relato completo.
public final class SoftDelete {

    public static final String NOT_DELETED = "deleted_at IS NULL";

    private SoftDelete() {
    }
}
