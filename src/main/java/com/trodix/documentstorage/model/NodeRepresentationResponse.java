package com.trodix.documentstorage.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class NodeRepresentationResponse {

    private String uuid;

    private String fileName;

    private String bucket;

    private String directoryPath;

    private List<String> aspects;

    private Map<String, Serializable> properties;

}
