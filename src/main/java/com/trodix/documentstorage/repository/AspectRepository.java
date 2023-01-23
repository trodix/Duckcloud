package com.trodix.documentstorage.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.trodix.documentstorage.entity.Aspect;
import com.trodix.documentstorage.entity.QName;

public interface AspectRepository extends JpaRepository<Aspect, Long> {

    Optional<Aspect> findOneByQname(QName qname);

}
