package edu.psu.cse.vadroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.google.protobuf.CodedInputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VANet {
    private Socket socket;
    private static final int BUFFER_SIZE = 65536;
    private Context context;
    private static boolean socketInUse = false;
    private static Object socketLock;
    private boolean keepAlive;
    static int PING_PORT = 38301;
    static int PING_TIMEOUT = 15000;
    private boolean connected;
    private CaffeMobile caffe;
    private ExecutorService sendService;
    private ExecutorService extractService;
    private ExecutorService classifyService;

    public enum UploadTask {
        VIDEO, VIDEO_INFO, FRAME, FRAME_INFO;
    }

    public VANet(Socket socket, Context context) {
        this.socket = socket;
        this.context = context;
        this.connected = true;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void initCaffe() {
        if (caffe == null) {
            caffe = CaffeMobile.caffeMobile;
        }
        caffe.startInit();

    }

    public void sendInfo(Iterable<Video> vids) {
        VanetPb.ClientMessage.Builder responseBuilder = VanetPb.ClientMessage.newBuilder()
                .setType(VanetPb.ClientMessage.Type.VIDEO_INFO);

        for (Video vid : vids) {

            responseBuilder.addVideoInfo(buildVideoInfo(vid));
        }

        sendMessage(responseBuilder);
    }

    public VanetPb.VideoInfo buildVideoInfo(Video vid) {
        VanetPb.VideoInfo.Builder infoBuilder = VanetPb.VideoInfo.newBuilder().setName(vid.name)
                .setPath(vid.path)//.setTimestamp(vid.timestamp.toString())
                .setSize(vid.size).setDuration(vid.duration)
                .setBitrate(vid.bitrate).setMime(vid.mime)
                .setLocLat(vid.location.getLatitude())
                .setLocLong(vid.location.getLongitude())
                .setWidth(vid.width).setHeight(vid.height)
                .setRotation(vid.rotation).setFramesProcessed(vid.framesProcessed)
                .setFramesTotal(vid.totalFrames)
                .addAllTags(vid.tags);

        if (vid.timestamp == null)
            infoBuilder.setTimestamp("");
        else
            infoBuilder.setTimestamp(vid.timestamp.toString());

        infoBuilder.setProcessMode(vid.processMode);

        return infoBuilder.build();
    }


    public VanetPb.ServerMessage receiveMessage() {
        if (!connected)
            return null;
        try {

            InputStream in = socket.getInputStream();
            CodedInputStream coded = CodedInputStream.newInstance(socket.getInputStream());
            return VanetPb.ServerMessage.parseDelimitedFrom(in);
            //size = in.read(bite, 0, 30);
            /*int size = coded.readRawVarint32();
            int read = 0, total = 0;
            byte[] bites = new byte[size];
            int avail = in.available();
            while (total < size && (read = in.read(bites, 0, size)) != 0)
                total += read;
            return VanetPb.ServerMessage.parseFrom(bites);*/
        } catch (SocketTimeoutException e) {
            Log.e(VADroidUtils.TAG, "server connection timeout");
            connected = false;
        } catch (IOException e) {
            Log.e(VADroidUtils.TAG, e.getMessage());
            connected = false;
        }
        /*size = size;
        bite = bite;
        VanetPb.ServerMessage msg = null;
        byte[] arr = Arrays.copyOfRange(bite, 4, size);
        try {
            msg = VanetPb.ServerMessage.parseFrom(arr);
        }
        catch (InvalidProtocolBufferException e){
        }
        return msg;*/
        return null;
    }

    public void sendMessage(VanetPb.ClientMessage.Builder builder) {
        if (!connected)
            return;
        WifiManager wifiMan = (WifiManager) context.getSystemService(
                Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMan.getConnectionInfo();
        if (wifiInfo == null)
            return;
        String macAddr = wifiInfo.getMacAddress();
        int link  = wifiInfo.getLinkSpeed();
        builder.setMac(Long.parseLong(macAddr.toUpperCase().replaceAll("[^0-9A-F]", ""), 16));
        VanetPb.ClientMessage msg = builder.build();

        try {
            //CodedOutputStream out = CodedOutputStream.newInstance(socket.getOutputStream());
            //out.writeRawVarint32(msg.getSerializedSize());
            //msg.writeTo(out);
            msg.writeDelimitedTo(socket.getOutputStream());
            //out.flush();
        } catch (SocketTimeoutException e) {
            Log.e(VADroidUtils.TAG, "server connection timeout");
            connected = false;
        } catch (IOException e) {
            VanetPb.ClientMessage.Type t = builder.getType();
            String type = "";
            if (t.equals(VanetPb.ClientMessage.Type.VIDEO_INFO))
                type = "info";
            else if (t.equals(VanetPb.ClientMessage.Type.INIT))
                type = "init";
            else if (t.equals(VanetPb.ClientMessage.Type.VIDEO))
                type = "video";
            Log.e(VADroidUtils.TAG, "error sending " + type + " message");
            connected = false;
        }
    }

    public void sendVideo(Video vid) {
        if (!connected)
            return;
        if (!new File(vid.path).exists())
            return;

        VanetPb.ClientMessage.Builder msg = VanetPb.ClientMessage.newBuilder()
                .setType(VanetPb.ClientMessage.Type.VIDEO)
                .addVideoInfo(buildVideoInfo(vid));
        sendMessage(msg);
        long total = 0;
        int percent = 0;
        try {
            OutputStream out = new BufferedOutputStream(socket.getOutputStream());
            FileInputStream file = new FileInputStream(vid.path);
            byte[] buf = new byte[BUFFER_SIZE];

            int bytesRead = 0;
            while (-1 != (bytesRead = file.read(buf, 0, buf.length))) {
                try {
                    out.write(buf, 0, bytesRead);
                } catch (Exception e) {
                    Log.e(VADroidUtils.TAG, "error sending video");
                }
                total += bytesRead;
                int newPercent = (int) (total * 100 / vid.size);
                if (newPercent - percent >= 10) {
                    percent = newPercent;
                    Notifier.updateProcess(Notifier.Location.SERVER, VideoProcess.ProcessState.UPLOADING, percent);
                }
            }
            file.close();
            out.flush();
        } catch (IOException e) {
            Log.e(VADroidUtils.TAG, "error sending video");
            connected = false;
        } catch (Exception e) {
            Log.e(VADroidUtils.TAG, "error sending video");
        }
    }

    public void sendVideoFinal(Video vid) {
        if (!connected)
            return;
        if (!new File(vid.path).exists())
            return;

        VanetPb.ClientMessage.Builder msg = VanetPb.ClientMessage.newBuilder()
                .setType(VanetPb.ClientMessage.Type.VIDEO)
                .addVideoInfo(buildVideoInfo(vid));
        sendMessage(msg);
        try {
            OutputStream out = new BufferedOutputStream(socket.getOutputStream());
            FileInputStream file = new FileInputStream(vid.path);
            byte[] buf = new byte[BUFFER_SIZE];

            int bytesRead = 0;
            while (-1 != (bytesRead = file.read(buf, 0, buf.length))) {
                try {
                    out.write(buf, 0, bytesRead);
                } catch (Exception e) {
                    Log.e(VADroidUtils.TAG, "error sending video");
                }
            }
            file.close();
            out.flush();
        } catch (IOException e) {
            Log.e(VADroidUtils.TAG, "error sending video");
            connected = false;
        } catch (Exception e) {
            Log.e(VADroidUtils.TAG, "error sending video");
        }
    }



    public boolean init() {
        VanetPb.ClientMessage.Builder msg = VanetPb.ClientMessage.newBuilder();
        msg.setType(VanetPb.ClientMessage.Type.INIT);
        ArrayList<String> res = new ArrayList<String>();

        File f;
        f = new File(CaffeMobile.DEFAULT_MODEL_PATH);
        if (!f.exists()) {
            msg.addResources(VanetPb.Resource.MODEL);
            res.add(CaffeMobile.DEFAULT_MODEL_PATH);
        }
        f = new File(CaffeMobile.DEFAULT_WEIGHTS_PATH);
        if (!f.exists()) {
            msg.addResources(VanetPb.Resource.WEIGHTS);
            res.add(CaffeMobile.DEFAULT_WEIGHTS_PATH);
        }
        f = new File(CaffeMobile.DEFAULT_MEAN_PATH);
        if (!f.exists()) {
            msg.addResources(VanetPb.Resource.MEAN);
            res.add(CaffeMobile.DEFAULT_MEAN_PATH);
        }
        f = new File(CaffeMobile.SYNSET_PATH);
        if (!f.exists()) {
            msg.addResources(VanetPb.Resource.SYNSET);
            res.add(CaffeMobile.SYNSET_PATH);
        }

        sendMessage(msg);
        if (!connected)
            return false;


        VanetPb.ServerMessage sMsg = receiveMessage();
        if (!connected)
            return false;


        if (sMsg == null || sMsg.getType() != VanetPb.ServerMessage.Type.RES)
            return false;


        for (int i = 0; i < sMsg.getResourcesCount(); i++) {
            VanetPb.Resource r = sMsg.getResources(i);
            String path;
            if (r.equals(VanetPb.Resource.MODEL)) {
                downloadResources(CaffeMobile.DEFAULT_MODEL_PATH, sMsg.getSize(i));
            } else if (r.equals(VanetPb.Resource.WEIGHTS)) {
                downloadResources(CaffeMobile.DEFAULT_WEIGHTS_PATH, sMsg.getSize(i));
            } else if (r.equals(VanetPb.Resource.MEAN)) {
                downloadResources(CaffeMobile.DEFAULT_MEAN_PATH, sMsg.getSize(i));
            } else if (r.equals(VanetPb.Resource.SYNSET)) {
                downloadResources(CaffeMobile.SYNSET_PATH, sMsg.getSize(i));
            }
        }


        return true;
    }

    public boolean downloadResources(String res, long size) {
        long total = 0;
        try {
            String path = res.substring(0, res.lastIndexOf('/'));
            if (!VADroidUtils.buildDir(path))
                return false;
            InputStream in = socket.getInputStream();
            FileOutputStream out = new FileOutputStream(res);

            byte[] buf = new byte[BUFFER_SIZE];

            long rem = size;
            int bytesRead = (int) (rem < BUFFER_SIZE ? size : BUFFER_SIZE);
            while ((total < size) && (((bytesRead = in.read(buf, 0, bytesRead))) > 0)) {
                out.write(buf, 0, bytesRead);
                total += bytesRead;
                rem -= bytesRead;
                bytesRead = (int) (rem < BUFFER_SIZE ? rem : BUFFER_SIZE);
            }
            out.flush();
            out.close();
        } catch (SocketTimeoutException e) {
            Log.e(VADroidUtils.TAG, "server connection timeout");
            connected = false;
        } catch (IOException e) {
            Log.e(VADroidUtils.TAG, "error downloading resource " + res);
            return false;
        }

        if (total != size)
            return false;

        return true;
    }

    public Map<String, Video> prepareAll() {
        DBHelper db = new DBHelper(VADroid.getContext());
        List<Video> vids = db.getAllVideos();
        List<Video> newVids = new ArrayList<Video>();
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.package_name), Context.MODE_PRIVATE);
        String directory = prefs.getString(context.getString(R.string.pref_directory), null);
        Map<String, Video> videoMap = new HashMap<>();
        if (directory == null)
            return videoMap;
        //path = CaffeMobile.VIDEOS_DIR;
        File dir = new File(directory);
        for (File vid : dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getName();
                String ext = name.substring(name.lastIndexOf('.') + 1);
                return (ext.equals("3gp") || ext.equals("mp4") || ext.equals("MOV"));
            }
        })) {
            videoMap.put(vid.getPath(), new Video(vid.getPath()));
        }
        return videoMap;
    }

    public boolean query(Map<String, Video> videoMap) {
        initCaffe();
        sendInfo(videoMap.values());
        if (!connected) {
            disconnect();
            return false;
        }
        VanetPb.ServerMessage sMsg = receiveMessage();
        if (!connected || sMsg == null
                || !sMsg.getType().equals(VanetPb.ServerMessage.Type.PROCESS_DIRECTIVE)
                || sMsg.getPathCount() != sMsg.getProcessModeCount()) {
            disconnect();
            return false;
        }

        if (sMsg.getPathCount() == 0) {
            disconnect();
            return true;
        }

        //PingThread pingThread = new PingThread();
        //pingThread.start();
        List<String> paths = sMsg.getPathList();
        List<VanetPb.ProcessMode> mode = sMsg.getProcessModeList();
        Queue<Video> localQueue = new LinkedList<>();
        ArrayList<Video> local = new ArrayList<>();
        ArrayList<Video> server = new ArrayList<>();

        if (sMsg.getPathCount() == 0)
            return true;

        for (int i = 0; i < sMsg.getPathCount(); i++) {
            String path = sMsg.getPath(i);
            if (sMsg.getProcessMode(i) == VanetPb.ProcessMode.SERVER) {
                server.add(videoMap.get(path));
            } else {
                localQueue.add(videoMap.get(path));
                local.add(videoMap.get(path));
                videoMap.remove(path);
            }
        }

        Notifier.beginProcessing(local, server);

        sendService = Executors.newSingleThreadExecutor();

        if (!local.isEmpty()) {
            if (!caffe.isInit())
                Notifier.beginCaffeInit();
            for (Video vid : local) {
                caffe.addVideoProcessAll(this, sendService, vid, CaffeMobile.ProcessMode.EXTRACT_AND_CLASSIFY);
            }
            //predictionThread = new PredictionThread(localQueue);
            //predictionThread.start();
        }

        while (connected && !videoMap.isEmpty()) {
            Notifier.setProcess(Notifier.Location.SERVER, VideoProcess.ProcessState.WAITING, 0);
            sMsg = receiveMessage();
            if (sMsg == null || sMsg.getType() != VanetPb.ServerMessage.Type.VIDEO_REQUEST)
                continue;
            sendService.submit(new AsyncSender(UploadTask.VIDEO, videoMap.get(sMsg.getPath(0))));
            videoMap.remove(sMsg.getPath(0));
        }

        try {
            caffe.waitAll();
            sendService.shutdown();
            sendService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean queryTags(Map<String, Video> videoMap) {
        initCaffe();
        sendInfo(videoMap.values());
        if (!connected) {
            disconnect();
            return false;
        }
        VanetPb.ServerMessage sMsg = receiveMessage();
        if (!connected || sMsg == null
                || !sMsg.getType().equals(VanetPb.ServerMessage.Type.PROCESS_DIRECTIVE)
                || sMsg.getPathCount() != sMsg.getProcessModeCount()) {
            disconnect();
            return false;
        }

        if (sMsg.getPathCount() == 0) {
            disconnect();
            return true;
        }

        //PingThread pingThread = new PingThread();
        //pingThread.start();
        List<String> paths = sMsg.getPathList();
        List<VanetPb.ProcessMode> mode = sMsg.getProcessModeList();
        Queue<Video> localQueue = new LinkedList<>();
        ArrayList<Video> local = new ArrayList<>();
        ArrayList<Video> server = new ArrayList<>();
        Map<String, Video> serverMap = new HashMap<>(videoMap);

        if (sMsg.getPathCount() == 0)
            return true;

        for (int i = 0; i < sMsg.getPathCount(); i++) {
            String path = sMsg.getPath(i);
            if (sMsg.getProcessMode(i) == VanetPb.ProcessMode.SERVER) {
                server.add(serverMap.get(path));
            } else {
                localQueue.add(serverMap.get(path));
                local.add(serverMap.get(path));
                serverMap.remove(path);
            }
        }

        Notifier.beginProcessing(local, server);

        sendService = Executors.newSingleThreadExecutor();

        if (!local.isEmpty()) {
            if (!caffe.isInit())
                Notifier.beginCaffeInit();
            caffe.setkResults(sMsg.getTopK());
            for (Video vid : local) {
                caffe.addVideoProcessAll(this, sendService, vid, CaffeMobile.ProcessMode.EXTRACT_AND_CLASSIFY);
            }
            //predictionThread = new PredictionThread(localQueue);
            //predictionThread.start();
        }

        while (connected && !serverMap.isEmpty()) {
            Notifier.setProcess(Notifier.Location.SERVER, VideoProcess.ProcessState.WAITING, 0);
            sMsg = receiveMessage();
            if (sMsg == null || sMsg.getType() != VanetPb.ServerMessage.Type.VIDEO_REQUEST)
                continue;
            sendService.submit(new AsyncSender(UploadTask.VIDEO, serverMap.get(sMsg.getPath(0))));
            serverMap.remove(sMsg.getPath(0));
        }

        try {
            caffe.waitAll();
            sendService.shutdown();
            sendService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        sMsg = receiveMessage();
        if (sMsg.getType() == VanetPb.ServerMessage.Type.VIDEO_REQUEST) {
            for (String path : sMsg.getPathList()) {
                sendVideoFinal(videoMap.get(path));
            }
        }

        return true;
    }

    private void disconnect() {
        connected = false;
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(VADroidUtils.TAG, "error closing ping sockets");
        }
    }

    public AsyncSender getAsyncSender(UploadTask task, Video vid) {
        return new AsyncSender(task, vid);
    }

    private class PingThread extends Thread {
        int attempts;

        public void run() {
            attempts = 0;
            ServerSocket server = null;
            Socket client = null;
            try {
                server = new ServerSocket(PING_PORT);
            } catch (Exception e) {
                return;
            }
            try {
                client = server.accept();
                client.setSoTimeout(PING_TIMEOUT);
            } catch (IOException e) {
                return;
            }

            while (connected && !isInterrupted()) {
                VanetPb.ServerMessage serverMessage;
                try {
                    InputStream in = socket.getInputStream();
                    CodedInputStream coded = CodedInputStream.newInstance(socket.getInputStream());
                    serverMessage = VanetPb.ServerMessage.parseDelimitedFrom(in);
                    if (serverMessage == null) {
                        Log.e(VADroidUtils.TAG, "bad ping message");
                        attempts++;
                        return;
                    }
                } catch (SocketTimeoutException e) {
                    Log.e(VADroidUtils.TAG, "ping timeout");
                    connected = false;
                    return;
                } catch (Exception e) {
                    Log.e(VADroidUtils.TAG, e.getMessage());
                    String msg = e.getMessage();
                    connected = false;
                    return;
                }
                if (serverMessage.getType() != VanetPb.ServerMessage.Type.PING) {
                    Log.e(VADroidUtils.TAG, "bad ping message");
                    attempts++;
                    return;
                }
                attempts = 0;

                VanetPb.ClientMessage.Builder builder = VanetPb.ClientMessage.newBuilder();
                builder.setType(VanetPb.ClientMessage.Type.PING).setTerminate(false);
                sendMessage(builder);
            }

            if (isInterrupted()) {
                VanetPb.ClientMessage.Builder builder = VanetPb.ClientMessage.newBuilder();
                builder.setType(VanetPb.ClientMessage.Type.PING).setTerminate(true);
                sendMessage(builder);

            }

            connected = false;
            try {
                client.close();
                server.close();
            } catch (IOException e) {
                Log.e(VADroidUtils.TAG, "error closing ping sockets");
            }
        }
    }

    public class AsyncSender implements Callable<Void> {
        private Video vid;
        private UploadTask task;

        public AsyncSender(UploadTask task, Video vid) {
            this.task = task;
            this.vid = vid;
        }

        @Override
        public Void call() throws Exception {
            if (!connected)
                return null;

            if (task == UploadTask.VIDEO_INFO) {
                ArrayList<Video> vids = new ArrayList<>();
                vids.add(vid);
                sendInfo(vids);

                //VanetPb.ServerMessage msg = receiveMessage();


            } else if (task == UploadTask.VIDEO) {
                Notifier.setProcess(Notifier.Location.SERVER, VideoProcess.ProcessState.UPLOADING, 100);
                sendVideo(vid);
                Notifier.setProcess(Notifier.Location.SERVER, VideoProcess.ProcessState.FINISHED, 0);
            } else if (task == UploadTask.FRAME) {

            } else if (task == UploadTask.FRAME_INFO) {

            }
            return null;
        }
    }
}