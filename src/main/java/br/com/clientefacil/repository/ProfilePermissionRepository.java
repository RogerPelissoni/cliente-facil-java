package br.com.clientefacil.repository;

import br.com.clientefacil.entity.ProfilePermission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfilePermissionRepository extends JpaRepository<ProfilePermission, Long> {

    @EntityGraph(attributePaths = {
            "resource",
            "resource.module"
    })
    List<ProfilePermission> findAllByProfileId(Long profileId);

    boolean existsByProfileIdAndResourceId(Long profileId, Long resourceId);

    void deleteByResourceId(Long resourceId);
}
