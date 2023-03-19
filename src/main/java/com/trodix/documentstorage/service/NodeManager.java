package com.trodix.documentstorage.service;

import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.persistance.dao.NodeDAO;
import com.trodix.documentstorage.persistance.entity.Node;
import com.trodix.documentstorage.persistance.entity.NodeIndex;
import com.trodix.documentstorage.persistance.entity.StoredFile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class NodeManager {

    private final NodeService nodeService;

    private final NodeIndexerService nodeIndexerService;

    private final NodeDAO nodeDAO;

    private final StorageService storageService;

    public NodeRepresentation persistNode(final NodeRepresentation nodeRep, @Nullable final byte[] file) {
        NodeRepresentation response = nodeService.persistNode(nodeRep, file);

        //new CompletableFuture<Void>().thenRun(() -> {
            // FIXME value Date retrived from database cause an error in index creation
            // Unable to make field private int java.sql.Timestamp.nanos accessible: module java.sql does not \"opens java.sql\" to unnamed module
            Node node_ = nodeDAO.findByUuId(response.getUuid());
            Node node = nodeService.nodeRepresentationToNode(response);
            node.setVersions(node_.getVersions());
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
            node.setVersions(node_.getVersions());
            // We need the dbId for the index
            node.setDbId(node_.getDbId());

            final NodeIndex nodeIndex = nodeService.nodeToNodeIndex(node);
            nodeIndexerService.createNodeIndex(nodeIndex);
        //});

        return response;
    }

    public void deleteNode(final String nodeId) {
        deleteNode(nodeId, true);
    }

    /**
     * Delete a node.
     *
     * If the node is a type content, all related file contents versions will be deleted
     * If the node is a type directory, this will also delete all children directories and node contents recursively
     *
     * @param nodeId
     */
    public void deleteNode(final String nodeId, boolean recursive) {
        final Node nodeToDelete = nodeDAO.findByUuId(nodeId);

        if (nodeToDelete != null) {

            if (recursive) {
                List<Node> children = nodeService.findChildren(nodeId);

                for (Node child : children) {
                    deleteNode(child.getUuid(), false);
                }
            }

            final NodeIndex nodeIndex = nodeService.nodeToNodeIndex(nodeToDelete);
            log.debug("Deleting node index for {} with path={}", nodeToDelete.getUuid(), nodeService.getFullPath(nodeToDelete));
            nodeIndexerService.deleteNodeIndex(nodeIndex);

            log.debug("Deleting node {} with path={}", nodeToDelete.getUuid(), nodeService.getFullPath(nodeToDelete));
            nodeService.deleteNode(nodeToDelete);

            for (String fileId : nodeService.findAllFileContentVersions(nodeId)) {
                storageService.deleteFile(nodeToDelete.getDirectoryPath(), fileId);
            }

        }

    }
}
