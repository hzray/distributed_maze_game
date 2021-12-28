
import java.io.Serializable;


/**
 * PlayerData maintain all information will be stored in State and show on the panel
 */
public class PlayerGameData implements Serializable {
    private String id;
    private Position position;
    private int score;

    private int lastRequestHandled;

    public void finishOneRequest() {
        lastRequestHandled++;
    }


    public int getLastRequestHandled() {
        return lastRequestHandled;
    }

    public PlayerGameData(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public void incScore() {
        score += 1;
    }



    @Override
    public String toString() {
        return "Player-" +
                "id='" + id + '\'' +
                ", score=" + score;
    }
}
