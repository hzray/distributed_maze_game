

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

/**
 * BootstrapInfo
 * The class maintains all information that Tracker would send to a new player
 * at the time of registration
 */

public class BootstrapInfo implements Serializable {
    private final int K;
    private final int N;
    private List<PlayInfo> playInfos;

    public BootstrapInfo(int k, int n, List<PlayInfo> playInfos) {
        this.K = k;
        this.N = n;
        this.playInfos = playInfos;
    }


    public int getK() {
        return K;
    }

    public int getN() {
        return N;
    }

    public List<PlayInfo> getPlayInfos() {
        return playInfos;
    }
}
