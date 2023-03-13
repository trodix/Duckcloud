package com.trodix.documentstorage.persistance.entity;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.trodix.documentstorage.model.ContentModel;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.MultiField;
import lombok.Data;

@Data
@Document(indexName = "node")
public class NodeIndex {

    @Id
    private Long dbId;

    @Field(type = FieldType.Text, name = "uuid")
    private String uuid;

    @Field(type = FieldType.Text, name = "bucket")
    private String bucket;

    @Field(type = FieldType.Text, name = "directoryPath")
    private String directoryPath;

    private String type;

    @MultiField(mainField = @Field(type = FieldType.Text))
    private List<String> aspects;

    @MultiField(mainField = @Field(type = FieldType.Flattened))
    private Map<String, Serializable> properties;

    @Field(type = FieldType.Text, name = "cm:content")
    private String filecontent;

}
