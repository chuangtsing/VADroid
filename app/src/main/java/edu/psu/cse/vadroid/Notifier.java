package edu.psu.cse.vadroid;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;


public class Notifier {

    public enum MessageWhat {
        INIT_QUERY,
        BEGIN_PROCESSING,
        BEGIN_CAFFE_INIT,
        END_CAFFE_INIT,
        SET_QUERY_PROCESS,
        UPDATE_QUERY_PROCESS,
        POST_QUERY_RESULTS,
        END_QUERY;
    }

    public enum QueryState {
        NONE,
        QUERY_INIT,
        CAFFE_INIT,
        PROCESSING,
        FINISHED;
    }

    public enum Location {
        LOCAL,
        SERVER;
    }

    private static int id = 1;
    private static NotificationManager manager = null;
    private static int progMax = 0;
    private static NotificationCompat.Builder progBuilder = null;

    private static Context mContext;
    private static Handler handler = null;

    private static List<VideoProcess> local;
    private static List<VideoProcess> server;
    private static int currentLocal, currentServer;

    static {
        mContext = VADroid.getContext();
        manager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private static void setNotificationIndeterminateProgress(String str) {
        Notifier.progBuilder = new NotificationCompat.Builder(mContext);
        manager.cancel(0);
        Notifier.progBuilder.setProgress(0, 0, true)
                .setContentTitle(mContext.getString(R.string.notif_prefix, str))
                .setSmallIcon(R.mipmap.ic_notif);
        manager.notify(0, Notifier.progBuilder.build());
    }

    private static void setNotificationProgress(int current, int max, String str) {
        Notifier.progBuilder = new NotificationCompat.Builder(mContext);
        manager.cancel(0);
        Notifier.progMax = max;
        Notifier.progBuilder.setProgress(max, current, false)
                .setContentTitle(mContext.getString(R.string.notif_prefix, str))
                .setContentText(current + "/" + max)
                .setSmallIcon(R.mipmap.ic_notif);
        manager.notify(0, Notifier.progBuilder.build());
    }

    private static void updateNotificationProgress(int current) {
        if (Notifier.progBuilder == null)
            return;
        Notifier.progBuilder.setProgress(Notifier.progMax, current, false)
                .setContentText(current + "/" + Notifier.progMax);
        manager.notify(0, Notifier.progBuilder.build());
    }

    private static void endProgress() {
        if (manager == null)
            return;
        manager.cancel(0);
        Notifier.progBuilder = null;
        if (ListenerService.service != null)
            Notifier.setRunning();
    }


    public static void setRunning() {
        Intent notificationIntent = new Intent(mContext, MainActivity.class);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent intent = PendingIntent.getActivity(mContext, 0,
                notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext).setContentTitle(mContext.getString(R.string.notif_prefix, mContext.getString(R.string.notif_listening)))
                .setContentText(mContext.getString(R.string.touch_settings)).setSmallIcon(R.mipmap.ic_notif).setOngoing(true).setContentIntent(intent);

        manager.notify(0, builder.build());
    }

    public static void clearNotification() {
        manager.cancel(0);
    }


    public static void initQuery() {
        if (handler != null) {
            Message msg = handler.obtainMessage(MessageWhat.INIT_QUERY.ordinal());
            msg.sendToTarget();
        }
        setNotificationIndeterminateProgress("Query in progress.");
    }


    public static void beginProcessing(List<Video> local, List<Video> server) {
        currentLocal = 0;
        currentServer = 0;
        Notifier.local = new ArrayList<>();
        Notifier.server = new ArrayList<>();
        for (Video video : local) {
            Notifier.local.add(new VideoProcess(video));
        }
        for (Video video : server) {
            Notifier.server.add(new VideoProcess(video));
        }

        if (handler != null) {
            Message msg = handler.obtainMessage(MessageWhat.BEGIN_PROCESSING.ordinal());
            msg.sendToTarget();
        }
    }

    public static void setProcess(Location location, VideoProcess.ProcessState state, int maxProgress) {
        Message msg = handler.obtainMessage(MessageWhat.SET_QUERY_PROCESS.ordinal());
        msg.arg1 = location.ordinal();
        switch (location) {
            case LOCAL:
                msg.arg1 = Location.LOCAL.ordinal();
                msg.arg2 = currentLocal;
                local.get(currentLocal).state = state;
                switch (state) {
                    case EXTRACTING:
                        //setNotificationProgress(0, maxProgress, "Extracting video.");
                        setNotificationIndeterminateProgress("Extracting frames");
                        //local.get(currentLocal).maxProgress = maxProgress;
                        break;
                    case CLASSIFYING:
                        setNotificationIndeterminateProgress("Classifying video.");
                        break;
                    case FINISHED:
                        currentLocal++;
                        break;
                }
                break;
            case SERVER:
                msg.arg1 = Location.SERVER.ordinal();
                msg.arg2 = currentServer;
                server.get(currentServer).state = state;
                switch (state) {
                    case UPLOADING:
                        server.get(currentServer).maxProgress = 100;
                        break;
                    case FINISHED:
                        currentServer++;
                        break;
                }
                break;
        }
        msg.sendToTarget();
    }

    public static void updateProcess(Location location, VideoProcess.ProcessState state, int progress) {
        Message msg = handler.obtainMessage(MessageWhat.UPDATE_QUERY_PROCESS.ordinal());
        msg.arg1 = location.ordinal();
        switch (location) {
            case LOCAL:
                updateNotificationProgress(progress);
                local.get(currentLocal).progress = progress;
                switch (state) {
                    case EXTRACTING:
                        msg.arg2 = currentLocal;
                        break;
                }
                break;
            case SERVER:
                server.get(currentServer).progress = progress;
                switch (state) {
                    case UPLOADING:
                        msg.arg2 = currentServer;
                        break;
                }
                break;
        }
        msg.sendToTarget();
    }

    public static void beginCaffeInit() {
        if (handler != null) {
            Message msg = handler.obtainMessage(MessageWhat.BEGIN_CAFFE_INIT.ordinal());
            msg.sendToTarget();
        }
    }

    public static void endCaffeInit() {
        if (handler != null) {
            Message msg = handler.obtainMessage(MessageWhat.END_CAFFE_INIT.ordinal());
            msg.sendToTarget();
        }
    }

    public static void endQuery() {
        if (handler != null) {
            Message msg = handler.obtainMessage(MessageWhat.END_QUERY.ordinal());
            msg.sendToTarget();
        }
        endProgress();
    }

    public static void registerHandler(Handler handler) {
        Notifier.handler = handler;
    }

    public static void unregisterHandler() {
        handler = null;
    }

    public static List<VideoProcess> getList(Location location) {
        switch (location) {
            case LOCAL:
                return local;
            case SERVER:
                return server;
            default:
                return new ArrayList<VideoProcess>();
        }
    }

}

