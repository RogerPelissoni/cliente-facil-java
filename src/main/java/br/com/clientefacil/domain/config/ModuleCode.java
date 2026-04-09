package br.com.clientefacil.domain.config;

import lombok.Getter;

@Getter
public enum ModuleCode {
    CORE("Core"),
    FINANCIAL("Financeiro");

    private final String description;

    ModuleCode(String description) {
        this.description = description;
    }

    public String getCode() {
        return name();
    }
}