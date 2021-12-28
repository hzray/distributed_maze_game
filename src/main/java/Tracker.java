import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Tracker
 * The class maintain the basic information about the game
 * K, N, list of players
 */

public class Tracker implements Bootstrap {

    private final int K;
    private final int N;

    private List<PlayInfo> playInfos;
    private final Object listLockObject = new Object();



    public Tracker(int k, int n) throws RemoteException {
        UnicastRemoteObject.exportObject(this, 0);
        this.playInfos = Collections.synchronizedList(new ArrayList<>());
        this.K = k;
        this.N = n;
    }


    @Override
    public BootstrapInfo registerPlayer(PlayInfo playInfo) throws RemoteException {
        synchronized (listLockObject) {
            System.out.println("Contacted by " + playInfo.getId()) ;
            playInfos.add(playInfo);
            BootstrapInfo ret = new BootstrapInfo(N, N, new CopyOnWriteArrayList<>(playInfos));
            return ret;
        }

    }

    @Override
    public void removePlayer(String id) throws RemoteException {
        synchronized (listLockObject) {
            System.out.println("Remove player - " + id);
            boolean find = false;
            int i;
            for (i=0; i<playInfos.size();i++) {
                if (playInfos.get(i).getId().equals(id)) {
                    find = true;
                    break;
                }
            }
            if (find) {
                playInfos.remove(i);
            }
        }
    }


    // Start the Tracker
    public static void main(String[] args)  {
        if (args.length != 3) {
            System.out.println("Incorrect command. <java Tracker [port] [N] [K]>");
            System.exit(0);
        }
        int port = Integer.parseInt(args[0]);
        int n = Integer.parseInt(args[1]);
        int k = Integer.parseInt(args[2]);

        try {
            Bootstrap tracker = new Tracker(n, k);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind("Tracker", tracker);
            System.out.println("Tracker Successfully Set Up");
        } catch (Exception e) {
            System.err.println("Tracker exception: " + e);
            e.printStackTrace();
        }
    }
}
