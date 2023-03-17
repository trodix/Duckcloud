package com.trodix.documentstorage.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class DirectoryRepresentationRequest {

    private String directoryPath;

    private String bucket;

    private List<String> aspects;

    private Map<String, Serializable> properties;

}
