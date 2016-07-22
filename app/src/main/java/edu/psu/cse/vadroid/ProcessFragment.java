package edu.psu.cse.vadroid;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;


public class ProcessFragment extends Fragment {
    private Context context;
    private TextView textViewQuery;
    private ProgressBar progressBar;
    private RelativeLayout layoutProcessLists;
    private LinearLayout layoutQueryStatus;
    private LinearLayout layoutLocalProcessInit;
    private static Handler handler;
    private static Notifier.MessageWhat[] whats = Notifier.MessageWhat.values();
    private static Notifier.Location[] locations = Notifier.Location.values();
    ViewSwitcher viewSwitcherProcess, viewSwitcherLocalProcess;
    private ProcessListAdapter localAdapter, serverAdapter;
    private ListView localList, serverList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = container.getContext();
        View view = inflater.inflate(R.layout.fragment_process, container, false);
        textViewQuery = (TextView) view.findViewById(R.id.textViewQuery);
        textViewQuery.setText("No query in progress");
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        localList = (ListView) view.findViewById(R.id.listViewLocal);
        serverList = (ListView) view.findViewById(R.id.listViewServer);
        layoutProcessLists = (RelativeLayout) view.findViewById(R.id.layoutProcessLists);
        layoutQueryStatus = (LinearLayout) view.findViewById(R.id.layoutQueryStatus);
        layoutLocalProcessInit = (LinearLayout) view.findViewById(R.id.layoutLocalProcessInit);
        viewSwitcherProcess = (ViewSwitcher) view.findViewById(R.id.viewSwitcherProcess);
        viewSwitcherLocalProcess = (ViewSwitcher) view.findViewById(R.id.viewSwitcherLocalProcess);
        handler = new Handler(new HandlerCallback());
        Notifier.registerHandler(handler);
        return view;
    }

    @Override
    public void onDestroy() {
        Notifier.unregisterHandler();
        super.onDestroy();
    }

    public void initQuery() {
        textViewQuery.setText("Query in progress");
        progressBar.setVisibility(View.VISIBLE);
        viewSwitcherProcess.setDisplayedChild(0);
    }

    public void beginCaffeInit() {
        viewSwitcherLocalProcess.setDisplayedChild(1);
    }

    public void endCaffeInit() {
        viewSwitcherLocalProcess.setDisplayedChild(0);
    }

    public void beginProcessing() {
        localAdapter = new ProcessListAdapter(context, localList, Notifier.Location.LOCAL);
        serverAdapter = new ProcessListAdapter(context, serverList, Notifier.Location.SERVER);

        localList.setAdapter(localAdapter);
        serverList.setAdapter(serverAdapter);

        localAdapter.notifyDataSetChanged();
        serverAdapter.notifyDataSetChanged();

        viewSwitcherProcess.setDisplayedChild(1);
    }

    public void setProcess(Notifier.Location location, int position) {
        switch (location) {
            case LOCAL:
                localAdapter.setProcessStatus(position);
                break;
            case SERVER:
                serverAdapter.setProcessStatus(position);
                break;
        }
    }

    public void updateProcess(Notifier.Location location, int position) {
        switch (location) {
            case LOCAL:
                localAdapter.updateProgress(position);
                break;
            case SERVER:
                serverAdapter.updateProgress(position);
                break;
        }
    }

    public void endQuery() {
        textViewQuery.setText("No query in progress");
        progressBar.setVisibility(View.INVISIBLE);
        //viewSwitcherProcess.setDisplayedChild(0);
    }

    class HandlerCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            switch (whats[msg.what]) {
                case INIT_QUERY:
                    initQuery();
                    break;
                case BEGIN_PROCESSING:
                    beginProcessing();
                    break;
                case BEGIN_CAFFE_INIT:
                    beginCaffeInit();
                    break;
                case END_CAFFE_INIT:
                    endCaffeInit();
                    break;
                case SET_QUERY_PROCESS:
                    setProcess(locations[msg.arg1], msg.arg2);
                    break;
                case UPDATE_QUERY_PROCESS:
                    updateProcess(locations[msg.arg1], msg.arg2);
                    break;
                case POST_QUERY_RESULTS:
                    break;
                case END_QUERY:
                    endQuery();
                    break;
            }
            return true;
        }
    }
}
