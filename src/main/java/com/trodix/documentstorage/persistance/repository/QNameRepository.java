package com.trodix.documentstorage.persistance.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.trodix.documentstorage.persistance.entity.QName;

public interface QNameRepository extends JpaRepository<QName, Long> {

    Optional<QName> findOneByNamespaceIdAndName(Long id, String name);

    Optional<QName> findOneByName(String name);

}
