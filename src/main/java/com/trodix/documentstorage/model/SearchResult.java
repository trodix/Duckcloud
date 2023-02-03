package com.trodix.documentstorage.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SearchResult<T> {

    private final Integer resultCount;

    private final List<T> items;

}
