package br.com.clientefacil.domain.config;

import lombok.Getter;

@Getter
public enum ResourceEnum {

    CLIENT_VIEW("Clientes - visualizar", ModuleCode.CORE),
    CLIENT_CREATE("Clientes - criar", ModuleCode.CORE),
    CLIENT_UPDATE("Clientes - editar", ModuleCode.CORE),
    CLIENT_DELETE("Clientes - excluir", ModuleCode.CORE),

    COMPANY_VIEW("Empresas - visualizar", ModuleCode.CORE),
    COMPANY_CREATE("Empresas - criar", ModuleCode.CORE),
    COMPANY_UPDATE("Empresas - editar", ModuleCode.CORE),
    COMPANY_DELETE("Empresas - excluir", ModuleCode.CORE),

    EVENT_VIEW("Eventos - visualizar", ModuleCode.CORE),
    EVENT_CREATE("Eventos - criar", ModuleCode.CORE),
    EVENT_UPDATE("Eventos - editar", ModuleCode.CORE),
    EVENT_DELETE("Eventos - excluir", ModuleCode.CORE),

    NOTIFICATION_SEND("Notificações - enviar para outros usuários", ModuleCode.CORE),

    DEAD_LETTER_VIEW("Mensageria - visualizar falhas (DLQ)", ModuleCode.CORE),
    DEAD_LETTER_RESOLVE("Mensageria - marcar falhas como resolvidas", ModuleCode.CORE),
    DEAD_LETTER_TEST("Mensageria - disparar falha simulada para testar o pipeline de DLQ", ModuleCode.CORE),

    // MAIL_CONFIG é um recurso singleton (config base do sistema + no máximo 1 config por
    // empresa, ver mail_config), não uma lista de registros — por isso só duas permissões
    // (visualizar / gerenciar), em vez do padrão CRUD de 4 usado pelas entidades normais do
    // projeto (criar e editar são a mesma ação de "salvar a configuração").
    MAIL_CONFIG_VIEW("Mensageria - visualizar configuração de e-mail", ModuleCode.CORE),
    MAIL_CONFIG_MANAGE("Mensageria - gerenciar configuração de e-mail (salvar, excluir, testar)", ModuleCode.CORE),

    PERSON_VIEW("Pessoas - visualizar", ModuleCode.CORE),
    PERSON_CREATE("Pessoas - criar", ModuleCode.CORE),
    PERSON_UPDATE("Pessoas - editar", ModuleCode.CORE),
    PERSON_DELETE("Pessoas - excluir", ModuleCode.CORE),

    PROFESSIONAL_VIEW("Profissionais - visualizar", ModuleCode.CORE),
    PROFESSIONAL_CREATE("Profissionais - criar", ModuleCode.CORE),
    PROFESSIONAL_UPDATE("Profissionais - editar", ModuleCode.CORE),
    PROFESSIONAL_DELETE("Profissionais - excluir", ModuleCode.CORE),

    PROFILE_VIEW("Perfis - visualizar", ModuleCode.CORE),
    PROFILE_CREATE("Perfis - criar", ModuleCode.CORE),
    PROFILE_UPDATE("Perfis - editar", ModuleCode.CORE),
    PROFILE_DELETE("Perfis - excluir", ModuleCode.CORE),

    USER_VIEW("Usuários - visualizar", ModuleCode.CORE),
    USER_CREATE("Usuários - criar", ModuleCode.CORE),
    USER_UPDATE("Usuários - editar", ModuleCode.CORE),
    USER_DELETE("Usuários - excluir", ModuleCode.CORE),

    // Protege /actuator/** (ver SecurityConfig) — não é uma permissão de negócio, é acesso a
    // métrica/saúde da infraestrutura em si (JVM, pool de conexão, etc.), então fica de fora do
    // padrão CRUD de 4 permissões usado pelas entidades normais.
    SYSTEM_METRICS_VIEW("Sistema - visualizar métricas e saúde da aplicação (Actuator)", ModuleCode.CORE);

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
