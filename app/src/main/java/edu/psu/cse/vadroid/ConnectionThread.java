package edu.psu.cse.vadroid;


import java.net.Socket;
import java.util.Map;

public class ConnectionThread extends Thread {

    private VANet vanet;
    private ListenerService.ListenerThread listenerThread;
    private Socket client;

    public ConnectionThread(VANet vanet, ListenerService.ListenerThread listenerThread) {
        this.vanet = vanet;
        this.listenerThread = listenerThread;
    }
    public ConnectionThread(Socket client) {
        this.client = client;
    }

    public void run() {
        vanet = new VANet(client, VADroid.getContext());
        //while (!Thread.currentThread().isInterrupted()) {
        VanetPb.ServerMessage serverMessage;

        // clientSocket.getInputStream().reset();
        serverMessage = vanet.receiveMessage();

        if (serverMessage == null || !serverMessage.isInitialized())
            return;

        if (serverMessage.getType() == VanetPb.ServerMessage.Type.INIT) {
            String[] res = {};
            vanet.init();
        } else if (serverMessage.getType() == VanetPb.ServerMessage.Type.QUERY_ALL) {
            Notifier.initQuery();
            Map<String, Video> videoMap = vanet.prepareAll();
            vanet.query(videoMap);
            Notifier.endQuery();

            //String[] projection = { MediaStore.Video.Media._ID};
            /*Cursor cursor = new CursorLoader(VADroid.getContext(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection,
                    null, // Return all rows
                    null, null).loadInBackground();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id);
                newVids.add(new Video(uri.getPath()));
            }*/

        } else if (serverMessage.getType() == VanetPb.ServerMessage.Type.QUERY_TAG){
            Notifier.initQuery();
            Map<String, Video> videoMap = vanet.prepareAll();
            vanet.queryTags(videoMap);
            Notifier.endQuery();
        }

        vanet.closeSocket();
    }

}
