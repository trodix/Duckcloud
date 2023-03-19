package com.trodix.documentstorage.service;

import com.trodix.documentstorage.exceptions.ParsingContentException;
import com.trodix.documentstorage.model.ContentModel;
import com.trodix.documentstorage.model.FileStoreMetadata;
import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.persistance.dao.NodeDAO;
import com.trodix.documentstorage.persistance.dao.StoredFileDAO;
import com.trodix.documentstorage.persistance.entity.*;
import com.trodix.documentstorage.security.services.AuthenticationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public NodeRepresentation persistNode(final NodeRepresentation nodeRep, final byte[] file) {

        Node node = nodeRepresentationToNode(nodeRep);

        // TODO support multiple buckets
        node.setBucket(StorageService.ROOT_BUCKET);

        try {

            // Set ownership
            final String userId = authService.getUserId();
            log.debug("Set creator userId {} for node {}", userId, node.getUuid());
            final Property creatorProperty = propertyService.createProperty(qnameService.stringToQName(ContentModel.PROP_CREATOR), userId);
            node.getProperties().add(creatorProperty);

            final String userName = authService.getName();
            log.debug("Set creator name {} for node {}", userName, node.getUuid());
            final Property creatorNameProperty = propertyService.createProperty(qnameService.stringToQName(ContentModel.PROP_CREATOR_NAME), userName);
            node.getProperties().add(creatorNameProperty);

            // set created at
            final Property createdAtProperty = propertyService.createProperty(qnameService.stringToQName(ContentModel.PROP_CREATED_AT), OffsetDateTime.now());
            node.getProperties().add(createdAtProperty);

            final Property originalFileNameProperty =
                    propertyService.createProperty(qnameService.stringToQName(ContentModel.PROP_NAME), nodeRep.getProperties().get(ContentModel.PROP_NAME));

            node.getProperties().add(originalFileNameProperty);
        } catch (final ParseException e) {
            log.error(e.getMessage(), e);
        }

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
                    Map<String, Serializable> properties = new HashMap<>();
                    properties.put(ContentModel.PROP_NAME, dirName);

                    NodeRepresentation nodeRepDir = new NodeRepresentation();
                    nodeRepDir.setUuid(UUID.randomUUID().toString());
                    nodeRepDir.setDirectoryPath(dirPath);
                    nodeRepDir.setType(ContentModel.TYPE_DIRECTORY);
                    nodeRepDir.setVersions(1);
                    nodeRepDir.setAspects(Collections.emptyList());
                    nodeRepDir.setProperties(properties);

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
