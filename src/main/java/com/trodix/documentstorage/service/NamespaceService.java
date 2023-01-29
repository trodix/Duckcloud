package com.trodix.documentstorage.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import com.trodix.documentstorage.persistance.dao.NamespaceDAO;
import com.trodix.documentstorage.persistance.entity.Namespace;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class NamespaceService {

    private final NamespaceDAO namespaceDAO;

    public Optional<Namespace> findByName(final String name) {
        return namespaceDAO.findByName(name);
    }

}
