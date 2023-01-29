package com.trodix.documentstorage.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import com.trodix.documentstorage.persistance.dao.AspectDAO;
import com.trodix.documentstorage.persistance.entity.Aspect;
import com.trodix.documentstorage.persistance.entity.QName;

@Service
public class AspectService extends QNameModelService {

    private final AspectDAO aspectDAO;

    private final QNameService qnameService;

    public AspectService(final AspectDAO aspectDAO, final QNameService qnameService, final ModelService modelService) {
        super(modelService);
        this.aspectDAO = aspectDAO;
        this.qnameService = qnameService;
    }

    public Aspect save(final Aspect aspect) {
        if (isValid(aspect.getQname())) {
            return aspectDAO.save(aspect);
        }

        throw new IllegalArgumentException("Aspect " + qnameService.qnameToString(aspect.getQname()) + " is not registered in model");
    }

    public String aspectToString(final Aspect aspect) {
        return qnameService.qnameToString(aspect.getQname());
    }

    public Aspect stringToAspect(final String aspectString) {
        final QName qnameAspect = qnameService.stringToQName(aspectString);
        final Optional<Aspect> resultAspect = aspectDAO.findByQname(qnameAspect);

        final Aspect aspect;

        if (resultAspect.isEmpty()) {
            aspect = new Aspect();
            aspect.setQname(qnameAspect);
        } else {
            aspect = resultAspect.get();
        }

        return aspect;
    }

}
