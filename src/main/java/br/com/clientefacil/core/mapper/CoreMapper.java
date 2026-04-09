package br.com.clientefacil.core.mapper;

import java.util.List;

public interface CoreMapper<TEntity, TResponse> {
    TResponse toResponse(TEntity entity);

    List<TResponse> toResponseList(List<TEntity> entities);
}