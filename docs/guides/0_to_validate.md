# NÃO DELETAR ESTE ARQUIVO

- Alterações que precisam ainda de validações para serem implementadas devem ficar aqui

-------------------

- Verifcar questão do SoftDelte, hoje pelo que analisei no código, são criados diversos índices para comportar algumas
  regras, isso deveria ser automático, caso contrário cada tabela que precise de uma regra semelhante, necessita de
  novos índices, ex:
    - -- Índice único PARCIAL em vez de constraint de coluna: permite recriar um Client pra
      -- pessoa+empresa depois que o anterior foi soft-deletado (ver deleted_at acima).
      CREATE UNIQUE INDEX uk_client_person_company ON client (person_id, company_id) WHERE deleted_at IS NULL;