package br.com.clientefacil.core;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public abstract class CoreService<TEntity, TResponse, TId> {

    protected abstract JpaRepository<TEntity, TId> getRepository();

    protected abstract TResponse toResponse(TEntity entity);

    public List<TResponse> findAll() {
        return getRepository().findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<TResponse> findAllPaged(int page, int size, String sort, String direction) {
        Sort.Direction dir = Sort.Direction.fromOptionalString(direction)
                .orElse(Sort.Direction.ASC);

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));

        return getRepository().findAll(pageable)
                .map(this::toResponse);
    }

    public TResponse findById(TId id) {
        return toResponse(findEntityById(id));
    }

    public void delete(TId id) {
        getRepository().delete(findEntityById(id));
    }

    protected TEntity findEntityById(TId id) {
        return getRepository().findById(id)
                .orElseThrow(() -> new RuntimeException("Recurso não encontrado"));
    }
}