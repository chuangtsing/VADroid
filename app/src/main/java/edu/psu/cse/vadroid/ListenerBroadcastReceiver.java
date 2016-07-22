package edu.psu.cse.vadroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ListenerBroadcastReceiver extends BroadcastReceiver {
    public ListenerBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, ListenerService.class));
    }
}
