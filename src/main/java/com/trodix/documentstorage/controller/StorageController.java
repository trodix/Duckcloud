package com.trodix.documentstorage.controller;

import com.trodix.documentstorage.mapper.NodeMapper;
import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.model.NodeRepresentationRequest;
import com.trodix.documentstorage.model.NodeRepresentationResponse;
import com.trodix.documentstorage.persistance.entity.StoredFile;
import com.trodix.documentstorage.service.NodeService;
import com.trodix.documentstorage.service.StorageService;
import io.minio.messages.Bucket;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@AllArgsConstructor
@Slf4j
public class StorageController {

    private final StorageService storageService;

    private final NodeService nodeService;

    private final NodeMapper nodeMapper;
    
    @Operation(summary = "Get the list of available buckets where at least one file is stored")
    @GetMapping(path = "/buckets")
    public List<Bucket> listBuckets() {
        return storageService.listBuckets();
    }

    @Operation(summary = "Create a new node and attach a file")
    @PostMapping(path = "/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public NodeRepresentationResponse uploadFile(@RequestPart(value = "file", required = false) final MultipartFile file, final NodeRepresentationRequest node)
            throws IOException {

        final NodeRepresentation createNodeData = nodeMapper.nodeRepresentationRequestToNodeRepresentation(node, file);
        final NodeRepresentation result = nodeService.persistNode(createNodeData, file.getBytes());

        return nodeMapper.nodeRepresentationToNodeResponse(result);
    }

    @Operation(summary = "Upload a new version of a file")
    @PutMapping(path = "/{nodeId}/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public NodeRepresentationResponse uploadNewVersion(@PathVariable final String nodeId, @RequestPart(value = "file", required = false) final MultipartFile file)
            throws IOException {

        final NodeRepresentation nodeRepresentation = nodeService.findByNodeId(nodeId);

        if (nodeRepresentation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found for id " + nodeId);
        }

        nodeRepresentation.setContentType(file.getContentType());
        final StoredFile result = nodeService.createContent(nodeRepresentation, file.getBytes());
        nodeRepresentation.setVersions(result.getVersion());

        return nodeMapper.nodeRepresentationToNodeResponse(nodeRepresentation);
    }

    @Operation(summary = "Get the content of the latest version of the file attached to the node")
    @GetMapping("/node/{nodeId}/content")
    public ResponseEntity<ByteArrayResource> getNodeContentById(@PathVariable final String nodeId) {
        final NodeRepresentation node = nodeService.findByNodeId(nodeId);

        if (node == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found for nodeId " + nodeId);
        }

        final String fileId = nodeService.findFileContentUuid(nodeId);

        if (fileId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Filecontent not found for nodeId " + nodeId);
        }

        final byte[] data = storageService.getFile(node.getDirectoryPath(), fileId);
        final ByteArrayResource resource = new ByteArrayResource(data);
        
        final String filename = nodeService.getOriginalFileName(nodeService.nodeRepresentationToNode(node));

        return ResponseEntity
                .ok()
                .contentLength(data.length)
                .header("Content-type", "application/octet-stream")
                .header("Content-disposition", "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @Operation(summary = "Get the content of a specific version of the file attached to the node")
    @GetMapping("/node/{nodeId}/version/{version}/content")
    public ResponseEntity<ByteArrayResource> getNodeContentById(@PathVariable final String nodeId, @PathVariable final int version) {
        final NodeRepresentation node = nodeService.findByNodeId(nodeId);

        if (node == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found for nodeId " + nodeId);
        }

        final String fileId = nodeService.findFileContentUuidForVersion(nodeId, version);

        if (fileId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Filecontent not found for nodeId " + nodeId + " and version " + version);
        }

        final byte[] data = storageService.getFile(node.getDirectoryPath(), fileId);
        final ByteArrayResource resource = new ByteArrayResource(data);

        final String filename = nodeService.getOriginalFileName(nodeService.nodeRepresentationToNode(node));

        return ResponseEntity
                .ok()
                .contentLength(data.length)
                .header("Content-type", "application/octet-stream")
                .header("Content-disposition", "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

}
