package com.trodix.documentstorage.service;

import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.persistance.dao.NodeDAO;
import com.trodix.documentstorage.persistance.entity.Node;
import com.trodix.documentstorage.persistance.entity.NodeIndex;
import com.trodix.documentstorage.persistance.entity.StoredFile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@AllArgsConstructor
@Slf4j
public class NodeManager {

    private final NodeService nodeService;

    private final NodeIndexerService nodeIndexerService;

    private final NodeDAO nodeDAO;

    public NodeRepresentation persistNode(final NodeRepresentation nodeRep, final byte[] file) {
        NodeRepresentation response = nodeService.persistNode(nodeRep, file);

        //new CompletableFuture<Void>().thenRun(() -> {
            // FIXME value Date retrived from database cause an error in index creation
            // Unable to make field private int java.sql.Timestamp.nanos accessible: module java.sql does not \"opens java.sql\" to unnamed module
            Node node_ = nodeDAO.findByUuId(response.getUuid());
            Node node = nodeService.nodeRepresentationToNode(nodeRep);
            // We need the dbId for the index
            node.setDbId(node_.getDbId());

            final NodeIndex nodeIndex = nodeService.nodeToNodeIndex(node);
            nodeIndexerService.createNodeIndex(nodeIndex);
        //});

        return response;
    }

    public StoredFile createContent(final NodeRepresentation nodeRep, final byte[] file) throws IllegalArgumentException {
        StoredFile response = nodeService.createContent(nodeRep, file);

        //new CompletableFuture<Void>().thenRun(() -> {
            // FIXME value Date retrived from database cause an error in index creation
            // Unable to make field private int java.sql.Timestamp.nanos accessible: module java.sql does not \"opens java.sql\" to unnamed module
            Node node_ = response.getNode();
            Node node = nodeService.nodeRepresentationToNode(nodeRep);
            // We need the dbId for the index
            node.setDbId(node_.getDbId());

            final NodeIndex nodeIndex = nodeService.nodeToNodeIndex(node);
            nodeIndexerService.createNodeIndex(nodeIndex);
        //});

        return response;
    }
}
