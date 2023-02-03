package com.trodix.documentstorage.model;

import java.io.Serializable;
import lombok.Data;

@Data
public class SearchQuery {

    private String term;

    private Serializable value;

}
