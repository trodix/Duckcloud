package com.trodix.documentstorage.model;

public class ContentModel {

    public static final String NAMESPACE = "cm";
    public static final String TYPE_CONTENT = getQNameString("content");

    public static final String TYPE_DIRECTORY = getQNameString("directory");

    public static final String PROP_CREATOR = getQNameString("creator");

    public static final String PROP_CREATOR_NAME = getQNameString("creator-name");

    public static final String PROP_CREATED_AT = getQNameString("created-at");

    public static final String PROP_MODIFIED_BY = getQNameString("modified-by");

    public static final String PROP_MODIFIED_AT = getQNameString("modified-at");

    public static final String PROP_NAME = getQNameString("name");

    private ContentModel() {
        // no-op
    }

    private static String getQNameString(String name) {
        return NAMESPACE + ":" + name;
    }

}
