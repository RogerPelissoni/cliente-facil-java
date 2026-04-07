package br.com.clientefacil.core;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public abstract class CoreController<TResponse, TId> {

    protected abstract CoreCrudService<TResponse, TId> getService();

    @GetMapping
    public List<TResponse> findAll() {
        return getService().findAll();
    }

    @GetMapping("/paged")
    public Page<TResponse> findAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return getService().findAllPaged(page, size, sort, direction);
    }

    @GetMapping("/{id}")
    public TResponse findById(@PathVariable TId id) {
        return getService().findById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable TId id) {
        getService().delete(id);
    }
}