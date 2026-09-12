# Comentários no código: só para regras complexas

Não comente código que já se explica sozinho (nome de método/classe claro, lógica trivial, getter/setter, mapeamento direto de entidade). Um comentário só se justifica quando explica algo que o código em si não deixa óbvio — e que, sem ele, alguém poderia "corrigir" de volta pro jeito errado. Exemplos que justificam comentário:

- Uma regra de negócio não óbvia (ex: por que um usuário sem determinada authority nunca vê dados de outro usuário, mesmo que tente forçar um filtro via request — a checagem de escopo tem que ficar documentada onde é decidida).
- Um workaround pra um bug/limitação de infraestrutura (ex: por que uma query usa `coalesce` em vez de `IS NULL OR` pra contornar uma limitação do driver do Postgres em inferir tipo de parâmetro).
- Uma decisão de design que parece inconsistente à primeira vista (ex: por que um método de repositório não pagina como os outros, ou por que um `@Query` usa `left join` em vez de `join`/fetch join numa associação que "deveria" ser direta).

Não comente: o que um método faz (o nome já diz), reformulação em português do que a linha de código já mostra, ou observações genéricas de "boa prática". Ao gerar código novo, escreva primeiro sem comentário nenhum e só adicione um se, relendo, a regra continuar não óbvia. Evite duplicar a mesma explicação em dois lugares (ex: no DTO e no service) — documente uma vez, no lugar onde a regra é de fato decidida/aplicada.

Mesmo quando justificado, o comentário deve ser curto: o fato + a consequência, em 1-3 linhas. Não narre o mecanismo interno por trás (ex: como o protocolo do driver do Postgres infere tipo de parâmetro) — isso é algo pra pesquisar se alguém ficar curioso, não algo que o código precisa preservar pra sempre. Se a explicação está passando de 3-4 linhas, é sinal de cortar pro essencial.
