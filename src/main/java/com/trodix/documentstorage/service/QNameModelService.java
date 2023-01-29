package com.trodix.documentstorage.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import com.trodix.documentstorage.persistance.entity.Model;
import com.trodix.documentstorage.persistance.entity.QName;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public abstract class QNameModelService {

    protected final ModelService modelService;

    public boolean isValid(final QName qname) {
        final Optional<Model> model = modelService.findModel(qname);
        return model.isPresent();
    }

}
