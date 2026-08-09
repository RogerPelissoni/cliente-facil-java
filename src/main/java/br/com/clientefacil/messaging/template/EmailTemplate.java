package br.com.clientefacil.messaging.template;

import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contrato tipado entre código Java e um template Thymeleaf (`resources/templates/email/*.html`):
 * cada `.html` tem um record correspondente aqui, cujos componentes são exatamente as variáveis que
 * o template espera. Resolve dois problemas do jeito antigo — {@code sendTemplated(String template,
 * Map<String,Object> variables)} — que só existiam em runtime (e sem lançar erro, já que Thymeleaf
 * simplesmente renderiza `${chaveErrada}` como vazio):
 * <p>
 * 1. Passar a chave errada (typo) para uma variável — agora é erro de compilação (campo do record
 * não existe).
 * 2. Esquecer de passar uma variável que o `.html` usa — não dá pra pegar em compile-time (é HTML,
 * não Java), mas {@code EmailTemplateVariablesTest} confere isso automaticamente pra cada record
 * encontrado neste pacote, sem precisar escrever um teste novo por template.
 * <p>
 * Convenção: use sempre tipos referência (`String`, `Long`, `Integer`...), nunca primitivos (`long`,
 * `int`...) — o teste instancia cada record com todos os campos `null` (não usa os valores, só os
 * nomes), e primitivos não aceitam `null`.
 */
public interface EmailTemplate {

    // Nome do arquivo em resources/templates/email/, sem extensão.
    String templateName();

    default Map<String, Object> toVariables() {
        Map<String, Object> variables = new LinkedHashMap<>();

        for (RecordComponent component : getClass().getRecordComponents()) {
            try {
                variables.put(component.getName(), component.getAccessor().invoke(this));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "Falha ao ler as variáveis do template '" + templateName() + "'", e);
            }
        }

        return variables;
    }
}
