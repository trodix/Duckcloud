package com.trodix.documentstorage.service;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import com.trodix.documentstorage.model.ContentModel;
import com.trodix.documentstorage.persistance.dao.TypeDAO;
import com.trodix.documentstorage.persistance.entity.QName;
import com.trodix.documentstorage.persistance.entity.Type;

@Service
public class TypeService extends QNameModelService {

    private final TypeDAO typeDAO;

    private final QNameService qnameService;

    public TypeService(final TypeDAO typeDAO, final QNameService qnameService, final ModelService modelService) {
        super(modelService);
        this.typeDAO = typeDAO;
        this.qnameService = qnameService;
    }

    public Type save(final Type type) {
        if (isValid(type.getQname())) {
            return typeDAO.save(type);
        }

        throw new IllegalArgumentException("Type " + qnameService.qnameToString(type.getQname()) + " is not registered in model");
    }

    public Optional<Type> findOneByQname(final QName qname) {
        return typeDAO.findOneByQname(qname);
    }

    public Type stringToType(final String typeString) {
        final QName qnameType;

        if (StringUtils.isEmpty(typeString)) {
            qnameType = qnameService.save(qnameService.stringToQName(ContentModel.TYPE_CONTENT));
        } else {
            qnameType = qnameService.stringToQName(typeString);
        }

        final Optional<Type> resultType = findOneByQname(qnameType);
        final Type type;

        if (resultType.isEmpty()) {
            type = new Type();
            type.setQname(qnameType);
        } else {
            type = resultType.get();
        }

        return type;
    }

    public String typeToString(final Type type) {
        return qnameService.qnameToString(type.getQname());
    }


}
