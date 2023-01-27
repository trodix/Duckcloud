package com.trodix.documentstorage.persistance.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.trodix.documentstorage.persistance.entity.Model;
import com.trodix.documentstorage.persistance.entity.QName;

public interface ModelRepository extends JpaRepository<Model, Long> {

    public Optional<Model> findByQname(QName qname);

}
