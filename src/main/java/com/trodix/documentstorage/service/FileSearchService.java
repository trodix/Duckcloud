package com.trodix.documentstorage.service;

import com.trodix.documentstorage.exceptions.ParsingContentException;
import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.persistance.entity.Node;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

@Service
@AllArgsConstructor
@Slf4j
public class FileSearchService {

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
