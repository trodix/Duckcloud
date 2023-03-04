package com.trodix.documentstorage.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.trodix.documentstorage.mapper.NodeMapper;
import com.trodix.documentstorage.model.ContentModel;
import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.model.NodeRepresentationRequest;
import com.trodix.documentstorage.model.NodeRepresentationResponse;
import com.trodix.documentstorage.model.NodeTreeElement;
import com.trodix.documentstorage.model.SearchQuery;
import com.trodix.documentstorage.model.SearchResult;
import com.trodix.documentstorage.persistance.entity.Node;
import com.trodix.documentstorage.persistance.entity.NodeIndex;
import com.trodix.documentstorage.service.NodeService;
import com.trodix.documentstorage.service.PropertyService;
import com.trodix.documentstorage.service.SearchService;
import com.trodix.documentstorage.service.StorageService;
import com.trodix.documentstorage.service.TreeService;
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

    private final TreeService directoryService;

    private final PropertyService propertyService;

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

    @GetMapping("/search")
    public SearchResult<NodeTreeElement> searchNodes(@RequestBody final SearchQuery searchRequest,
            @RequestParam(defaultValue = "0") final Integer limit) {
        final List<NodeIndex> result = searchService.findNodeByFieldContaining(searchRequest.getTerm(), searchRequest.getValue(), limit);
        final List<Node> nodeResult = result.stream().map(nodeMapper::nodeIndexToNode).toList();
        final List<NodeRepresentation> nodeRepresentationResult = nodeResult.stream().map(nodeService::nodeToNodeRepresentation).toList();
        final List<NodeTreeElement> tree = directoryService.buildTree(nodeRepresentationResult, false);
        return new SearchResult<>(tree.size(), tree);
    }

    @GetMapping("/node")
    public List<NodeRepresentationResponse> getByPath(@RequestParam final String path) {
        final List<NodeRepresentation> result = nodeService.findByPath(path);
        return result.stream().map(nodeMapper::nodeRepresentationToNodeResponse).toList();
    }

    @GetMapping("/node/{nodeId}")
    public NodeRepresentationResponse getNodeById(@PathVariable final String nodeId) {
        final NodeRepresentation result = nodeService.findByNodeId(nodeId);
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found for id " + nodeId);
        }
        return nodeMapper.nodeRepresentationToNodeResponse(result);
    }

    @GetMapping("/node/{nodeId}/content")
    public ResponseEntity<ByteArrayResource> getNodeContentById(@PathVariable final String nodeId) {
        final NodeRepresentation node = nodeService.findByNodeId(nodeId);

        if (node == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found for nodeId " + nodeId);
        }

        final byte[] data = storageService.getFile(node.getDirectoryPath(), node.getUuid());
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
