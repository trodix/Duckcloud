package com.trodix.documentstorage;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.model.NodeTreeElement;
import com.trodix.documentstorage.service.TreeService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DirectoryServiceTests {

    TreeService directoryService = new TreeService();

    @Test
    public void buildTreeTest() throws JsonProcessingException {

        final List<NodeRepresentation> listNode = new ArrayList<>();

        final NodeRepresentation rootNode = new NodeRepresentation();
        rootNode.setDirectoryPath("/fruits");
        listNode.add(rootNode);

        final NodeRepresentation nodeChild1 = new NodeRepresentation();
        nodeChild1.setDirectoryPath("/fruits/a1");
        listNode.add(nodeChild1);

        final NodeRepresentation nodeChild2 = new NodeRepresentation();
        nodeChild2.setDirectoryPath("/fruits/a2");
        listNode.add(nodeChild2);

        final NodeRepresentation nodeChild3 = new NodeRepresentation();
        nodeChild3.setDirectoryPath("/fruits/a2/b1");
        listNode.add(nodeChild3);

        final NodeRepresentation nodeChild4 = new NodeRepresentation();
        nodeChild4.setDirectoryPath("/fruits/a2/b2");
        listNode.add(nodeChild4);

        final NodeRepresentation nodeChild5 = new NodeRepresentation();
        nodeChild5.setDirectoryPath("/fruits/a2/b2/toto.pdf");
        listNode.add(nodeChild5);


        final List<NodeTreeElement> xnodeList = directoryService.buildTree(listNode, false);
        log.info("\n" + directoryService.printAsJson(xnodeList));
    }

}
