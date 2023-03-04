package com.trodix.documentstorage.model;

import lombok.Data;

@Data
public class NodeTreeElement {

    private String identifier;

    private NodeTreeElement parent;

    private Object value;

    @Override
    public String toString() {
        return this.getIdentifier();
    }

}
