package br.com.clientefacil.core;

import java.util.List;

public interface CoreMapper<TEntity, TResponse> {
    TResponse toResponse(TEntity entity);

    List<TResponse> toResponseList(List<TEntity> entities);
}