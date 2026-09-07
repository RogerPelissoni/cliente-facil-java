package br.com.clientefacil.entity;

import br.com.clientefacil.core.dto.UserRoleEnum;
import br.com.clientefacil.entity.enums.AccountReceivableMovementPaymentTypeEnum;
import br.com.clientefacil.entity.enums.AccountReceivableMovementTypeEnum;
import br.com.clientefacil.entity.enums.AccountReceivableStatusEnum;
import br.com.clientefacil.entity.enums.EventStatusEnum;
import br.com.clientefacil.entity.enums.EventTypeEnum;
import br.com.clientefacil.entity.enums.PersonGenderEnum;
import br.com.clientefacil.repository.AccountReceivableMovementRepository;
import br.com.clientefacil.repository.AccountReceivableRepository;
import br.com.clientefacil.repository.ClientRepository;
import br.com.clientefacil.repository.CompanyRepository;
import br.com.clientefacil.repository.EventRepository;
import br.com.clientefacil.repository.PersonRepository;
import br.com.clientefacil.repository.ProfessionalRepository;
import br.com.clientefacil.repository.ProfileRepository;
import br.com.clientefacil.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SoftDeletableEntitiesTest} garante que a declaração (`@SQLRestriction`) está correta — este
 * teste vai além: prova o COMPORTAMENTO real, contra o Postgres de verdade, pras 8 entidades. Não
 * depende de qual mecanismo está por trás (`@SQLRestriction` na leitura, `ForeignKeyDeletionGuard` no
 * delete) — se algum dia esse mecanismo mudar de novo (mais uma tentativa de automação, uma versão
 * nova do Hibernate, etc.), este teste continua validando sem precisar ser reescrito, porque só
 * verifica o resultado observável: depois de {@code repository.delete(...)},
 * <ol>
 *     <li>uma busca via Hibernate (`findById`) não encontra mais o registro;</li>
 *     <li>a linha continua existindo fisicamente no banco, com `deleted_at` preenchido — consultado
 *     via {@link JdbcTemplate} puro, sem passar pelo Hibernate, pra não correr o risco do próprio
 *     mecanismo sendo testado mascarar um bug (ex.: se o @SQLRestriction escondesse uma linha que na
 *     verdade foi apagada de verdade, uma consulta via Hibernate não pegaria isso).</li>
 * </ol>
 * {@code @Transactional} — cada teste desfaz sozinho ao final (rollback), banco sempre volta limpo,
 * sem precisar do reset manual (`DROP SCHEMA ...`) que o smoke test manual desta sessão exigiu.
 */
@SpringBootTest
@Transactional
class SoftDeleteBehaviorIntegrationTest {

    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private ProfessionalRepository professionalRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private AccountReceivableRepository accountReceivableRepository;
    @Autowired
    private AccountReceivableMovementRepository accountReceivableMovementRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    @Test
    void client_disappearsFromReadsButStaysInDatabase_afterDelete() {
        Client client = new Client();
        client.setPerson(persistPerson("Pessoa Teste Client"));
        client = clientRepository.save(client);
        Long id = client.getId();

        assertThat(clientRepository.findById(id)).as("sanity check antes do delete").isPresent();

        clientRepository.delete(client);
        flushAndClear();

        assertThat(clientRepository.findById(id)).isEmpty();
        assertSoftDeleted("client", id);
    }

    @Test
    void professional_disappearsFromReadsButStaysInDatabase_afterDelete() {
        Professional professional = new Professional();
        professional.setPerson(persistPerson("Pessoa Teste Professional"));
        professional = professionalRepository.save(professional);
        Long id = professional.getId();

        professionalRepository.delete(professional);
        flushAndClear();

        assertThat(professionalRepository.findById(id)).isEmpty();
        assertSoftDeleted("professional", id);
    }

    @Test
    void person_disappearsFromReadsButStaysInDatabase_afterDelete() {
        Person person = persistPerson("Pessoa Teste Person");
        Long id = person.getId();

        personRepository.delete(person);
        flushAndClear();

        assertThat(personRepository.findById(id)).isEmpty();
        assertSoftDeleted("person", id);
    }

    @Test
    void company_disappearsFromReadsButStaysInDatabase_afterDelete() {
        Company company = new Company();
        company.setName("Empresa Teste Soft Delete");
        company = companyRepository.save(company);
        Long id = company.getId();

        companyRepository.delete(company);
        flushAndClear();

        assertThat(companyRepository.findById(id)).isEmpty();
        assertSoftDeleted("company", id);
    }

    @Test
    void event_disappearsFromReadsButStaysInDatabase_afterDelete() {
        Event event = new Event();
        event.setDsTitle("Evento Teste Soft Delete");
        event.setDtStart(LocalDateTime.now());
        event.setDtEnd(LocalDateTime.now().plusHours(1));
        event.setTpStatus(EventStatusEnum.SCHEDULED);
        event.setTpEvent(EventTypeEnum.PERSONAL);
        event = eventRepository.save(event);
        Long id = event.getId();

        eventRepository.delete(event);
        flushAndClear();

        assertThat(eventRepository.findById(id)).isEmpty();
        assertSoftDeleted("event", id);
    }

    @Test
    void accountReceivable_disappearsFromReadsButStaysInDatabase_afterDelete() {
        AccountReceivable accountReceivable = newAccountReceivable(persistPerson("Pessoa Teste AccountReceivable"));
        accountReceivable = accountReceivableRepository.save(accountReceivable);
        Long id = accountReceivable.getId();

        accountReceivableRepository.delete(accountReceivable);
        flushAndClear();

        assertThat(accountReceivableRepository.findById(id)).isEmpty();
        assertSoftDeleted("account_receivable", id);
    }

    @Test
    void accountReceivableMovement_disappearsFromReadsButStaysInDatabase_afterDelete() {
        AccountReceivable accountReceivable = newAccountReceivable(persistPerson("Pessoa Teste ARM"));
        accountReceivable = accountReceivableRepository.save(accountReceivable);

        AccountReceivableMovement movement = new AccountReceivableMovement();
        movement.setAccountReceivable(accountReceivable);
        movement.setVlMovement(50.0);
        movement.setVlDiscount(0.0);
        movement.setDtMovement(LocalDateTime.now());
        movement.setTpPayment(AccountReceivableMovementPaymentTypeEnum.PIX);
        movement.setTpMovement(AccountReceivableMovementTypeEnum.PAYMENT);
        movement.setDsObservations("movimentação de teste");
        movement = accountReceivableMovementRepository.save(movement);
        Long id = movement.getId();

        accountReceivableMovementRepository.delete(movement);
        flushAndClear();

        assertThat(accountReceivableMovementRepository.findById(id)).isEmpty();
        assertSoftDeleted("account_receivable_movement", id);
    }

    @Test
    void user_disappearsFromReadsButStaysInDatabase_afterDelete() {
        var profile = profileRepository.findByName("Admin")
                .orElseThrow(() -> new IllegalStateException("MainSeeder devia ter criado o profile Admin"));

        User user = new User();
        user.setName("Usuário Teste Soft Delete");
        user.setEmail("teste-softdelete-" + System.nanoTime() + "@example.com");
        user.setPassword("hash-fake-nao-usado-neste-teste");
        user.setRole(UserRoleEnum.admin);
        user.setPerson(persistPerson("Pessoa Teste User"));
        user.setProfile(profile);
        user = userRepository.save(user);
        Long id = user.getId();

        userRepository.delete(user);
        flushAndClear();

        assertThat(userRepository.findById(id)).isEmpty();
        assertSoftDeleted("users", id);
    }

    // Sem isto, findById() logo após delete() volta vazio só por bookkeeping em memória do JPA (uma
    // entidade removida no mesmo persistence context já é tratada como ausente, sem round-trip no
    // banco) — daria falso positivo, o teste passaria mesmo se o ForeignKeyDeletionGuard não tivesse
    // feito nada de verdade. flush() força a execução real (dispara PreDeleteEvent/guard); clear()
    // esvazia o persistence context, obrigando o findById() seguinte a consultar o banco de novo em
    // vez de responder do cache de 1º nível.
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private Person persistPerson(String name) {
        Person person = new Person();
        person.setName(name);
        person.setDsDocument("TESTE-" + System.nanoTime());
        person.setTpGender(PersonGenderEnum.M);
        return personRepository.save(person);
    }

    private AccountReceivable newAccountReceivable(Person person) {
        AccountReceivable accountReceivable = new AccountReceivable();
        accountReceivable.setPerson(person);
        accountReceivable.setDsCode("TESTE-" + System.nanoTime());
        accountReceivable.setNrInstallment(1);
        accountReceivable.setVlTotal(100.0);
        accountReceivable.setVlBalance(100.0);
        accountReceivable.setDaDue(LocalDate.now().plusDays(30));
        accountReceivable.setTpStatus(AccountReceivableStatusEnum.PENDING);
        return accountReceivable;
    }

    // Consulta direto via JdbcTemplate (não via Hibernate/repository) de propósito — provar que a
    // linha existe fisicamente sem depender do mesmo mecanismo (@SQLRestriction) que esconde ela do
    // lado do Hibernate.
    private void assertSoftDeleted(String table, Long id) {
        LocalDateTime deletedAt = jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM " + table + " WHERE id = ?", LocalDateTime.class, id);

        assertThat(deletedAt)
                .as(table + " id=" + id + " devia continuar existindo fisicamente, com deleted_at preenchido")
                .isNotNull();
    }
}
