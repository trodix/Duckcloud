package com.trodix.documentstorage.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import com.trodix.documentstorage.persistance.dao.QNameDAO;
import com.trodix.documentstorage.persistance.entity.Namespace;
import com.trodix.documentstorage.persistance.entity.QName;
import com.trodix.documentstorage.persistance.repository.QNameRepository;

@Service
public class QNameService extends QNameModelService {

    private final QNameDAO qnameDAO;

    private final QNameRepository qnameRepository;

    private final NamespaceService namespaceService;

    public QNameService(final QNameDAO qnameDAO, final QNameRepository qnameRepository, final NamespaceService namespaceService,
            final ModelService modelService) {
        super(modelService);
        this.qnameDAO = qnameDAO;
        this.qnameRepository = qnameRepository;
        this.namespaceService = namespaceService;
    }

    public QName save(final QName qname) {

        if (isValid(qname)) {
            return qnameDAO.save(qname);
        }

        throw new IllegalArgumentException("QName " + qnameToString(qname) + " is not registered in model");
    }

    public QName stringToQName(final String qnameString) throws IllegalArgumentException {
        final String[] parts = qnameString.split(":");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid QName representation: " + qnameString);
        }

        final Optional<Namespace> resultNamespace = namespaceService.findByName(parts[0]);

        final Namespace namespace;

        if (resultNamespace.isEmpty()) {
            namespace = new Namespace();
            namespace.setName(parts[0]);
        } else {
            namespace = resultNamespace.get();
        }

        Optional<QName> resultQname = Optional.empty();

        if (namespace.getId() != null) {
            resultQname = qnameRepository.findOneByNamespaceIdAndName(namespace.getId(), parts[1]);
        }

        QName qname;

        if (resultQname.isEmpty()) {
            qname = new QName();
            qname.setNamespace(namespace);
            qname.setName(parts[1]);
            qname = save(qname);
        } else {
            qname = resultQname.get();
        }

        return qname;
    }

    public String qnameToString(final QName qname) {
        return qname.getNamespace().getName() + ":" + qname.getName();
    }

}
