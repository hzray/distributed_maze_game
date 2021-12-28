import java.io.Serializable;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerResponse implements Serializable {

    private CopyOnWriteArrayList<PlayInfo> players;
    private GameState state;

    public ServerResponse(CopyOnWriteArrayList<PlayInfo> players, GameState state) {
        this.players = players;
        this.state = state;
    }

    public CopyOnWriteArrayList<PlayInfo> getPlayers() {
        return players;
    }

    public GameState getState() {
        return state;
    }
}
