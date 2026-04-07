package br.com.clientefacil.core;

import org.springframework.data.domain.Page;

import java.util.List;

public interface CoreCrudService<TResponse, TId> {

    List<TResponse> findAll();

    Page<TResponse> findAllPaged(int page, int size, String sort, String direction);

    TResponse findById(TId id);

    void delete(TId id);
}