package br.com.clientefacil.controller;

import br.com.clientefacil.dto.UserRequest;
import br.com.clientefacil.dto.UserResponse;
import br.com.clientefacil.dto.UserScreenResponse;
import br.com.clientefacil.service.CompanyService;
import br.com.clientefacil.service.PersonService;
import br.com.clientefacil.service.ProfileService;
import br.com.clientefacil.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;
    private final PersonService personService;
    private final ProfileService profileService;
    private final CompanyService companyService;

    @GetMapping("/screen")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public UserScreenResponse screen() {
        Page<UserResponse> obUser = service.findAll(
                0, 10, "id", "asc"
        );
        Map<Long, String> kvPerson = personService.keyValue();
        Map<Long, String> kvProfile = profileService.keyValue();
        Map<Long, String> kvCompany = companyService.keyValue();

        return new UserScreenResponse(obUser, kvPerson, kvProfile, kvCompany);
    }
    
    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public Page<UserResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return service.findAll(page, size, sort, direction);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public UserResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public UserResponse create(@RequestBody @Valid UserRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public UserResponse update(
            @PathVariable Long id,
            @RequestBody @Valid UserRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}