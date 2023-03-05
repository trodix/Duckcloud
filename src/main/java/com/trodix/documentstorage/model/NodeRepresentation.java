package com.trodix.documentstorage.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Data;

@Data
public class NodeRepresentation {

    private String uuid;

    private String bucket;

    private String directoryPath;

    private String contentType;

    private int versions;

    private String type;

    private List<String> aspects;

    private Map<String, Serializable> properties;

    @Override
    public String toString() {
        return directoryPath;
    }

}
