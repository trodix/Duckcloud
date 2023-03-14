package com.trodix.documentstorage.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trodix.documentstorage.mapper.NodeMapper;
import com.trodix.documentstorage.model.FileStoreMetadata;
import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.request.OnlyOfficeCallbackRequest;
import com.trodix.documentstorage.request.OnlyOfficeDocumentStatus;
import com.trodix.documentstorage.response.NodeRepresentationResponse;
import com.trodix.documentstorage.response.OnlyOfficeUpdatedDocumentResponse;
import com.trodix.documentstorage.service.*;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.http.entity.ContentType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

@RestController
@AllArgsConstructor
@Slf4j
@CrossOrigin("*")
@RequestMapping("/integration/onlyoffice")
public class OnlyOfficeIntegrationController {

    private final StorageService storageService;

    private final NodeService nodeService;

    private final NodeManager nodeManager;

    private final NodeMapper nodeMapper;

    @Operation(summary = "Get the content of the latest version of the file attached to the node")
    @GetMapping("/document/{nodeId}/contents")
    public ResponseEntity<ByteArrayResource> getDocumentContentByNodeId(@PathVariable final String nodeId) {
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

    @Operation(summary = "Update the file content", description = "See https://api.onlyoffice.com/editors/callback")
    @PostMapping(path = "/document", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public OnlyOfficeUpdatedDocumentResponse updateDocumentContent(@Valid @RequestBody final OnlyOfficeCallbackRequest data) throws JsonProcessingException {

        log.debug("Document data received from OnlyOffice: \n" + data);

        switch (data.getStatus()) {
            case READY_FOR_SAVING:
            case SAVING_ERROR:
            case DOCUMENT_EDITED_STATE_SAVED:
            case FORCE_SAVING_ERROR:
                updateDocument(data.getKey(), data.getUrl());
        }

        return new OnlyOfficeUpdatedDocumentResponse(0);
    }

    private void updateDocument(String nodeId, String url) {
        log.debug("Updating document (nodeId={}) from url: {}", nodeId, url);

        if (url == null) {
            return;
        }

        NodeRepresentation node = nodeService.findByNodeId(nodeId);
        String documentId = nodeService.findFileContentUuid(nodeId);

        try(InputStream is = new URL(url).openConnection().getInputStream()) {

            FileStoreMetadata fileStoreMetadata = new FileStoreMetadata();
            fileStoreMetadata.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"); // FIXME
            fileStoreMetadata.setUuid(documentId);
            fileStoreMetadata.setBucket(node.getBucket());
            fileStoreMetadata.setDirectoryPath(node.getDirectoryPath());

            storageService.uploadFile(fileStoreMetadata, is.readAllBytes());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
