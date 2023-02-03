package com.trodix.documentstorage.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import com.trodix.documentstorage.persistance.entity.NodeIndex;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SearchService {

    private static final String NODE_INDEX = "node";

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public List<NodeIndex> findNodeByFieldContaining(final String field, final Serializable value, final Integer limit) {
        log.debug("Search with query [{}={}] and limit [{}]", field, value, limit);

        // 1. Create query with term conditions
        final QueryBuilder queryBuilder =
                QueryBuilders
                        .boolQuery()
                        .must(QueryBuilders.matchQuery(field, value))
                        .must(QueryBuilders.matchQuery("uuid", "dafbe1a2-c9f0-4067-922c-5f77fbf8de6d"));

        final NativeSearchQueryBuilder nativeBuilder = new NativeSearchQueryBuilder();
        nativeBuilder.withFilter(queryBuilder);

        if (limit > 0) {
            nativeBuilder.withMaxResults(limit);
        }

        final Query searchQuery = nativeBuilder.build();

        // 2. Execute search
        final SearchHits<NodeIndex> indexHits =
                elasticsearchOperations
                        .search(searchQuery, NodeIndex.class,
                                IndexCoordinates.of(NODE_INDEX));

        // 3. Map searchHits to index list
        final List<NodeIndex> indexMatches = new ArrayList<>();
        indexHits.forEach(searchHit -> indexMatches.add(searchHit.getContent()));

        log.debug("Results found: {}", indexMatches.size());

        return indexMatches;
    }

}
