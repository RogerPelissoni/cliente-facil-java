package br.com.clientefacil.messaging.template;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confere, para TODO record que implementa {@link EmailTemplate} — descoberto por classpath scan,
 * sem lista manual —, que as variáveis declaradas no record batem exatamente com as variáveis
 * (`${...}`) que o `.html` correspondente usa de verdade. Um template novo (mesmo o 500º) não exige
 * escrever teste nenhum: basta o record existir no pacote {@code messaging.template} que ele já cai
 * aqui sozinho.
 * <p>
 * Não instancia os records com valores reais — não precisa: só os *nomes* dos componentes importam
 * pra essa checagem, então todo record é construído com todo campo {@code null} (daí a convenção,
 * documentada em {@link EmailTemplate}, de nunca usar tipo primitivo num record de template).
 */
class EmailTemplateVariablesTest {

    private static final String TEMPLATE_PACKAGE = "br.com.clientefacil.messaging.template";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_][a-zA-Z0-9_]*)");

    @TestFactory
    Stream<DynamicTest> cadaTemplateSoUsaVariaveisDeclaradasNoRecordCorrespondente() {
        return findTemplateClasses().stream()
                .map(type -> DynamicTest.dynamicTest(type.getSimpleName(), () -> assertVariablesMatch(type)));
    }

    private void assertVariablesMatch(Class<? extends EmailTemplate> type) throws Exception {
        EmailTemplate instance = instantiateWithNulls(type);

        Set<String> declared = instance.toVariables().keySet();
        Set<String> usedInHtml = extractVariablesFromTemplate(instance.templateName());

        assertThat(usedInHtml)
                .as("Variáveis usadas em templates/email/%s.html devem bater exatamente com os campos de %s "
                                + "(sobrando ou faltando uma, o e-mail sai errado sem nenhum erro em runtime)",
                        instance.templateName(), type.getSimpleName())
                .isEqualTo(declared);
    }

    // Só filtra por herança de EmailTemplate — nenhuma lista de classes pra manter atualizada.
    @SuppressWarnings("unchecked")
    private List<Class<? extends EmailTemplate>> findTemplateClasses() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(EmailTemplate.class));

        return scanner.findCandidateComponents(TEMPLATE_PACKAGE).stream()
                .<Class<? extends EmailTemplate>>map(beanDefinition -> {
                    try {
                        return (Class<? extends EmailTemplate>) Class.forName(beanDefinition.getBeanClassName());
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .filter(Class::isRecord) // exclui a própria interface EmailTemplate
                .toList();
    }

    private EmailTemplate instantiateWithNulls(Class<? extends EmailTemplate> type) throws ReflectiveOperationException {
        RecordComponent[] components = type.getRecordComponents();
        Class<?>[] paramTypes = new Class<?>[components.length];

        for (int i = 0; i < components.length; i++) {
            paramTypes[i] = components[i].getType();
        }

        Constructor<?> constructor = type.getDeclaredConstructor(paramTypes);
        constructor.setAccessible(true);

        return (EmailTemplate) constructor.newInstance(new Object[components.length]);
    }

    private Set<String> extractVariablesFromTemplate(String templateName) throws IOException {
        var resource = new ClassPathResource("templates/email/" + templateName + ".html");

        String html;
        try (var input = resource.getInputStream()) {
            html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        Set<String> variables = new HashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(html);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }

        return variables;
    }
}
