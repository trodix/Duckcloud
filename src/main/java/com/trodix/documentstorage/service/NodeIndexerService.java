package com.trodix.documentstorage.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
import com.trodix.documentstorage.mapper.NodeMapper;
import com.trodix.documentstorage.persistance.entity.Node;
import com.trodix.documentstorage.persistance.entity.NodeIndex;
import com.trodix.documentstorage.persistance.repository.NodeRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class NodeIndexerService {

    private static final String PRODUCT_INDEX = "node";

    private final NodeRepository nodeRepository;

    private final NodeMapper nodeMapper;

    private final ElasticsearchOperations elasticsearchOperations;


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

            final List<NodeIndex> nodeIndexList = page.getContent().stream().map(nodeMapper::nodeToNodeIndex).toList();

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

        final List<IndexedObjectInformation> result = elasticsearchOperations.bulkIndex(queries, IndexCoordinates.of(PRODUCT_INDEX));
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
                        IndexCoordinates.of(PRODUCT_INDEX));

        return documentId;
    }

    public void deleteNodeIndex(final NodeIndex node) {

        final IndexQuery indexQuery = new IndexQueryBuilder()
                .withId(node.getDbId().toString())
                .withObject(node).build();

        elasticsearchOperations.delete(indexQuery, IndexCoordinates.of(PRODUCT_INDEX));
    }

}
