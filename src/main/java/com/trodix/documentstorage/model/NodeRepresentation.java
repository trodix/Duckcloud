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

    private String type;

    private List<String> aspects;

    private Map<String, Serializable> properties;

    @Override
    public String toString() {
        return directoryPath;
    }


    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NodeRepresentation)) {
            return false;
        }
        final NodeRepresentation nodeRepresentation = (NodeRepresentation) o;
        return Objects.equals(uuid, nodeRepresentation.uuid) && Objects.equals(bucket, nodeRepresentation.bucket)
                && Objects.equals(directoryPath, nodeRepresentation.directoryPath) && Objects.equals(contentType, nodeRepresentation.contentType)
                && Objects.equals(type, nodeRepresentation.type) && Objects.equals(aspects, nodeRepresentation.aspects)
                && Objects.equals(properties, nodeRepresentation.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, bucket, directoryPath, contentType, type, aspects, properties);
    }


}
