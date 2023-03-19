package com.trodix.documentstorage.service;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.springframework.stereotype.Service;
import com.trodix.documentstorage.persistance.dao.PropertyDAO;
import com.trodix.documentstorage.persistance.entity.Model;
import com.trodix.documentstorage.persistance.entity.Property;
import com.trodix.documentstorage.persistance.entity.QName;

@Service
public class PropertyService extends QNameModelService {

    private final PropertyDAO propertyDAO;

    private final QNameService qnameService;

    public PropertyService(final PropertyDAO propertyDAO, final ModelService modelService, final QNameService qnameService) {
        super(modelService);
        this.propertyDAO = propertyDAO;
        this.qnameService = qnameService;
    }

    public Property save(final Property property) {
        if (isValid(property.getQname())) {
            return propertyDAO.save(property);
        }

        throw new IllegalArgumentException("Property " + qnameService.qnameToString(property.getQname()) + " is not registered in model");
    }

    public Property createProperty(final QName qname, final Serializable value) throws ParseException {

        // Validate model
        final Model model = getPropertyTypeAssociation(qname);

        if (model.getType() == null) {
            throw new IllegalArgumentException("Can't set a property value for qname" + qnameService.qnameToString(qname) + " associated to a model with type null");
        }

        final Property p = new Property();
        p.setQname(qname);

        switch (model.getType()) {
            case STRING:
                if (value == null) {
                    p.setStringValue(null);
                    break;
                }
                p.setStringValue(value.toString());
                break;
            case LONG:
                p.setLongValue(Long.parseLong(value.toString()));
                break;
            case DOUBLE:
                p.setDoubleValue(Double.parseDouble(value.toString()));
                break;
            case DATE:
                final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                p.setDateValue(df.parse(value.toString()));
                break;
            case SERIALIZABLE:
                p.setSerializableValue(value);
                break;
        }

        return p;
    }

    public Serializable getPropertyValue(final Property property) {

        if (property.getStringValue() != null) {
            return property.getStringValue();
        } else if (property.getLongValue() != null) {
            return property.getLongValue();
        } else if (property.getDoubleValue() != null) {
            return property.getDoubleValue();
        } else if (property.getDateValue() != null) {
            return property.getDateValue();
        } else if (property.getSerializableValue() != null) {
            return property.getSerializableValue();
        }

        return null;
    }

    public Model getPropertyTypeAssociation(final QName qname) throws IllegalArgumentException {
        return modelService.findModel(qname).orElseThrow(() -> new IllegalArgumentException("QName " + qname.getName() + " not registered in model"));
    }

}
