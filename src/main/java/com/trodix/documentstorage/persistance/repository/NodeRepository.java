package com.trodix.documentstorage.persistance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.trodix.documentstorage.persistance.entity.Node;

public interface NodeRepository extends JpaRepository<Node, Long> {

}
