package com.trodix.documentstorage.controller;

import com.trodix.documentstorage.mapper.NodeMapper;
import com.trodix.documentstorage.model.*;
import com.trodix.documentstorage.persistance.entity.Node;
import com.trodix.documentstorage.persistance.entity.NodeIndex;
import com.trodix.documentstorage.service.NodeIndexerService;
import com.trodix.documentstorage.service.NodeService;
import com.trodix.documentstorage.service.SearchService;
import com.trodix.documentstorage.service.TreeService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@AllArgsConstructor
@Slf4j
public class SearchController {

    private final NodeService nodeService;

    private final SearchService searchService;

    private final TreeService directoryService;

    private final NodeMapper nodeMapper;

    private final NodeIndexerService nodeIndexerService;


    @Operation(summary = "Search indexed nodes by metadata (elasticsearch)")
    @PostMapping("/search")
    public SearchResult<NodeTreeElement> searchNodes(@RequestBody final SearchQuery searchRequest, @RequestParam(defaultValue = "0") final Integer limit) {
        final List<NodeIndex> result = searchService.findNodeByFieldContaining(searchRequest.getTerm(), searchRequest.getValue(), limit);
        final List<Node> nodeResult = result.stream().map(nodeIndexerService::nodeIndexToNode).toList();
        final List<NodeRepresentation> nodeRepresentationResult = nodeResult.stream().map(nodeService::nodeToNodeRepresentation).toList();
        final List<NodeTreeElement> tree = directoryService.buildTree(nodeRepresentationResult, false);
        return new SearchResult<>(tree.size(), tree);
    }

    @Operation(summary = "Find nodes located at the path")
    @GetMapping("/node")
    public List<NodeRepresentationResponse> getByPath(@RequestParam final String path) {
        final List<NodeRepresentation> result = nodeService.findByPath(path);
        return result.stream().map(nodeMapper::nodeRepresentationToNodeResponse).toList();
    }

    @Operation(summary = "Find a node by its UUID")
    @GetMapping("/node/{nodeId}")
    public NodeRepresentationResponse getNodeById(@PathVariable final String nodeId) {
        final NodeRepresentation result = nodeService.findByNodeId(nodeId);
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found for id " + nodeId);
        }
        return nodeMapper.nodeRepresentationToNodeResponse(result);
    }

}
