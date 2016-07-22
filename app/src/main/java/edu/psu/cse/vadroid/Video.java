package edu.psu.cse.vadroid;

import android.location.Location;
import android.media.MediaMetadataRetriever;

import java.io.File;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import edu.psu.cse.vadroid.Ffmpeg;

public class Video implements Serializable {

    private static final int PROCESS_FRAME_RATE = 1000; // ms/frame

    public String name;
    public Timestamp timestamp;
    public String path;
    public long size;
    public long duration;
    public int bitrate;
    public String mime;
    public Location location;
    public int width;
    public int height;
    public int rotation;
    public double fps;
    public int framesProcessed;
    public int totalFrames;
    public VanetPb.ProcessMode processMode;
    public int extractionTime;
    public int classification_time;
    public List<Integer> tags;
    public Object metametadata;

    public Video() {
        tags = new ArrayList<Integer>();
        location = new Location("CaffeApp");
    }

    public Video(String path) {
        tags = new ArrayList<Integer>();
        location = new Location("CaffeApp");
        this.path = path;

        MediaMetadataRetriever meta = new MediaMetadataRetriever();
        meta.setDataSource(path);
        name = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (name == null) {
            String regex = "([^/\\.]+)(\\.)(\\w+)[^/]*$";
            Pattern pat = Pattern.compile(regex);
            Matcher m = pat.matcher(path);
            if (m.find()) {
                name = m.group();
            } else {
                name = path;
            }
        }

        String info = Ffmpeg.getInfo(this);
        String regex = "creation_time\\s*:\\s*[0-9]{4}-[0-9]{2}-[0-9]{2}\\s*[0-9]{2}:[0-9]{2}:[0-9]{2}";
        Pattern pat = Pattern.compile(regex);
        Matcher m = pat.matcher(info);
        if (m.find()) {
            String creation_time = m.group();
            regex = "[0-9]{4}-[0-9]{2}-[0-9]{2}\\s*[0-9]{2}:[0-9]{2}:[0-9]{2}";
            pat = Pattern.compile(regex);
            m = pat.matcher(creation_time);
            if (m.find()) {
                timestamp = Timestamp.valueOf(m.group());
            }
        }

        regex = "[0-9]*\\.[0-9]*\\sfps";

        pat = Pattern.compile(regex);
        m = pat.matcher(info);
        if (m.find()) {
            String fps_str = m.group();
            regex = "[0-9]*\\.?[0-9]*";
            pat = Pattern.compile(regex);
            m = pat.matcher(fps_str);
            if (m.find())
                fps = Float.parseFloat(m.group());
            else
                fps = -1;
        }

        size = new File(path).length();
        duration = Long.parseLong(meta
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        bitrate = Integer.parseInt(meta
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
        mime = meta
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
        String locationRegex = "[+-][0-9]{3}.[0-9]{3}";
        pat = Pattern.compile(locationRegex);
        String value = meta
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);
        if (value != null) {
            m = pat.matcher(meta
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION));
            if (m.find()) {
                location.setLatitude(Double.parseDouble(m.group()));
            }
            if (m.find()) {
                location.setLongitude(Double.parseDouble(m.group()));
            }
        }

        height = Integer
                .parseInt(meta
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        width = Integer
                .parseInt(meta
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        rotation = Integer
                .parseInt(meta
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
        framesProcessed = 0;
        extractionTime = -1;
        classification_time = -1;
        totalFrames = (int) (duration / PROCESS_FRAME_RATE) + 1;
        processMode = VanetPb.ProcessMode.MOBILE;
    }

    @Override
    public String toString() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < 5 && i < tags.size(); i++) {
            list.add(tags.get(i));
        }
        StringBuilder sb = new StringBuilder();
        Formatter form = new Formatter(sb);
        String str = "Name: %s\nDate: %s\nPath: %s\nBitrate: %d\nMIME: %s\nLocation: %s\nRes: %dx%d\nRotation: %d\nTags: %s\n";
        StringBuilder loc = new StringBuilder();
        double val = location.getLatitude();
        if (val >= 0) {
            loc.append('+');
        }
        loc.append(val);
        val = location.getLongitude();
        if (val >= 0) {
            loc.append('+');
        }
        loc.append(val);
        if (timestamp == null)
            timestamp = new Timestamp(0);
        form.format(str, name, timestamp.toString(), path, bitrate, mime,
                loc.toString(), width, height, rotation,
                android.text.TextUtils.join(",", list));
        form.close();
        return sb.toString();
        // return "Name: " + name + "\nDate: " + (timestamp != null ?
        // timestamp.toString() : "") + "\nPath: " + path + "Duration "\nTags:
        // " + android.text.TextUtils.join(",", list) + "\n";
    }

    public static boolean hasVideo(List<Video> vids, Video vid) {
        for (Video v : vids) {
            if (v.path.equals(vid.path))
                return true;
        }
        return false;
    }


}
