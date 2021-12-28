import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;


public interface Bootstrap extends Remote {
    // add new player to the player list
    BootstrapInfo registerPlayer(PlayInfo playInfo) throws RemoteException;

    void removePlayer(String id) throws RemoteException;
}
