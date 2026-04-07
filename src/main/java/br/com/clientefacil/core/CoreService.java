package br.com.clientefacil.core;

import br.com.clientefacil.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public abstract class CoreService<TEntity, TResponse, TMapper extends CoreMapper<TEntity, TResponse>, TId>
        implements CoreCrudService<TResponse, TId> {

    protected abstract JpaRepository<TEntity, TId> getRepository();

    protected abstract TMapper getMapper();

    protected abstract String getEntityName();

    @Override
    public List<TResponse> findAll() {
        return getMapper().toResponseList(getRepository().findAll());
    }

    @Override
    public Page<TResponse> findAllPaged(int page, int size, String sort, String direction) {
        Sort.Direction dir = Sort.Direction.fromOptionalString(direction)
                .orElse(Sort.Direction.ASC);

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));

        // Importante:
        // Utilizamos Page.map() ao invés de toResponseList porque Page contém metadados
        // de paginação (totalElements, totalPages, etc).
        // O método map() preserva esses metadados e transforma apenas o conteúdo.
        //
        // Se usássemos toResponseList, perderíamos essas informações e precisaríamos
        // reconstruir manualmente o Page (PageImpl), o que é desnecessário.
        return getRepository().findAll(pageable)
                .map(getMapper()::toResponse);
    }

    @Override
    public TResponse findById(TId id) {
        return getMapper().toResponse(findEntityById(id));
    }

    @Override
    public void delete(TId id) {
        getRepository().delete(findEntityById(id));
    }

    protected TEntity findEntityById(TId id) {
        return getRepository().findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(getEntityName() + " não encontrado(a)"));
    }
}