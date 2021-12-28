import javax.swing.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class PlayerImpl implements Player {
    private GameState state;
    private BootstrapInfo bootstrapInfo;
    private Bootstrap tracker;
    private GUI gui;

    private int requestNo;

    private CopyOnWriteArrayList<PlayInfo> playerList;

    private Role role;
    private ScheduledExecutorService scheduler;

    private PlayInfo primaryServer;
    private PlayInfo backupServer;
    private PlayInfo playInfo;
    private String id;


    private volatile boolean hasJoinGame;

    private final Object listStateLock = new Object();
    private final Object joinLock = new Object();

    public PlayerImpl(String id) throws RemoteException {
        this.id = id;
        this.playInfo = new PlayInfo(this, id);
        this.role = Role.NORMAL;
        UnicastRemoteObject.exportObject(this, 0);
    }

    public void contactTracker(String ip, int port) throws RemoteException {
        // find Tracker from rmiregistry
        Registry registry = LocateRegistry.getRegistry(ip, port);
        try {
            tracker = (Bootstrap) registry.lookup("Tracker");
        } catch (NotBoundException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("Successfully contact with tracker");
        // obtain information about the existing game
        try {
            bootstrapInfo = tracker.registerPlayer(playInfo);
            playerList = new CopyOnWriteArrayList<>(bootstrapInfo.getPlayInfos());
        } catch (RemoteException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not contact tracker");
        }
    }

    public void joinGame() throws RemoteException {
        System.out.println("Trying to join game...");
        gui = new GUI(playInfo.getId(), bootstrapInfo.getN());
        Role roleToBeAssigned = Role.NORMAL;
        // Can fail to Join (PS may crashed or exited)
        while (!hasJoinGame) {
            try {
                removeCrashedPlayerFromList();
                if (playerList.size() == 1) {
                    setRole(Role.PRIMARY, this.playInfo);
                    state = new GameState(
                            bootstrapInfo.getN(),
                            bootstrapInfo.getK(),
                            new ArrayList<>()
                    );
                }

                if (playerList.size() == 1) {
                    roleToBeAssigned = Role.PRIMARY;
                } else if (playerList.size() == 2) {
                    roleToBeAssigned = Role.BACKUP;
                }

//                System.out.println("SELF DETERMINED ROLE " + roleToBeAssigned);


                try {
                    while (playerList.get(0).getPlayer().getRole() != Role.PRIMARY) {
                        System.out.println("list[0] is not ps");
                        sleep(200);
                    }
                    setPrimaryServer(playerList.get(0));
                } catch (RemoteException ignored) {
                    sleep(200);
                    System.out.println("list[0] " + playerList.get(0).getId() + " crashed when finding ps");
                    continue;
                }

                ServerResponse response = primaryServer.getPlayer().handleJoinRequest(this.playInfo, roleToBeAssigned);

                if (response == null) {
//                    System.out.println("failed to join ,bs/ps has not join");
                    continue;
                }
//                System.out.println("Player list len from state = " + response.getState().getPlayers().size());

                receiveResponse(response);
                hasJoinGame = true;
                System.out.println("successfully join game");
            } catch (Exception e) {
                e.printStackTrace();
                sleep(500);
            }
        }
    }

    @Override
    public  ServerResponse handleJoinRequest(PlayInfo playInfo, Role roleToBeAssigned) throws RemoteException {
        synchronized (joinLock) {
            sleep(200);
            System.out.println("Player want to join game - " + playInfo.getId());
            if (!playInfo.getId().equals(id) && !hasJoinGame) {
                System.out.println("ERROR: PS has not join game - join request from " + playInfo.getId());
                return null;
            }

            // let bs first join
            if (backupServer != null && backupServer.getId().equals(this.id)  && roleToBeAssigned != Role.BACKUP) {
                System.out.println("ERROR: BS has not join game - join request from " + playInfo.getId());
                return null;
            }

            for (PlayInfo p: playerList) {
                if (p.getId().equals(playInfo.getId()) && !playInfo.getId().equals(id)) {
                    return new ServerResponse(playerList, state);
                }
            }

            synchronized (listStateLock) {
                if (!playInfo.getId().equals(id)) {
                    playerList.add(playInfo);
                }
                PlayerGameData playerData = new PlayerGameData(playInfo.getId());
                state.addPlayer(playerData);
            }


                if (roleToBeAssigned == Role.BACKUP) {
                    playInfo.getPlayer().setRole(Role.BACKUP, this.playInfo);
                    setBackupServer(playInfo);
                }

                if (roleToBeAssigned == Role.PRIMARY) {
                    setBackupServer(playInfo);
                }

                refreshServerName();
                refreshGUI();

                if (!backupServer.getId().equals(primaryServer.getId()) && !playInfo.getId().equals(backupServer.getId())) {
//                    System.out.println("has refresh backup server [" + backupServer.getId() + "], refresh len = " + state.getPlayers().size());
                    refreshGame(backupServer);
                }
                System.out.println("OK! Player has joined the game - " +playInfo.getId() + " len from state = " + state.getPlayers().size());
                return new ServerResponse(playerList, state);
            }
    }


    @Override
    public ServerResponse handleMoveRequest(Direction direction, PlayInfo playInfo, int requestNo) throws RemoteException {
        synchronized (listStateLock) {
            System.out.println("handle move request send from [" +playInfo.getId()+"]" );
            this.state.move(direction, playInfo.getId(), requestNo);
        }


        refreshGUI();

        if (backupServer != null && !backupServer.getId().equals(primaryServer.getId()) && !playInfo.getId().equals(backupServer.getId())) {
            refreshGame(backupServer);
        }

        return new ServerResponse(playerList, state);

    }



    public void refreshGame(PlayInfo playInfo)   {
        // does not need to refresh ps itself
        if (playInfo.getId().equals(this.id)) {
            return;
        }
        // player may crashed or leave
        // if so does nothing
        try {
            playInfo.getPlayer().receiveResponse(new ServerResponse(playerList, state));
            System.out.println(playInfo.getId() + " refreshed!");
        } catch (RemoteException e) {
            System.out.println("Fail to refresh [" + playInfo.getId() + "], it may crashed" );
        }
    }


    @Override
    public void setState(GameState state) throws RemoteException {
        this.state = state;
        refreshGUI();
    }


    public void handleCrashedPlayer(String crashedId) {
        System.out.println("Player - [" + crashedId + "] crashed!" );
        Role crashedRole = Role.NORMAL;

        // Not ps crash, Let ps handle it (i must be 0 if it is ps crash)
        if (role == Role.BACKUP) {
            if (!crashedId.equals(primaryServer.getId())) {
                return;
            }
            crashedRole = Role.PRIMARY;
            System.out.println("It is Primary Server Crashed");
        }  else if (role == Role.PRIMARY) {
            if (crashedId.equals(backupServer.getId())) {
                crashedRole = Role.BACKUP;
                System.out.println("It is Backup Server Crashed");
            }
        }

        try {
            synchronized (listStateLock) {
                removePlayer(crashedId);
                tracker.removePlayer(crashedId);
                System.out.println("After remove crashed, length of player list = " + playerList.size());

                if (playerList.size() == 1) {
                    if (role == Role.BACKUP) {
                        setRole(Role.PRIMARY, this.playInfo);
                        setPrimaryServer(this.playInfo);
                    }
                    setBackupServer(this.playInfo);
                    refreshServerName();
                    refreshGUI();
                    System.out.println("Crash Handled!");
                    return;
                }
            }

                PlayInfo bs = null;
                for (PlayInfo info: playerList) {
                    if (info.getPlayer().getRole() == Role.NORMAL) {
                        bs = info;
                        break;
                    }
                }
                if (crashedRole == Role.PRIMARY) {
                    setRole(Role.PRIMARY, this.playInfo);
                    setPrimaryServer(this.playInfo);
                    if (bs != null) {
                        bs.getPlayer().setPlayerList(playerList);
                        System.out.println("I PS, set [" + bs.getId() + "] As BS Because bs crashed");
                        bs.getPlayer().setRole(Role.BACKUP, this.playInfo);
                        setBackupServer(bs);
                    } else {
                        System.out.println("SET BS MY SELF");
                        setBackupServer(this.playInfo);
                    }
                } else if (crashedRole == Role.BACKUP) {
                    if (bs != null) {
                        bs.getPlayer().setPlayerList(playerList);
                        System.out.println("I PS, set [" + bs.getId() + "] As BS Because ps crashed");
                        bs.getPlayer().setRole(Role.BACKUP, this.playInfo);
                        setBackupServer(bs);
                    } else {
                        System.out.println("SET BS AS MY SELF");
                        setBackupServer(this.playInfo);
                    }
                }


            refreshServerName();
            refreshGUI();
            for (PlayInfo player: playerList) {
                System.out.println("refresh - " + player.getId());
                refreshGame(player);
            }
//            tracker.removePlayer(crashedId);
            System.out.println("Have Refresh All game state after some player crash ");
        } catch (RemoteException e) {
            System.out.println("REMOTE EXCEPTION HANDLE CRASHED PLAYER");
        }
        System.out.println("Crash handled!");
    }


    // Can only be invoked by server
    private void removePlayer(String crashedId) {
        int i=0;
        boolean inList = false;
        while (i < playerList.size()) {
            if (playerList.get(i).getId().equals(crashedId)) {
                inList = true;
                break;
            }
            i++;
        }
        if (inList) {
            playerList.remove(i);
            state.removePlayer(crashedId);
        } else {
            System.out.println("Failed to find player when remove");
        }
    }

    @Override
    public void refreshGUI() {
        if (gui != null) {
            SwingUtilities.invokeLater(() -> gui.refreshGraph(state));
        }
    }


    public void makePingToPlayers() {
        for (int i=0; i<playerList.size(); i++) {
            try {
                playerList.get(i).getPlayer().ping();
            } catch (RemoteException e) {
                // Handle crashed player
                System.out.println("[" + playerList.get(i).getId() + "] crashed!!!!");
                handleCrashedPlayer(playerList.get(i).getId());
                return;
            }
        }
    }


    @Override
    public void setPlayerList(CopyOnWriteArrayList<PlayInfo> playerList) throws RemoteException {
        this.playerList = playerList;
    }



    @Override
    public void ping() throws RemoteException {

    }


    public void lookupPrimaryServer() {
        if (role == Role.PRIMARY) {
            return;
        }
        while (true) {
            try {
                System.out.println("Looking up server");

                if (playerList.get(0).getPlayer().getRole() == Role.PRIMARY) {
                    setPrimaryServer(playerList.get(0));
                } else {
                    sleep(200);
                }
                System.out.println("[PS] = " + primaryServer.getId());
                return;
            } catch (RemoteException e) {
                System.out.println("ps - " + primaryServer.getId() + " crashed at lookupserver");
                sleep(500);
            }
        }
    }

    private void removeCrashedPlayerFromList() {
        List<Integer> crashedIdx = new ArrayList<>();
        for (int i=0; i<playerList.size(); i++) {
            try {
                playerList.get(i).getPlayer().ping();
            }   catch (RemoteException e) {
                crashedIdx.add(i);
            }
        }

        for (int i: crashedIdx) {
            System.out.println("Player pre crashed - " + playerList.get(i).getId());
        }

        int removedCount = 0;
        while (crashedIdx.size() != 0) {
            int index = crashedIdx.remove(0);
            playerList.remove(index-removedCount);
            removedCount++;
        }
    }


    @Override
    public Role getRole() throws RemoteException {
        return role;
    }

    @Override
    public void setRole(Role role, PlayInfo setter) throws RemoteException {
        System.out.println("SET ROLE -> " + role + ", setter = " + setter.getId());
        this.role = role;
        if ((role == Role.PRIMARY || role == Role.BACKUP) && scheduler == null) {
            if (role == Role.BACKUP) {
                setPrimaryServer(setter);
            }
            System.out.println("Start Pinging");
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(this::makePingToPlayers, 0, 500, TimeUnit.MILLISECONDS);
        }
    }


    public void setPrimaryServer(PlayInfo primaryServer) {
        this.primaryServer = primaryServer;
    }

    public void setBackupServer(PlayInfo backupServer) {
        System.out.println("SET NEW BS =  " + backupServer.getId());
        this.backupServer = backupServer;
    }



    public void move(Direction direction) {
        while (true) {
            try {
                ServerResponse response = primaryServer.getPlayer().handleMoveRequest(direction, playInfo, requestNo);
                if (role != Role.PRIMARY) {
                    receiveResponse(response);
                }
                System.out.println("move request done by -" + primaryServer.getId());
                requestNo++;
                return;
            } catch (RemoteException e) {
                lookupPrimaryServer();
                sleep(200);
            }
        }
    }


    public void quit() {
        System.exit(0);
    }


    public void refreshServerName() {
        state.setPname(primaryServer.getId());
        state.setBname(backupServer.getId());
    }

    public void sleep(int n) {
        try {
            Thread.sleep(n);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void receiveResponse(ServerResponse response) {
        synchronized (listStateLock) {
            try {
                setPlayerList(response.getPlayers());
                setState(response.getState());
                System.out.println("I - [" + id + "] has received response adn refreshed!");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

}
