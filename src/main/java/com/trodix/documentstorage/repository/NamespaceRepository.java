package com.trodix.documentstorage.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.trodix.documentstorage.entity.Namespace;

public interface NamespaceRepository extends JpaRepository<Namespace, Long> {

    Optional<Namespace> findOneByName(String name);

}
