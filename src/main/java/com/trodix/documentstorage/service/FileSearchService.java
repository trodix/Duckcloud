package com.trodix.documentstorage.service;

import com.trodix.documentstorage.exceptions.ParsingContentException;
import com.trodix.documentstorage.model.NodeRepresentation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.*;

@Service
@AllArgsConstructor
@Slf4j
public class FileSearchService {

    private final NodeService nodeService;

    private final StorageService storageService;

    public boolean searchInFile(String nodeUuid, Serializable value) {

        log.debug("Searching for the term '{}' in {}", value, nodeUuid);

        NodeRepresentation node = nodeService.findByNodeId(nodeUuid);
        String fileUuid = nodeService.findFileContentUuid(nodeUuid);
        byte[] content = storageService.getFile(node.getDirectoryPath(), fileUuid);

        if (content == null) {
            throw new IllegalStateException("File not found for nodeId " + nodeUuid);
        }

        log.debug("file content found: byte size: {}", content.length);
        String fileContent = extractFileContent(content);

        return fileContent.toLowerCase().contains(value.toString().toLowerCase());
    }

    public String extractFileContent(byte[] content) throws ParsingContentException {

        InputStream stream = new ByteArrayInputStream(content);
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();

        try {
            parser.parse(stream, handler, metadata);
            String handlerContent = handler.toString();
            return handlerContent;
        }  catch (IOException | SAXException | TikaException e){
            throw new ParsingContentException("Error while parsing file content", e);
        }

    }

}
