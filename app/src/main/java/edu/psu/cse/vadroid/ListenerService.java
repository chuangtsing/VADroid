package edu.psu.cse.vadroid;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class ListenerService extends Service {
    static ListenerService service = null;
    static int PORT = 38300;
    static int PING_PORT = 1900;
    static int BCAST_BUFFER_SIZE = 1024;
    Context context;
    ServerSocket server = null;
    DatagramSocket pingSocket = null;
    Socket socket;

    @Override
    public void onCreate() {
        super.onCreate();
        context = VADroid.getContext();
        service = this;
        new ListenerThread().start();
        Notifier.setRunning();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        try {
            server.close();
        } catch (Exception e) {}
        service = null;
        super.onDestroy();
        Notifier.clearNotification();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public class ListenerThread extends Thread {

        @Override
        public void run() {
           /* try {
                WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                DhcpInfo dhcp = wifi.getDhcpInfo();
                // handle null somehow

                int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
                byte[] quads = new byte[4];
                for (int k = 0; k < 4; k++)
                    quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
                //pingSocket = new DatagramSocket(PING_PORT, InetAddress.getByAddress(quads));
                pingSocket = new DatagramSocket(PING_PORT, InetAddress.getByName("10.100.1.255"));
                pingSocket.setBroadcast(true);
            } catch (SocketException e) {
                e.printStackTrace();
                Notifier.setError(e.getMessage());
                return;
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Notifier.setError(e.getMessage());
                pingSocket.close();
                return;
            }

            if (pingSocket == null) {
                Notifier.setError("Error opening ping socket");
                pingSocket.close();
                return;
            }


            while (!isInterrupted()) {
                VanetPb.ServerMessage smsg = null;
                try {
                    byte[] sizeBuf = new byte[4];
                    DatagramPacket sizePacket = new DatagramPacket(sizeBuf, 4);
                    pingSocket.receive(sizePacket);
                    int size = java.nio.ByteBuffer.wrap(sizeBuf).getInt();
                    byte[] buffer = new byte[size];
                    DatagramPacket packet = new DatagramPacket(buffer, size);
                    pingSocket.receive(packet);
                    smsg = VanetPb.ServerMessage.parseFrom(buffer);
                } catch (SocketException e) {
                    e.printStackTrace();
                    Notifier.setError(e.getMessage());
                    pingSocket.close();
                    return;
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    Notifier.setError(e.getMessage());
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    Notifier.setError(e.getMessage());
                    pingSocket.close();
                    return;
                }

                if (smsg != null && smsg.getType() == VanetPb.ServerMessage.Type.PING && smsg.hasIp()) {
                    String ip = smsg.getIp();
                    try {
                        socket = new Socket(ip, PORT);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Notifier.setError(e.getMessage());
                        pingSocket.close();
                        return;
                    }
                    if (socket == null) {
                        Notifier.setError("Error opening socket");
                        pingSocket.close();
                        return;
                    }
                    VANet vanet = new VANet(socket, context);
                    VanetPb.ClientMessage.Builder builder = VanetPb.ClientMessage.newBuilder();
                    builder.setType(VanetPb.ClientMessage.Type.PING);
                    vanet.sendMessage(builder);
                    new ConnectionThread(vanet, this).start();
                }


            }*/



            Socket client = null;
            try {
                server = new ServerSocket(PORT);
            } catch (Exception e) {
                //Notifier.setError(e.getMessage());
                return;
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    client = server.accept();
                    new ConnectionThread(client).start();


                } catch (IOException e) {
                    return;
                }
            }
        }

    }
}
