import java.io.Serializable;

public class PlayInfo implements Serializable {
    private final Player player;
    private final String id;

    public PlayInfo(Player player, String id) {
        this.player = player;
        this.id = id;
    }

    public Player getPlayer() {
        return player;
    }

    public String getId() {
        return id;
    }
}









