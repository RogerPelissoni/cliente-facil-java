package br.com.clientefacil.mapper;

import br.com.clientefacil.dto.AccountReceivableResponse;
import br.com.clientefacil.entity.AccountReceivable;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountReceivableMapper {

    AccountReceivableResponse toResponse(AccountReceivable entity);
}
