package com.trodix.documentstorage.service;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.persistance.dao.NodeDAO;
import com.trodix.documentstorage.persistance.entity.Aspect;
import com.trodix.documentstorage.persistance.entity.Node;
import com.trodix.documentstorage.persistance.entity.Property;
import com.trodix.documentstorage.persistance.entity.Type;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class NodeService {

    private final StorageService storageService;

    private final NodeDAO nodeDAO;

    private final QNameService qnameService;

    private final TypeService typeService;

    private final AspectService aspectService;

    private final PropertyService propertyService;

    public Node nodeRepresentationToNode(final NodeRepresentation nodeRepresentation) throws IllegalArgumentException {

        final Type type = typeService.stringToType(nodeRepresentation.getType());

        final List<Aspect> aspects = new ArrayList<>();
        nodeRepresentation.getAspects().stream().forEach(aspectString -> {
            final Aspect aspect = aspectService.stringToAspect(aspectString);
            aspects.add(aspect);
        });

        final List<Property> properties = new ArrayList<>();

        nodeRepresentation.getProperties().forEach((key, value) -> {
            Property property;
            try {
                property = propertyService.createProperty(qnameService.stringToQName(key), value);
            } catch (final ParseException e) {
                throw new IllegalArgumentException(e);
            }

            properties.add(property);
        });

        final Node node = new Node();
        node.setBucket(nodeRepresentation.getBucket());
        node.setDirectoryPath(nodeRepresentation.getDirectoryPath());
        node.setUuid(StringUtils.isBlank(nodeRepresentation.getUuid()) ? UUID.randomUUID().toString() : nodeRepresentation.getUuid());
        node.setType(type);
        node.setAspects(aspects);
        node.setProperties(properties);

        return node;
    }

    public NodeRepresentation nodeToNodeRepresentation(final Node node) {

        final List<String> aspects = new ArrayList<>();
        node.getAspects().stream().forEach(aspect -> aspects.add(aspectService.aspectToString(aspect)));

        final Map<String, Serializable> properties = new HashMap<>();
        node.getProperties().stream().forEach(property -> {
            final String propertyName = qnameService.qnameToString(property.getQname());
            final Serializable propertyValue = propertyService.getPropertyValue(property);

            properties.put(propertyName, propertyValue);
        });

        final NodeRepresentation nodeRepresentation = new NodeRepresentation();
        nodeRepresentation.setBucket(node.getBucket());
        nodeRepresentation.setDirectoryPath(node.getDirectoryPath());
        nodeRepresentation.setUuid(node.getUuid());
        nodeRepresentation.setType(typeService.typeToString(node.getType()));
        nodeRepresentation.setAspects(aspects);
        nodeRepresentation.setProperties(properties);

        return nodeRepresentation;
    }

    public NodeRepresentation persistNode(final NodeRepresentation nodeRep, final byte[] file) {
        final Node node = nodeRepresentationToNode(nodeRep);
        // TODO support multiple buckets
        node.setBucket(StorageService.ROOT_BUCKET);
        nodeDAO.save(node);

        nodeRep.setUuid(node.getUuid());
        nodeRep.setBucket(node.getBucket());
        nodeRep.setType(typeService.typeToString(node.getType()));

        storageService.uploadFile(nodeRep, file);

        log.debug("File uploaded: {}", nodeRep);

        return nodeRep;
    }


}
