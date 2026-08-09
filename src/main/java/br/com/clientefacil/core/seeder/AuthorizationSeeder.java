package br.com.clientefacil.core.seeder;

import br.com.clientefacil.domain.config.ModuleCode;
import br.com.clientefacil.domain.config.ResourceEnum;
import br.com.clientefacil.entity.Module;
import br.com.clientefacil.entity.Resource;
import br.com.clientefacil.repository.ModuleRepository;
import br.com.clientefacil.repository.ProfilePermissionRepository;
import br.com.clientefacil.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// HIGHEST_PRECEDENCE: precisa rodar antes do CommandLineRunner do MainSeeder, que concede as
// permissões default ao perfil Admin com base no que existir na tabela resource — sem essa ordem
// garantida, num boot "de sorte" o MainSeeder pode rodar primeiro e ver a tabela vazia, deixando o
// admin sem nenhuma permissão (Spring não garante ordem entre ApplicationRunner/CommandLineRunner
// sem @Order explícito).
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthorizationSeeder implements ApplicationRunner {

    private final ModuleRepository moduleRepository;
    private final ResourceRepository resourceRepository;
    private final ProfilePermissionRepository profilePermissionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, Module> modulesByName = syncModules();
        syncResources(modulesByName);
    }

    private Map<String, Module> syncModules() {
        Map<String, ModuleCode> enumModules = Arrays.stream(ModuleCode.values())
                .collect(Collectors.toMap(ModuleCode::getCode, Function.identity()));

        Map<String, Module> dbModules = moduleRepository.findAll().stream()
                .collect(Collectors.toMap(Module::getName, Function.identity()));

        for (ModuleCode code : enumModules.values()) {
            Module existing = dbModules.get(code.getCode());

            if (existing == null) {
                Module module = new Module();
                module.setName(code.getCode());
                moduleRepository.save(module);
            }
        }

        Set<String> enumModuleNames = enumModules.keySet();

        for (Module module : dbModules.values()) {
            if (!enumModuleNames.contains(module.getName())) {
                boolean hasResources = resourceRepository.existsByModuleId(module.getId());

                if (!hasResources) {
                    moduleRepository.delete(module);
                }
            }
        }

        return moduleRepository.findAll().stream()
                .collect(Collectors.toMap(Module::getName, Function.identity()));
    }

    private void syncResources(Map<String, Module> modulesByName) {
        Map<String, ResourceEnum> resourceBySignature = Arrays.stream(ResourceEnum.values())
                .collect(Collectors.toMap(ResourceEnum::getSignature, Function.identity()));

        Map<String, Resource> dbResources = resourceRepository.findAll().stream()
                .collect(Collectors.toMap(Resource::getSignature, Function.identity()));

        for (ResourceEnum code : resourceBySignature.values()) {
            Resource existing = dbResources.get(code.getSignature());
            Module module = modulesByName.get(code.getModule().getCode());

            if (existing == null) {
                Resource resource = new Resource();
                resource.setSignature(code.getSignature());
                resource.setName(code.getDescription());
                resource.setModule(module);
                resourceRepository.save(resource);
                continue;
            }

            boolean changed = false;

            if (!code.getDescription().equals(existing.getName())) {
                existing.setName(code.getDescription());
                changed = true;
            }

            Long currentModuleId = existing.getModule() != null ? existing.getModule().getId() : null;
            Long expectedModuleId = module != null ? module.getId() : null;

            if ((currentModuleId == null && expectedModuleId != null)
                    || (currentModuleId != null && !currentModuleId.equals(expectedModuleId))) {
                existing.setModule(module);
                changed = true;
            }

            if (changed) {
                resourceRepository.save(existing);
            }
        }

        for (Resource resource : dbResources.values()) {
            if (!resourceBySignature.containsKey(resource.getSignature())) {
                profilePermissionRepository.deleteByResourceId(resource.getId());
                resourceRepository.delete(resource);
            }
        }
    }
}
