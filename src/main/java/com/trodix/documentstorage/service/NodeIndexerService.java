package com.trodix.documentstorage.service;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.trodix.documentstorage.exceptions.ParsingContentException;
import com.trodix.documentstorage.persistance.entity.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.trodix.documentstorage.persistance.entity.Node;
import com.trodix.documentstorage.persistance.entity.NodeIndex;
import com.trodix.documentstorage.persistance.repository.NodeRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class NodeIndexerService {

    private static final String NODE_INDEX = "node";

    private final NodeRepository nodeRepository;

    private final ElasticsearchOperations elasticsearchOperations;

    private final TypeService typeService;

    private final AspectService aspectService;

    private final PropertyService propertyService;

    private final QNameService qnameService;

    private final NodeService nodeService;


    @Async
    @Scheduled(fixedDelayString = "${app.indexes.synchronization.fixed-delay}", timeUnit = TimeUnit.MINUTES)
    public List<Future<?>> synchronizeIndexes() {
        log.info("Starting to synchronize indexes");

        final int MAX_THREAD_NUMBER = 4;
        final int BATCH_SIZE = 100;
        final int count = (int) nodeRepository.count();
        final int pageCount = (count > 0 && count >= BATCH_SIZE)
                ? (count / BATCH_SIZE)
                : 1;

        final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREAD_NUMBER);
        final List<Future<?>> runningTasks = new ArrayList<>();

        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            final Pageable pageable = PageRequest.of(pageIndex, 100);
            final Page<Node> page = nodeRepository.findAll(pageable);

            final List<NodeIndex> nodeIndexList = page.getContent().stream().map(nodeService::nodeToNodeIndex).toList();

            final Runnable task = () -> createNodeIndexBulk(nodeIndexList);
            log.debug("Adding new task to thread pool with page {}/{} ({} records)", pageIndex + 1, pageCount, count);
            final Future<?> pendingTask = executor.submit(task);
            runningTasks.add(pendingTask);
        }

        return runningTasks;
    }

    public List<IndexedObjectInformation> createNodeIndexBulk(final List<NodeIndex> nodes) {

        final List<IndexQuery> queries = nodes.stream()
                .map(node -> new IndexQueryBuilder()
                        .withId(node.getDbId().toString())
                        .withObject(node).build())
                .collect(Collectors.toList());

        log.debug("Running Bulk index query for {} items", nodes.size());

        final List<IndexedObjectInformation> result = elasticsearchOperations.bulkIndex(queries, IndexCoordinates.of(NODE_INDEX));
        log.debug("{} items indexed from bulk query", result.size());

        return result;
    }

    public String createNodeIndex(final NodeIndex node) {

        final IndexQuery indexQuery = new IndexQueryBuilder()
                .withId(node.getDbId().toString())
                .withObject(node).build();

        final String documentId = elasticsearchOperations
                .index(
                        indexQuery,
                        IndexCoordinates.of(NODE_INDEX));

        log.debug("New index created for nodeId {}", node.getUuid());

        return documentId;
    }

    public void deleteNodeIndex(final NodeIndex node) {

        final IndexQuery indexQuery = new IndexQueryBuilder()
                .withId(node.getDbId().toString())
                .withObject(node).build();

        elasticsearchOperations.delete(indexQuery, IndexCoordinates.of(NODE_INDEX));
    }

    public Node nodeIndexToNode(final NodeIndex nodeIndex) {
        final Node node = new Node();

        node.setDbId(nodeIndex.getDbId());
        node.setUuid(nodeIndex.getUuid());
        node.setDirectoryPath(nodeIndex.getDirectoryPath());
        node.setBucket(nodeIndex.getBucket());
        node.setType(typeService.stringToType(nodeIndex.getType()));
        node.setAspects(nodeIndex.getAspects().stream().map(aspectService::stringToAspect).toList());
        node.setProperties(new ArrayList<>());

        if (nodeIndex.getProperties() != null) {
            nodeIndex.getProperties().forEach((k, v) -> {
                try {
                    final Property p = propertyService.createProperty(qnameService.stringToQName(k), v);
                    node.getProperties().add(p);

                } catch (final ParseException e) {
                    log.error(e.getMessage(), e);
                }

            });
        }

        return node;
    }

}
