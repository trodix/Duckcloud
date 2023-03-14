package com.trodix.documentstorage.response;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class NodeRepresentationResponse {

    private String uuid;

    private String bucket;

    private String directoryPath;

    private int versions;

    private String type;

    private List<String> aspects;

    private Map<String, Serializable> properties;

}
