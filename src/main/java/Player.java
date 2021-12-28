
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

public interface Player extends Remote {


    ServerResponse handleMoveRequest(Direction direction, PlayInfo playInfo, int requestNo) throws RemoteException;

    ServerResponse handleJoinRequest(PlayInfo playInfo, Role role) throws RemoteException;

    void setPlayerList(CopyOnWriteArrayList<PlayInfo> playerList) throws RemoteException;

    void setState(GameState state) throws RemoteException;

    void refreshGUI() throws RemoteException;

    void ping() throws RemoteException;

    Role getRole() throws RemoteException;

    void setRole(Role role, PlayInfo setter) throws RemoteException;

    void receiveResponse(ServerResponse response) throws RemoteException;

}


