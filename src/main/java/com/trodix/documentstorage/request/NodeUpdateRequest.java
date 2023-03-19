package com.trodix.documentstorage.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class NodeUpdateRequest {

    private String nodeId;

    private String bucket;

    private String directoryPath;

    private String type;

    private List<String> aspects;

    private Map<String, Serializable> properties;

}
