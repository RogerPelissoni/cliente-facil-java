package br.com.clientefacil.domain.config;

import lombok.Getter;

@Getter
public enum ResourceEnum {

    USER_VIEW("Usuários - visualizar", ModuleCode.CORE),
    USER_CREATE("Usuários - criar", ModuleCode.CORE),
    USER_UPDATE("Usuários - editar", ModuleCode.CORE),
    USER_DELETE("Usuários - excluir", ModuleCode.CORE),

    PROFILE_VIEW("Perfis - visualizar", ModuleCode.CORE),
    PROFILE_CREATE("Perfis - criar", ModuleCode.CORE),
    PROFILE_UPDATE("Perfis - editar", ModuleCode.CORE),
    PROFILE_DELETE("Perfis - excluir", ModuleCode.CORE),

    PERSON_VIEW("Pessoas - visualizar", ModuleCode.CORE),
    PERSON_CREATE("Pessoas - criar", ModuleCode.CORE),
    PERSON_UPDATE("Pessoas - editar", ModuleCode.CORE),
    PERSON_DELETE("Pessoas - excluir", ModuleCode.CORE),

    COMPANY_VIEW("Empresas - visualizar", ModuleCode.CORE),
    COMPANY_CREATE("Empresas - criar", ModuleCode.CORE),
    COMPANY_UPDATE("Empresas - editar", ModuleCode.CORE),
    COMPANY_DELETE("Empresas - excluir", ModuleCode.CORE);

    private final String description;
    private final ModuleCode module;

    ResourceEnum(String description, ModuleCode module) {
        this.description = description;
        this.module = module;
    }

    public String getSignature() {
        return name();
    }
}