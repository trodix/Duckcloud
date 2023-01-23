package com.trodix.documentstorage.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.trodix.documentstorage.entity.QName;

public interface QNameRepository extends JpaRepository<QName, Long> {

    Optional<QName> findOneByNamespaceIdAndName(Long id, String name);

}
