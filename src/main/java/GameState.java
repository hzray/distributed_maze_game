
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * State maintain current information about the game
 */
public class GameState implements Serializable {
    private int N;
    private int K;
    private List<PlayerGameData> players;
    private Point[][] maze;
    private String pname;
    private String bname;





    public GameState(int n, int k, List<PlayerGameData> players) {
        N = n;
        K = k;
        this.players = players;
        maze = new Point[N][N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                maze[i][j] = new Point(false, false);
            }
        }
        for (int i = 0; i < K; i++) {
            makeRandomTreasure();
        }

    }

    public List<PlayerGameData> getPlayers() {
        return players;
    }

    public Point[][] getMaze() {
        return maze;
    }





    // Primary Server will call this method to make move
    // return updated playerData(position+score) if the move is valid and null otherwise
    // if the move is valid, update the state
    public void move(Direction direction, String id, int requestNo) {
        // find the player wants to make the move
        for (PlayerGameData playerData: players) {
            if (playerData.getId().equals(id)) {
                if (requestNo < playerData.getLastRequestHandled()) {
                    System.out.println("duplicate request from " + id);
                    return;
                }
                if (direction == Direction.STAY) {
                    playerData.finishOneRequest();
                    return;
                }
                Position currPosition = playerData.getPosition();
                Position newPosition = makeValidMove(direction, currPosition);
                if (newPosition != null) {
                    // if it is a valid move
                    int currX = currPosition.getX();
                    int currY = currPosition.getY();
                    maze[currX][currY].removeOccupier();
                    System.out.println("maze  = [" + currX + "," + currY + "] has been removed");

                    playerData.setPosition(newPosition);
                    int x = newPosition.getX();
                    int y = newPosition.getY();
                    maze[x][y].setOccupier(playerData);

                    if (maze[x][y].isHasTreasure()) {
                        playerData.incScore();
                        maze[x][y].removeTreasure();
                        makeRandomTreasure();
                    }
                    System.out.println("new pos for [" + id + " ] = " + x + ", " + y);
                    playerData.finishOneRequest();
                    return;
                }
                return;
            }
        }
    }

    // Check if a wanted move is valid, if valid return the new position
    // 1. the new position is within the grid range
    // 2. the new position is not occupied by another player
    private Position makeValidMove(Direction direction, Position position) {
        int x = position.getX();
        int y = position.getY();
        switch (direction) {
            case EAST:
                y++;
                break;
            case WEST:
                y--;
                break;
            case NORTH:
                x--;
                break;
            case SOUTH:
                x++;
                break;
        }
        if  (x >= 0 && x < N
                && y >= 0 && y < N
                && !maze[x][y].isOccupied()) {
            return new Position(x, y);
        }
        return null;
    }

    // make one random treasure on the maze
    public void makeRandomTreasure() {
        while (true) {
            int x = ThreadLocalRandom.current().nextInt(0, N);
            int y = ThreadLocalRandom.current().nextInt(0, N);
            if (!maze[x][y].isHasTreasure() && !maze[x][y].isOccupied()) {
                maze[x][y].addTreasure();
                return;
            }
        }
    }

    public void addPlayer(PlayerGameData player) {
        player.setPosition(generateInitPosition());
        players.add(player);
        int x = player.getPosition().getX();
        int y = player.getPosition().getY();
        maze[x][y].setOccupier(player);
    }

    public void removePlayer(String id) {
        int i;
        for (i=0; i<players.size(); i++) {
            if (players.get(i).getId().equals(id)) {
                break;
            }
        }
        PlayerGameData playerData = players.get(i);
        int x = playerData.getPosition().getX();
        int y = playerData.getPosition().getY();
        maze[x][y].removeOccupier();
        System.out.println("state - remove player at [" + x + ", " + y + "]");
        players.remove(i);
    }

    public void setPname(String pname) {
        this.pname = pname;
    }

    public void setBname(String bname) {
        this.bname = bname;
    }

    public String getPname() {
        return pname;
    }

    public String getBname() {
        return bname;
    }


    public Position generateInitPosition() {
        while (true) {
            int x = ThreadLocalRandom.current().nextInt(0, N);
            int y = ThreadLocalRandom.current().nextInt(0, N);
            if (maze[x][y].isOccupied() || maze[x][y].isHasTreasure()) {
                continue;
            }
            return new Position(x, y);
        }
    }
}
