package edu.psu.cse.vadroid;

import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Zack on 7/17/15.
 */
public class VADroidUtils {
    public static String TAG = "VADroid";
    public static boolean buildDir(String path) {
        if (new File(path).exists())
            return true;
        String[] dirs = path.split("/");
        String dir = "";

        for (String str : dirs) {
            dir += "/" + str;
            File f = new File(dir);
            if (!f.exists()) {
                if (!f.mkdir())
                    return false;
            }
        }

        return true;
    }
}
