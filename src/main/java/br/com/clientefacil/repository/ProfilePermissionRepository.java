package br.com.clientefacil.repository;

import br.com.clientefacil.entity.ProfilePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfilePermissionRepository extends JpaRepository<ProfilePermission, Long> {
    List<ProfilePermission> findAllByProfileId(Long profileId);
}
