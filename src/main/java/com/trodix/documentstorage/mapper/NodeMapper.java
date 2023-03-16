package com.trodix.documentstorage.mapper;

import com.trodix.documentstorage.model.ContentModel;
import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.request.NodeRepresentationRequest;
import com.trodix.documentstorage.response.NodeRepresentationResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@AllArgsConstructor
public class NodeMapper {

    public NodeRepresentationResponse nodeRepresentationToNodeResponse(final NodeRepresentation result) {
        final NodeRepresentationResponse response = new NodeRepresentationResponse();
        response.setBucket(result.getBucket());
        response.setDirectoryPath(result.getDirectoryPath());
        response.setUuid(result.getUuid());
        response.setVersions(result.getVersions());
        response.setType(result.getType());
        response.setAspects(result.getAspects());
        response.setProperties(result.getProperties());

        return response;
    }

    public NodeRepresentation nodeRepresentationRequestToNodeRepresentation(final NodeRepresentationRequest node, final MultipartFile file) {

        if (file == null) {
            throw new IllegalArgumentException("Node of type " + ContentModel.TYPE_CONTENT + " expects a non null file");
        }

        final NodeRepresentation createNodeData = new NodeRepresentation();
        createNodeData.setBucket(node.getBucket());
        createNodeData.setDirectoryPath(node.getDirectoryPath());
        createNodeData.setContentType(file.getContentType());
        createNodeData.setType(ContentModel.TYPE_CONTENT);
        createNodeData.setAspects(node.getAspects());
        createNodeData.setProperties(node.getProperties());
        createNodeData.getProperties().put(ContentModel.PROP_NAME, file.getOriginalFilename());

        return createNodeData;
    }

    public NodeRepresentation nodeRepresentationRequestToNodeRepresentation(final NodeRepresentationRequest node) {
        final NodeRepresentation createNodeData = new NodeRepresentation();
        createNodeData.setBucket(node.getBucket());
        createNodeData.setDirectoryPath(node.getDirectoryPath());
        createNodeData.setContentType(null);
        createNodeData.setType(ContentModel.TYPE_DIRECTORY);
        createNodeData.setAspects(node.getAspects());

        if (node.getProperties().get(ContentModel.PROP_NAME) == null) {
            throw new IllegalArgumentException("Directory must contain a property " + ContentModel.PROP_NAME);
        }

        createNodeData.setProperties(node.getProperties());

        return createNodeData;
    }

}
