package com.trodix.documentstorage.controller;

import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.model.NodeRepresentationRequest;
import com.trodix.documentstorage.model.NodeRepresentationResponse;
import com.trodix.documentstorage.service.NodeService;
import com.trodix.documentstorage.service.StorageService;
import io.minio.messages.Bucket;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@AllArgsConstructor
@Slf4j
public class StorageController {

    private final StorageService storageService;

    private final NodeService nodeService;

    @GetMapping(path = "/buckets")
    public List<Bucket> listBuckets() {
        return storageService.listBuckets();
    }

    @PostMapping(path = "/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public NodeRepresentationResponse uploadFile(@RequestPart(value = "file", required = false) final MultipartFile file, final NodeRepresentationRequest node)
            throws IOException {

        final NodeRepresentation createNodeData = new NodeRepresentation();
        createNodeData.setBucket(node.getBucket());
        createNodeData.setDirectoryPath(node.getDirectoryPath());
        createNodeData.setFileName(file.getOriginalFilename());
        createNodeData.setContentType(file.getContentType());
        createNodeData.setAspects(node.getAspects());
        createNodeData.setProperties(node.getProperties());
        
        final NodeRepresentation result = nodeService.persistNode(createNodeData, file.getBytes());

        NodeRepresentationResponse response = new NodeRepresentationResponse();
        response.setBucket(result.getBucket());
        response.setDirectoryPath(result.getDirectoryPath());
        response.setUuid(result.getUuid());
        response.setFileName(result.getFileName());
        response.setAspects(result.getAspects());
        response.setProperties(result.getProperties());

        return response;
    }

    @GetMapping(path = "/download")
    public ResponseEntity<ByteArrayResource> uploadFile(@RequestParam(value = "file") final String file) {
        final byte[] data = storageService.getFile("/directory", file);
        final ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity
                .ok()
                .contentLength(data.length)
                .header("Content-type", "application/octet-stream")
                .header("Content-disposition", "attachment; filename=\"" + file + "\"")
                .body(resource);

    }

}
