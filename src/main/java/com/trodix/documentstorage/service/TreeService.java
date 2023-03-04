package com.trodix.documentstorage.service;

import java.util.List;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.trodix.documentstorage.model.NodeRepresentation;
import com.trodix.documentstorage.model.NodeTreeElement;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class TreeService {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<NodeTreeElement> buildTree(final List<NodeRepresentation> nodeRepList, final boolean withRecursiveParent) {
        final List<NodeTreeElement> tree = nodeRepList.stream().map(node -> {
            final NodeTreeElement xnode = new NodeTreeElement();
            xnode.setParent(getDirectParent(nodeRepList, node, withRecursiveParent));
            xnode.setIdentifier(node.getDirectoryPath());
            xnode.setValue(node);
            return xnode;
        }).sorted((a, b) -> a.getIdentifier().compareToIgnoreCase(b.getIdentifier())).toList();

        try {
            log.debug("\n" + printAsJson(tree));
        } catch (final JsonProcessingException e) {
            log.error("Error while rendering tree as json", e);
        }

        return tree;
    }

    public String printAsJson(final Object root) throws JsonProcessingException {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(root);
    }

    public NodeTreeElement getDirectParent(final List<NodeRepresentation> elementsList, final NodeRepresentation item, final boolean withRecursiveParent) {

        for (final NodeRepresentation e : elementsList) {
            if (isDirectParent(e, item)) {
                final NodeTreeElement xnode = new NodeTreeElement();
                xnode.setIdentifier(e.getDirectoryPath());
                if (withRecursiveParent) {
                    xnode.setParent(getDirectParent(elementsList, e, withRecursiveParent));
                }
                xnode.setValue(e);
                return xnode;
            }
        }

        return null;
    }

    public boolean isParent(final NodeRepresentation node, final NodeRepresentation node2) {
        return node2.getDirectoryPath().contains(node.getDirectoryPath());
    }

    public boolean isDirectParent(final NodeRepresentation node, final NodeRepresentation node2) {
        return isParent(node, node2) && node.getDirectoryPath().split("/").length + 1 == node2.getDirectoryPath().split("/").length;
    }

}
