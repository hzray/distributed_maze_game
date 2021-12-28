

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class Game {
    public static void main(String[] args) throws RemoteException {
        if (args.length != 3) {
            System.out.println("Incorrect command. <java Game [IP] [port] [id]>");
            System.exit(0);
        }
        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        String id = args[2];

        if (id.length() != 2) {
            System.out.println("Id must be two characters");
            System.exit(0);
        }

        PlayerImpl player = new PlayerImpl(id);
        player.contactTracker(ip, port);
        player.joinGame();

        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println("Input >");
            String line = in.nextLine();
            switch (line) {
                case "0":
                    player.move(Direction.STAY);
                    break;
                case "1":
                    player.move(Direction.WEST);
                    break;
                case "2":
                    player.move(Direction.SOUTH);
                    break;
                case "3":
                    player.move(Direction.EAST);
                    break;
                case "4":
                    player.move(Direction.NORTH);
                    break;
                case "9":
                    player.quit();
                    System.exit(0);
                default:
                    System.out.println("Undefined command");
                    break;
            }
        }
    }
}
