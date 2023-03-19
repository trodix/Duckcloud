package com.trodix.documentstorage.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import com.trodix.documentstorage.persistance.dao.ModelDAO;
import com.trodix.documentstorage.persistance.entity.Model;
import com.trodix.documentstorage.persistance.entity.Namespace;
import com.trodix.documentstorage.persistance.entity.PropertyType;
import com.trodix.documentstorage.persistance.entity.QName;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class ModelService {

    private final ModelDAO modelDAO;

    public Model save(final Model model) {
        return modelDAO.save(model);
    }

    public Optional<Model> findModel(final QName qname) {
        return modelDAO.findByQname(qname);
    }

    public long count() {
        return modelDAO.count();
    }

    public Model buildModel(final String qnameString) {

        final String[] parts = qnameString.split(":");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid QName representation: " + qnameString);
        }

        final Namespace namespaceObj = new Namespace();
        namespaceObj.setName(parts[0]);

        final QName qname = new QName();
        qname.setNamespace(namespaceObj);
        qname.setName(parts[1]);

        final Model model = new Model();
        model.setQname(qname);

        return model;
    }

    public Model buildModel(final String qnameString, final PropertyType type) {
        final Model model = buildModel(qnameString);
        model.setType(type);

        return model;
    }

}
