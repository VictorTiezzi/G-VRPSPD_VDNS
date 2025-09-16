package data;

import ilog.concert.IloIntVar;

import java.util.Objects;

/**
 * Represents a link between two nodes with a specified distance.
 */
public class Link {

    public final Node origin;
    public final Node destiny;
    public final double distance;

    public IloIntVar pathCplexVar;
    public IloIntVar deliveryCplexVar;
    public IloIntVar pickupCplexVar;

    public Link(Node origin, Node destiny, double distance) {
        this.origin = origin;
        this.destiny = destiny;
        this.distance = distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Link link = (Link) o;
        return Objects.equals(origin, link.origin) && Objects.equals(destiny, link.destiny);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, destiny);
    }

}