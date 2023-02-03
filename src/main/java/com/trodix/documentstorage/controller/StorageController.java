package com.trodix.documentstorage.controller;

import java.io.IOException;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.trodix.documentstorage.mapper.NodeMapper;
import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.model.NodeRepresentationRequest;
import com.trodix.documentstorage.model.NodeRepresentationResponse;
import com.trodix.documentstorage.model.SearchQuery;
import com.trodix.documentstorage.model.SearchResult;
import com.trodix.documentstorage.persistance.entity.Node;
import com.trodix.documentstorage.persistance.entity.NodeIndex;
import com.trodix.documentstorage.service.NodeService;
import com.trodix.documentstorage.service.SearchService;
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

    private final SearchService searchService;

    private final NodeMapper nodeMapper;

    @GetMapping(path = "/buckets")
    public List<Bucket> listBuckets() {
        return storageService.listBuckets();
    }

    @PostMapping(path = "/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public NodeRepresentationResponse uploadFile(@RequestPart(value = "file", required = false) final MultipartFile file, final NodeRepresentationRequest node)
            throws IOException {

        final NodeRepresentation createNodeData = nodeMapper.nodeRepresentationRequestToNodeRepresentation(node, file);
        final NodeRepresentation result = nodeService.persistNode(createNodeData, file.getBytes());

        return nodeMapper.nodeRepresentationToNodeResponse(result);
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

    @GetMapping("/search")
    public SearchResult<NodeRepresentationResponse> searchNodes(@RequestBody final SearchQuery searchRequest,
            @RequestParam(defaultValue = "0") final Integer limit) {
        final List<NodeIndex> result = searchService.findNodeByFieldContaining(searchRequest.getTerm(), searchRequest.getValue(), limit);
        final List<Node> nodeResult = result.stream().map(nodeMapper::nodeIndexToNode).toList();
        final List<NodeRepresentation> nodeRepresentationResult = nodeResult.stream().map(nodeService::nodeToNodeRepresentation).toList();
        final List<NodeRepresentationResponse> nodeRepresentationResponseResult =
                nodeRepresentationResult.stream().map(nodeMapper::nodeRepresentationToNodeResponse).toList();
        return new SearchResult<>(nodeRepresentationResponseResult.size(), nodeRepresentationResponseResult);
    }

}
