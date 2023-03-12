package com.trodix.documentstorage.mapper;

import com.trodix.documentstorage.model.ContentModel;
import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.model.NodeRepresentationRequest;
import com.trodix.documentstorage.model.NodeRepresentationResponse;
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
