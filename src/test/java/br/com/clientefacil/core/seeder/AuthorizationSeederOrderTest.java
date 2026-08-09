package br.com.clientefacil.core.seeder;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regressão de um bug real desta sessão: sem @Order explícito, o CommandLineRunner do MainSeeder
 * (que concede permissões ao perfil Admin com base no que já existir na tabela `resource`) podia
 * rodar ANTES do ApplicationRunner do AuthorizationSeeder (que povoa essa mesma tabela) — resultado:
 * um boot "de sorte" deixava o admin sem nenhuma permissão, silenciosamente (nenhum erro, nenhum
 * log de falha — só profile_permission vazio). Corrigido com @Order(HIGHEST_PRECEDENCE) aqui.
 * <p>
 * Este teste não prova que a seedagem em si funciona (isso é validado manualmente/em boot real) —
 * só garante que ninguém remove o @Order numa refatoração futura sem perceber a dependência.
 */
class AuthorizationSeederOrderTest {

    @Test
    void mustRunBeforeAnyRunnerWithoutExplicitOrder() {
        int order = OrderUtils.getOrder(AuthorizationSeeder.class, Ordered.LOWEST_PRECEDENCE);

        assertThat(order)
                .as("AuthorizationSeeder precisa de @Order(Ordered.HIGHEST_PRECEDENCE) — o " +
                        "CommandLineRunner do MainSeeder depende da tabela resource já estar populada")
                .isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
