package com.trodix.documentstorage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.trodix.documentstorage.entity.Node;

public interface NodeRepository extends JpaRepository<Node, Long> {

}
