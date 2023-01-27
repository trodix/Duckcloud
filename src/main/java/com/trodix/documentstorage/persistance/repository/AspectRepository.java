package com.trodix.documentstorage.persistance.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.trodix.documentstorage.persistance.entity.Aspect;
import com.trodix.documentstorage.persistance.entity.QName;

public interface AspectRepository extends JpaRepository<Aspect, Long> {

    Optional<Aspect> findOneByQname(QName qname);

}
