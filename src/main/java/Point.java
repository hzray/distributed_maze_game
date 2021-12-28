
import java.io.Serializable;

/**
 * Point represent one unit area of the maze
 */
public class Point implements Serializable {
    private boolean hasTreasure;
    private boolean occupied;
    private PlayerGameData occupier;

    public Point(boolean hasTreasure, boolean occupied) {
        this.hasTreasure = hasTreasure;
        this.occupied = occupied;
    }

    public void removeOccupier() {
        occupier = null;
        occupied = false;
    }

    public void setOccupier(PlayerGameData player) {
        occupier = player;
        occupied = true;
    }


    public boolean isHasTreasure() {
        return hasTreasure;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void removeTreasure() {
        this.hasTreasure = false;
    }

    public void addTreasure() {
        this.hasTreasure = true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(hasTreasure) {
            sb.append("* \n");
        }
        if (occupied) {
            sb.append(occupier.getId());
        }
        return sb.toString();
    }
}


