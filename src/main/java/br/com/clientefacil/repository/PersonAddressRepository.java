package br.com.clientefacil.repository;

import br.com.clientefacil.entity.PersonAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PersonAddressRepository extends JpaRepository<PersonAddress, Long>, JpaSpecificationExecutor<PersonAddress> {
}
