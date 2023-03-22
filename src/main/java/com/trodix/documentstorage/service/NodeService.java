package com.trodix.documentstorage.service;

import com.trodix.documentstorage.exceptions.ParsingContentException;
import com.trodix.documentstorage.model.ContentModel;
import com.trodix.documentstorage.model.FileStoreMetadata;
import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.persistance.dao.NodeDAO;
import com.trodix.documentstorage.persistance.dao.StoredFileDAO;
import com.trodix.documentstorage.persistance.entity.*;
import com.trodix.documentstorage.request.NodeUpdateRequest;
import com.trodix.documentstorage.security.services.AuthenticationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@AllArgsConstructor
@Slf4j
public class NodeService {

    private final StorageService storageService;

    private final NodeDAO nodeDAO;

    private final QNameService qnameService;

    private final TypeService typeService;

    private final AspectService aspectService;

    private final PropertyService propertyService;

    private final StoredFileDAO storedFileDAO;

    private final FileSearchService fileSearchService;

    private final AuthenticationService authService;


    public Node nodeRepresentationToNode(final NodeRepresentation nodeRepresentation) throws IllegalArgumentException {

        final Type type = typeService.stringToType(nodeRepresentation.getType());

        final List<Aspect> aspects = new ArrayList<>();

        if (nodeRepresentation.getAspects() == null) {
            nodeRepresentation.setAspects(new ArrayList<>());
        }

        nodeRepresentation.getAspects().forEach(aspectString -> {
            final Aspect aspect = aspectService.stringToAspect(aspectString);
            aspects.add(aspect);
        });

        final List<Property> properties = new ArrayList<>();

        nodeRepresentation.getProperties().forEach((key, value) -> {
            Property property;
            try {
                property = propertyService.createProperty(qnameService.stringToQName(key), value);
            } catch (final ParseException e) {
                throw new IllegalArgumentException(e);
            }

            properties.add(property);
        });

        final Node node = new Node();
        node.setBucket(nodeRepresentation.getBucket());
        node.setDirectoryPath(nodeRepresentation.getDirectoryPath());
        node.setUuid(StringUtils.isBlank(nodeRepresentation.getUuid()) ? UUID.randomUUID().toString() : nodeRepresentation.getUuid());
        node.setVersions(nodeRepresentation.getVersions());
        node.setType(type);
        node.setAspects(aspects);
        node.setProperties(properties);

        return node;
    }

    public NodeRepresentation nodeToNodeRepresentation(final Node node) {

        final List<String> aspects = new ArrayList<>();
        node.getAspects().forEach(aspect -> aspects.add(aspectService.aspectToString(aspect)));

        final Map<String, Serializable> properties = new HashMap<>();
        node.getProperties().forEach(property -> {
            final String propertyName = qnameService.qnameToString(property.getQname());
            final Serializable propertyValue = propertyService.getPropertyValue(property);

            properties.put(propertyName, propertyValue);
        });

        final NodeRepresentation nodeRepresentation = new NodeRepresentation();
        nodeRepresentation.setBucket(node.getBucket());
        nodeRepresentation.setDirectoryPath(node.getDirectoryPath());
        nodeRepresentation.setUuid(node.getUuid());
        nodeRepresentation.setVersions(node.getVersions());
        nodeRepresentation.setType(typeService.typeToString(node.getType()));
        nodeRepresentation.setAspects(aspects);
        nodeRepresentation.setProperties(properties);

        return nodeRepresentation;
    }

    public  Node updateNode(final NodeRepresentation nodeRep) throws IllegalArgumentException {

        Node existingNode = nodeDAO.findByUuId(nodeRep.getUuid());
        Node node = nodeRepresentationToNode(nodeRep);
        node.setDbId(existingNode.getDbId());
        addProperty(node, ContentModel.PROP_MODIFIED_BY_ID, authService.getUserId());
        addProperty(node, ContentModel.PROP_MODIFIED_BY_NAME, authService.getName());
        addProperty(node, ContentModel.PROP_MODIFIED_AT, OffsetDateTime.now());

        if (node == null) {
            throw new IllegalArgumentException("Node not found for id " + nodeRep.getUuid());
        }

        return nodeDAO.update(node);
    }

    @Transactional
    public NodeRepresentation persistNode(final NodeRepresentation nodeRep, @Nullable final byte[] file) {

        Node node = nodeRepresentationToNode(nodeRep);

        // TODO support multiple buckets
        node.setBucket(StorageService.ROOT_BUCKET);

        final Map<String, Serializable> properties = new HashMap<>();

        // Set ownership
        final String userId = authService.getUserId();
        log.debug("Set creator userId {} for node {}", userId, node.getUuid());
        properties.put(ContentModel.PROP_CREATOR, userId);

        final String userName = authService.getName();
        log.debug("Set creator name {} for node {}", userName, node.getUuid());
        properties.put(ContentModel.PROP_CREATOR_NAME, userName);

        // set created at
        properties.put(ContentModel.PROP_CREATED_AT, OffsetDateTime.now());

        properties.put(ContentModel.PROP_NAME, nodeRep.getProperties().get(ContentModel.PROP_NAME));

        addProperties(node, properties);

        if (isTypeContent(node) && file != null) {
            // get directories or create them (recursively)
            String tmpSegmentBuild = "";
            boolean skipCheckExist = false;
            for (String segment : nodeRep.getDirectoryPath().split("/")) {

                if (segment.length() == 0) {
                    continue;
                }

                tmpSegmentBuild += "/" + segment;

                boolean dirExists = skipCheckExist ? false : nodeDAO.isDirectoryAtPathExists(tmpSegmentBuild);
                log.trace("Check path [{}] exists: {} - skipCheckExist: {}", tmpSegmentBuild, dirExists, skipCheckExist);

                if (dirExists) {
                    log.debug("Directory {} already exists, NOT recreating it again", tmpSegmentBuild);
                } else {
                    // create a directory
                    skipCheckExist = true;
                    String dirPath = tmpSegmentBuild.substring(0, tmpSegmentBuild.lastIndexOf("/"));
                    if (dirPath.length() == 0) {
                        dirPath = "/";
                    }
                    String dirName = tmpSegmentBuild.substring(tmpSegmentBuild.lastIndexOf("/") + 1, tmpSegmentBuild.length());
                    Map<String, Serializable> props = new HashMap<>();
                    props.put(ContentModel.PROP_NAME, dirName);

                    NodeRepresentation nodeRepDir = new NodeRepresentation();
                    nodeRepDir.setUuid(UUID.randomUUID().toString());
                    nodeRepDir.setDirectoryPath(dirPath);
                    nodeRepDir.setType(ContentModel.TYPE_DIRECTORY);
                    nodeRepDir.setVersions(1);
                    nodeRepDir.setAspects(Collections.emptyList());
                    nodeRepDir.setProperties(props);

                    persistNode(nodeRepDir, null);
                    log.debug("New directory created at {} (dirName={}) for storing node {} at path {}", dirPath, dirName, node.getUuid(), node.getDirectoryPath());
                }
            }
        }

        node = nodeDAO.save(node);

        if (isTypeContent(node) && file != null) {
            nodeRep.setUuid(node.getUuid());
            nodeRep.setBucket(node.getBucket());
            nodeRep.setType(typeService.typeToString(node.getType()));
            nodeRep.setVersions(node.getVersions());

            createContent(nodeRep, file);
        }

        return findByNodeId(node.getUuid());
    }

    public Node addProperties(Node node, Map<String, Serializable> properties) {
        for (Map.Entry<String, Serializable> property : properties.entrySet()) {
            addProperty(node, property.getKey(), property.getValue());
        }
        return node;
    }

    public Node addProperty(Node node, String propertyName, Serializable value) throws RuntimeException {
        try {
            final Property creatorProperty = propertyService.createProperty(qnameService.stringToQName(propertyName), value);
            node.getProperties().add(creatorProperty);
        } catch (Exception e) {
            throw new RuntimeException("Error while setting the property: " + propertyName + "=" + value, e);
        }
        return node;
    }

    public StoredFile createContent(final NodeRepresentation nodeRep, final byte[] file) throws IllegalArgumentException {

        final Node node = nodeDAO.findByUuId(nodeRep.getUuid());
        if (node == null) {
            throw new IllegalArgumentException("Node with uuid " + nodeRep.getUuid() + " was not found");
        } else if (!isTypeContent(node)) {
            throw new IllegalArgumentException("Node must be of type " + ContentModel.TYPE_CONTENT + ". Type found: " + typeService.typeToString(node.getType()));
        }

        // Validate the media type
        MediaType.valueOf(nodeRep.getContentType());

        FileStoreMetadata fileStoreMetadata = new FileStoreMetadata();
        fileStoreMetadata.setContentType(nodeRep.getContentType());
        fileStoreMetadata.setUuid(UUID.randomUUID().toString());
        fileStoreMetadata.setBucket(node.getBucket());
        fileStoreMetadata.setDirectoryPath(node.getDirectoryPath());

        storageService.uploadFile(fileStoreMetadata, file);
        log.debug("File uploaded: {}", fileStoreMetadata);

        StoredFile storedFile = new StoredFile();
        storedFile.setNode(node);
        storedFile.setUuid(fileStoreMetadata.getUuid());
        storedFile.setVersion(node.getVersions() + 1);

        storedFile = storedFileDAO.save(storedFile);
        node.setVersions(storedFile.getVersion());

//        addProperty(node, ContentModel.PROP_MODIFIED_BY_ID, authService.getUserId());
//        addProperty(node, ContentModel.PROP_MODIFIED_BY_NAME, authService.getName());
//        addProperty(node, ContentModel.PROP_MODIFIED_AT, OffsetDateTime.now());

        nodeDAO.save(node);

        log.debug("Version {} created for file uuid {} related to node uuid {}", storedFile.getVersion(), storedFile.getUuid(), node.getUuid());

        return storedFile;
    }

    public String getOriginalFileName(final Node node) {

        if (node.getProperties() != null) {
            final List<Property> res =
                    node.getProperties().stream().filter(i -> qnameService.qnameToString(i.getQname()).equals(ContentModel.PROP_NAME)).toList();

            if (!res.isEmpty()) {
                return res.get(0).getStringValue();
            }
        }

        throw new IllegalArgumentException("Node does not contain the property: " + ContentModel.PROP_NAME + ". Actual properties: " + node.getProperties());
    }

    public List<NodeRepresentation> findByPath(final String path) {
        return this.nodeDAO.findByPath(path).stream().map(this::nodeToNodeRepresentation).toList();
    }

    public NodeRepresentation findByNodeId(final String nodeId) {
        Node result = this.nodeDAO.findByUuId(nodeId);
        if (result != null) {
            return nodeToNodeRepresentation(result);
        }
        return null;
    }

    public String findFileContentUuid(final String nodeId) {
        int latestVersion = this.storedFileDAO.findStoredFileLatestVersion(nodeId);
        return findFileContentUuidForVersion(nodeId, latestVersion);
    }

    public String findFileContentUuidForVersion(final String nodeId, final int version) {
        String fileUuid = this.storedFileDAO.findStoredFile(nodeId, version);

        return fileUuid;
    }

    public List<String> findAllFileContentVersions(final String nodeId) {
        return this.storedFileDAO.findAllFileContentVersions(nodeId);
    }

    public boolean searchInFile(String nodeUuid, Serializable value) {

        log.debug("Searching for the term '{}' in {}", value, nodeUuid);

        NodeRepresentation node = findByNodeId(nodeUuid);
        String fileUuid = findFileContentUuid(nodeUuid);
        byte[] content = storageService.getFile(node.getDirectoryPath(), fileUuid);

        if (content == null) {
            throw new IllegalStateException("File not found for nodeId " + nodeUuid);
        }

        log.debug("file content found: byte size: {}", content.length);
        String fileContent = fileSearchService.extractFileContent(content);

        return fileContent.toLowerCase().contains(value.toString().toLowerCase());
    }

    public String extractFileContent(Node node, String fileUuid) throws ParsingContentException {

        if (!isTypeContent(node)) {
            throw new IllegalArgumentException("Node must be of type " + ContentModel.TYPE_CONTENT + " to extract file content");
        }

        if (fileUuid == null) {
            throw new IllegalStateException("File not found for nodeId " + node.getUuid());
        }

        byte[] file = storageService.getFile(node.getDirectoryPath(), fileUuid);

        return fileSearchService.extractFileContent(file);
    }

    public String extractFileContent(NodeRepresentation nodeRepresentation) throws ParsingContentException {
        String fileUuid = findFileContentUuid(nodeRepresentation.getUuid());
        byte[] file = storageService.getFile(nodeRepresentation.getDirectoryPath(), fileUuid);

        return fileSearchService.extractFileContent(file);
    }

    public boolean isTypeContent(final Node node) {
        return ContentModel.TYPE_CONTENT.equals(qnameService.qnameToString(node.getType().getQname()));
    }

    public boolean isTypeDirectory(final Node node) {
        return ContentModel.TYPE_DIRECTORY.equals(qnameService.qnameToString(node.getType().getQname()));
    }

    public NodeIndex nodeToNodeIndex(final Node node) {
        final NodeIndex nodeIndex = new NodeIndex();

        nodeIndex.setDbId(node.getDbId());
        nodeIndex.setUuid(node.getUuid());
        nodeIndex.setDirectoryPath(node.getDirectoryPath());
        nodeIndex.setBucket(node.getBucket());
        nodeIndex.setVersions(node.getVersions());
        nodeIndex.setType(typeService.typeToString(node.getType()));
        nodeIndex.setAspects(node.getAspects().stream().map(aspectService::aspectToString).toList());

        final Map<String, Serializable> properties = new HashMap<>();

        node.getProperties().forEach(p -> properties.put(qnameService.qnameToString(p.getQname()), propertyService.getPropertyValue(p)));

        if (isTypeContent(node)) {
            try {
                String fileUuid = findFileContentUuid(node.getUuid());
                if (fileUuid != null) {
                    nodeIndex.setFilecontent(extractFileContent(node, fileUuid));
                } else {
                    throw new IllegalStateException("File not found for nodeId " + node.getUuid());
                }
            } catch (ParsingContentException | IllegalStateException e) {
                log.error("Error while parsing file content. File content will not be indexed", e);
            }
        }

        nodeIndex.setProperties(properties);

        return nodeIndex;
    }

    public NodeRepresentation mergeNode(final NodeRepresentation oldNode, final NodeUpdateRequest newNode) {
        NodeRepresentation mergedNode = new NodeRepresentation();

        if (newNode.getNodeId() == null) {
            newNode.setNodeId(oldNode.getUuid());
        }
        mergedNode.setUuid(newNode.getNodeId());

        if (newNode.getBucket() == null) {
            newNode.setBucket(oldNode.getBucket());
        }
        mergedNode.setBucket(newNode.getBucket());

        if (newNode.getDirectoryPath() == null) {
            newNode.setDirectoryPath(oldNode.getDirectoryPath());
        }
        mergedNode.setDirectoryPath(newNode.getDirectoryPath());

        if (newNode.getType() != null && !newNode.getType().equals(oldNode.getType())) {
            throw new IllegalArgumentException("Node type can not be changed");
        } else if (newNode.getType() == null) {
            newNode.setType(oldNode.getType());
        }
        mergedNode.setType(newNode.getType());

        // merge aspects
        if (newNode.getAspects() == null) {
            newNode.setAspects(new ArrayList<>());
        }

        for (String aspect : newNode.getAspects()) {
            if (!newNode.getAspects().contains(aspect)) {
                newNode.getAspects().add(aspect);
            }
        }
        mergedNode.setAspects(newNode.getAspects());

        // merge properties
        if (newNode.getProperties() == null) {
            newNode.setProperties(new HashMap<>());
        }
        for (Map.Entry<String, Serializable> property : oldNode.getProperties().entrySet()) {
            newNode.getProperties().putIfAbsent(property.getKey(), property.getValue());
        }
        mergedNode.setProperties(newNode.getProperties());

        return mergedNode;
    }

    public NodeRepresentation nodeUpdateRequestToNodeRepresentation(NodeUpdateRequest nodeUpdateRequest) throws IllegalArgumentException {

        if (nodeUpdateRequest.getNodeId() == null) {
            throw new IllegalArgumentException("Node id must no be null");
        }

        NodeRepresentation existingNode = findByNodeId(nodeUpdateRequest.getNodeId());

        if (existingNode == null) {
            throw new IllegalArgumentException("Node not found for id " + nodeUpdateRequest.getNodeId());
        }

        NodeRepresentation mergedNode = mergeNode(existingNode, nodeUpdateRequest);

        return mergedNode;
    }

    public String getOwnerId(NodeRepresentation nodeRepresentation) {
        return (String) nodeRepresentation.getProperties().get(ContentModel.PROP_CREATOR);
    }

    public String getOwnerName(NodeRepresentation nodeRepresentation) {
        return (String) nodeRepresentation.getProperties().get(ContentModel.PROP_CREATOR_NAME);
    }

    public List<Node> findChildren(String nodeId) {
        Node node = nodeDAO.findByUuId(nodeId);
        return nodeDAO.findChildren(nodeId, getOriginalFileName(node));
    }

    public void deleteNode(Node node) {
        nodeDAO.delete(node);
    }

    public Path getFullPath(Node node) {
        return Paths.get(node.getDirectoryPath(), getOriginalFileName(node));
    }

}
