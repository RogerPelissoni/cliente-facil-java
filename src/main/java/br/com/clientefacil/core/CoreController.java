package br.com.clientefacil.core;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

public abstract class CoreController<TResponse, TId> {

    protected abstract CoreService<?, TResponse, TId> getService();

    @GetMapping
    public Object findAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        if (page != null && size != null) {
            return getService().findAllPaged(page, size, sort, direction);
        }

        return getService().findAll();
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