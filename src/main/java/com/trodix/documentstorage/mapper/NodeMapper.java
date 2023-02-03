package com.trodix.documentstorage.mapper;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.trodix.documentstorage.model.ContentModel;
import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.model.NodeRepresentationRequest;
import com.trodix.documentstorage.model.NodeRepresentationResponse;
import com.trodix.documentstorage.persistance.entity.Node;
import com.trodix.documentstorage.persistance.entity.NodeIndex;
import com.trodix.documentstorage.persistance.entity.Property;
import com.trodix.documentstorage.service.AspectService;
import com.trodix.documentstorage.service.PropertyService;
import com.trodix.documentstorage.service.QNameService;
import com.trodix.documentstorage.service.TypeService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class NodeMapper {

    private final TypeService typeService;

    private final AspectService aspectService;

    private final PropertyService propertyService;

    private final QNameService qnameService;

    public Node nodeIndexToNode(final NodeIndex nodeIndex) {
        final Node node = new Node();

        node.setDbId(nodeIndex.getDbId());
        node.setUuid(nodeIndex.getUuid());
        node.setDirectoryPath(nodeIndex.getDirectoryPath());
        node.setBucket(nodeIndex.getBucket());
        node.setType(typeService.stringToType(nodeIndex.getType()));
        node.setAspects(nodeIndex.getAspects().stream().map(aspectService::stringToAspect).toList());
        node.setProperties(new ArrayList<>());

        if (nodeIndex.getProperties() != null) {
            nodeIndex.getProperties().forEach((k, v) -> {
                try {
                    final Property p = propertyService.createProperty(qnameService.stringToQName(k), v);
                    node.getProperties().add(p);

                } catch (final ParseException e) {
                    log.error(e.getMessage(), e);
                }

            });
        }

        return node;
    }

    public NodeIndex nodeToNodeIndex(final Node node) {
        final NodeIndex nodeIndex = new NodeIndex();

        nodeIndex.setDbId(node.getDbId());
        nodeIndex.setUuid(node.getUuid());
        nodeIndex.setDirectoryPath(node.getDirectoryPath());
        nodeIndex.setBucket(node.getBucket());
        nodeIndex.setType(typeService.typeToString(node.getType()));
        nodeIndex.setAspects(node.getAspects().stream().map(aspectService::aspectToString).toList());

        final Map<String, Serializable> properties = new HashMap<>();

        node.getProperties().forEach(p -> properties.put(qnameService.qnameToString(p.getQname()), propertyService.getPropertyValue(p)));

        nodeIndex.setProperties(properties);

        return nodeIndex;
    }

    public NodeRepresentationResponse nodeRepresentationToNodeResponse(final NodeRepresentation result) {
        final NodeRepresentationResponse response = new NodeRepresentationResponse();
        response.setBucket(result.getBucket());
        response.setDirectoryPath(result.getDirectoryPath());
        response.setUuid(result.getUuid());
        response.setType(result.getType());
        response.setAspects(result.getAspects());
        response.setProperties(result.getProperties());

        return response;
    }

    public NodeRepresentation nodeRepresentationRequestToNodeRepresentation(final NodeRepresentationRequest node, final MultipartFile file) {
        final NodeRepresentation createNodeData = new NodeRepresentation();
        createNodeData.setBucket(node.getBucket());
        createNodeData.setDirectoryPath(node.getDirectoryPath());
        createNodeData.setContentType(file.getContentType());
        createNodeData.setType(node.getType());
        createNodeData.setAspects(node.getAspects());
        createNodeData.setProperties(node.getProperties());
        createNodeData.getProperties().put(ContentModel.PROP_NAME, file.getOriginalFilename());

        return createNodeData;
    }

}
