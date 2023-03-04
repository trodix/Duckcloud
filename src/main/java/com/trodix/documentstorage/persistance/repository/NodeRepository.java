package com.trodix.documentstorage.persistance.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.trodix.documentstorage.persistance.entity.Node;

public interface NodeRepository extends JpaRepository<Node, Long> {

    public List<Node> findByDirectoryPath(String path);

    public Optional<Node> findByUuid(String uuid);

}
