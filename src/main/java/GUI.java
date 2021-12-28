
import javax.swing.*;
import java.awt.*;

/**
 * Reference: https://www.youtube.com/watch?v=ohNqQagkDDY&ab_channel=BroCode
 */
public class GUI extends JFrame{
    private GameState gameState;
    private JFrame frame;
    private JScrollPane scorePanel;
    private JPanel mazePanel;
    private JSplitPane allSplitPane;
    private JSplitPane leftSplitPane;
    private JPanel serverPanel;
    private JList<PlayerGameData> playerList;
    private final int N;
    private JLabel primary;
    private JLabel backup;

    public GUI(String localName, int N) {
        this.N = N;
        frame = new JFrame(localName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        mazePanel = new JPanel();
        mazePanel.setLayout(new GridLayout(N, N));
        mazePanel.setVisible(true);

        playerList = new JList<>();
        playerList.setFont(new Font("Arial",Font.PLAIN,18));

        scorePanel = new JScrollPane(playerList);
        scorePanel.setVisible(true);

        serverPanel = new JPanel();
        primary = new JLabel();
        backup = new JLabel();

        serverPanel.setLayout(new BoxLayout(serverPanel, BoxLayout.Y_AXIS));
        serverPanel.add(primary);
        serverPanel.add(backup);

        leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scorePanel, serverPanel);
        leftSplitPane.setDividerLocation(450);

        allSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                leftSplitPane, mazePanel);

        allSplitPane.setDividerLocation(300);
        frame.add(allSplitPane);
        frame.setVisible(true);
    }

    public void refreshGraph(GameState gameState) {
        synchronized (this) {
            this.gameState = gameState;
            refreshServerPanel(gameState.getPname(), gameState.getBname());
            refreshScorePanel();
            refreshMazePanel();
        }
    }

    private void refreshMazePanel() {
        mazePanel.removeAll();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                Point point = gameState.getMaze()[i][j];
                final JLabel label = new JLabel(point.toString(), SwingConstants.CENTER);
                label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                mazePanel.add(label);
            }
        }
        mazePanel.revalidate();
        mazePanel.repaint();
    }

    public void refreshScorePanel()  {
        // update player data
        PlayerGameData[] arr = new PlayerGameData[gameState.getPlayers().size()];
        playerList.setListData(gameState.getPlayers().toArray(arr));
    }

    public void refreshServerPanel(String primaryName, String backupName) {
        primary.setText("Primary Server: " + primaryName);
        backup.setText("Backup Server:" + backupName);
    }
}
