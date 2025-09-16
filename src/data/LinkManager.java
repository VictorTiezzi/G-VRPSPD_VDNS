package data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LinkManager {

    private final Link[][] linkMatrix;

    public LinkManager() {
        this.linkMatrix = new Link[Data.numberOfNodes][Data.numberOfNodes];
    }

    public LinkManager(Set<Link> links) {
        this.linkMatrix = new Link[Data.numberOfNodes][Data.numberOfNodes];
        for (Link link : links) {
            linkMatrix[link.origin.id()][link.destiny.id()] = link;
        }
    }

    public void set(Link link) {
        linkMatrix[link.origin.id()][link.destiny.id()] = link;
    }

    public void setAll(Set<Link> links) {
        for (Link link : links)
            linkMatrix[link.origin.id()][link.destiny.id()] = link;
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

    public List<Link> getAllIncomingLinks(int nodeIndex) {
        List<Link> incomingLinks = new ArrayList<>();
        for (int i = 0; i < Data.allNodes.size(); i++) {
            Link link = get(i, nodeIndex);
            if (link != null)
                incomingLinks.add(link);
        }
        return incomingLinks;
    }

    public List<Link> getAllOutgoingLinks(int nodeIndex) {
        List<Link> outgoingLinks = new ArrayList<>();
        for (int j = 0; j < Data.allNodes.size(); j++) {
            Link link = get(nodeIndex, j);
            if (link != null)
                outgoingLinks.add(link);
        }
        return outgoingLinks;
    }

}