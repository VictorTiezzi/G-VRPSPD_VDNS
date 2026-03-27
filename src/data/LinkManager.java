package data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LinkManager {

    private final int numberOfNodes;
    private final Link[][] linkMatrix;

    public LinkManager(int numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
        this.linkMatrix = new Link[numberOfNodes][numberOfNodes];
    }

    public LinkManager(int numberOfNodes, Set<Link> links) {
        this.numberOfNodes = numberOfNodes;
        this.linkMatrix = new Link[numberOfNodes][numberOfNodes];
        for (Link link : links) {
            linkMatrix[link.origin().id()][link.destiny().id()] = link;
        }
    }

    public void set(Link link) {
        linkMatrix[link.origin().id()][link.destiny().id()] = link;
    }

    public void setAll(Set<Link> links) {
        for (Link link : links)
            linkMatrix[link.origin().id()][link.destiny().id()] = link;
    }

    public Link get(int originId, int destinyId) {
        return linkMatrix[originId][destinyId];
    }

    public List<Link> getAll() {
        List<Link> allLinks = new ArrayList<>();
        for (Link[] row : linkMatrix) {
            for (Link link : row) {
                if (link != null)
                    allLinks.add(link);
            }
        }
        return allLinks;
    }

    public List<Link> getAllInbound(int nodeIndex) {
        List<Link> incomingLinks = new ArrayList<>();
        for (int i = 0; i < numberOfNodes; i++) {
            Link link = get(i, nodeIndex);
            if (link != null)
                incomingLinks.add(link);
        }
        return incomingLinks;
    }

    public List<Link> getAllOutbound(int nodeIndex) {
        List<Link> outgoingLinks = new ArrayList<>();
        for (int j = 0; j < numberOfNodes; j++) {
            Link link = get(nodeIndex, j);
            if (link != null)
                outgoingLinks.add(link);
        }
        return outgoingLinks;
    }

}