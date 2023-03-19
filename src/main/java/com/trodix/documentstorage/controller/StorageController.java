package com.trodix.documentstorage.controller;

import com.trodix.documentstorage.mapper.NodeMapper;
import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.request.DirectoryRepresentationRequest;
import com.trodix.documentstorage.request.NodeRepresentationRequest;
import com.trodix.documentstorage.response.NodeRepresentationResponse;
import com.trodix.documentstorage.persistance.entity.StoredFile;
import com.trodix.documentstorage.security.services.AuthenticationService;
import com.trodix.documentstorage.service.NodeManager;
import com.trodix.documentstorage.service.NodeService;
import com.trodix.documentstorage.service.StorageService;
import io.minio.messages.Bucket;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.security.RolesAllowed;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
@Scope(WebApplicationContext.SCOPE_REQUEST)
@AllArgsConstructor
@Slf4j
@RolesAllowed({"ecm-user"})
public class StorageController {

    private final StorageService storageService;

    private final NodeService nodeService;

    private final NodeManager nodeManager;

    private final NodeMapper nodeMapper;

    private final AuthenticationService authService;

    @Operation(summary = "Get the list of available buckets where at least one file is stored")
    @GetMapping(path = "/buckets")
    public List<Bucket> listBuckets() {
        return storageService.listBuckets();
    }

    @Operation(summary = "Create a new directory")
    @PostMapping(path = "/directory", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public NodeRepresentationResponse createDirectory(final DirectoryRepresentationRequest node) {
        final String userId = authService.getUserId();
        log.debug("userId : {}", userId);
        final NodeRepresentation createNodeData = nodeMapper.directoryRepresentationRequestToNodeRepresentation(node);
        final NodeRepresentation result = nodeManager.persistNode(createNodeData, null);

        return nodeMapper.nodeRepresentationToNodeResponse(result);
    }

    @Operation(summary = "Create a new node and attach a file")
    @PostMapping(path = "/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public NodeRepresentationResponse uploadFile(@RequestPart(value = "file") final MultipartFile file, final NodeRepresentationRequest node)
            throws IOException {

        final NodeRepresentation createNodeData = nodeMapper.nodeRepresentationRequestToNodeRepresentation(node, file);
        final NodeRepresentation result = nodeManager.persistNode(createNodeData, file.getBytes());

        return nodeMapper.nodeRepresentationToNodeResponse(result);
    }

    @Operation(summary = "Upload a new version of a file")
    @PutMapping(path = "/{nodeId}/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public NodeRepresentationResponse uploadNewVersion(@PathVariable final String nodeId, @RequestPart(value = "file") final MultipartFile file)
            throws IOException {

        final NodeRepresentation nodeRepresentation = nodeService.findByNodeId(nodeId);

        if (nodeRepresentation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found for id " + nodeId);
        }

        nodeRepresentation.setContentType(file.getContentType());
        final StoredFile result = nodeManager.createContent(nodeRepresentation, file.getBytes());
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

    @Operation(summary = "Delete the node and the associated contents (every versions)")
    @DeleteMapping("/node/{nodeId}")
    public void deleteNode(@PathVariable final String nodeId) {
        final NodeRepresentation node = nodeService.findByNodeId(nodeId);

        if (node == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found for nodeId " + nodeId);
        }

        nodeManager.deleteNode(nodeId);
    }

}
