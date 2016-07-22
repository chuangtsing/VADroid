package edu.psu.cse.vadroid;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Zack on 11/16/15.
 */
public class Optimizer {
    public enum ProcessMode {
        EXTRACT_ONLY,
        EXTRACT_AND_CLASSIFY,
        SEND_VIDEO;
    }

    private HashMap<String, VidProc> vids;
    private boolean online;

    public Optimizer (Collection<Video> vids, boolean online) {
        this.vids = new HashMap(vids.size());
        for (Video vid : vids) {
            this.online = online;
            this.vids.put(vid.path, new VidProc(vid));
        }
    }

    public int optimize() {
        if (online)
            offlineOptimize();
        else
            return onlineOptimize();

        return 0;
    }

    private void offlineOptimize() {

    }

    private int onlineOptimize() {
        return 0;
    }

    private class VidProc {
        public Video vid;
        // public ArrayList<Pair<ProcessMode, Boolean>> frames; // Pair of process mode and finished boolean
        public int currentFrame;

        public VidProc(Video vid) {
            this.vid = vid;
            //if (online)
                //frames = new ArrayList<>(vid.totalFrames);
        }
    }
}
