package edu.psu.cse.vadroid;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zack on 8/19/15.
 */
public class ProcessListAdapter extends ArrayAdapter<VideoProcess> {
    private Context context;
    //private List<VideoProcess> objects;
    //private List<View> items;
    private ListView parent;
    private Notifier.Location location;

    public ProcessListAdapter(Context context, ListView parent, Notifier.Location location) {
        super(context, R.layout.process_list_item, Notifier.getList(location));
        this.parent = parent;
        this.location = location;
        //items = new ArrayList<View>();
        this.context = context;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = layoutInflater.inflate(R.layout.process_list_item, parent, false);
        //items.add(position, view);

        final TextView textViewName = (TextView) view.findViewById(R.id.textViewProcessItemName);
        textViewName.setText(Notifier.getList(location).get(position).getVideo().name);

        setProcess(view, position, true);

        return view;
    }

    private void setProcess(View view, int position, boolean init) {
        if (!init && (position < parent.getFirstVisiblePosition() || position > parent.getLastVisiblePosition()))
            return;
        final TextView textViewStatus = (TextView) view.findViewById(R.id.textViewProcessItemStatus);
        final ViewSwitcher viewSwitcher = (ViewSwitcher) view.findViewById((R.id.viewSwitcherProcessItem));
        final ImageView imageView = (ImageView) view.findViewById(R.id.imageViewProcessItem);
        final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressBarProcessItem);
        VideoProcess videoProcess = Notifier.getList(location).get(position);

        switch(videoProcess.state) {
            case UNPROCESSED:
                viewSwitcher.setVisibility(View.INVISIBLE);
                textViewStatus.setText("");
                break;
            case EXTRACTING:
                viewSwitcher.setVisibility(View.VISIBLE);
                textViewStatus.setText("Extracting frame " + videoProcess.progress + "/" + videoProcess.maxProgress);
                if (viewSwitcher.getCurrentView() != progressBar)
                    viewSwitcher.showPrevious();
                break;
            case CLASSIFYING:
                viewSwitcher.setVisibility(View.VISIBLE);
                textViewStatus.setText("Processing");
                if (viewSwitcher.getCurrentView() != progressBar)
                    viewSwitcher.showPrevious();
                break;
            case WAITING:
                viewSwitcher.setVisibility(View.VISIBLE);
                textViewStatus.setText("Waiting for request from server");
                if (viewSwitcher.getCurrentView() != imageView)
                    viewSwitcher.showNext();
                imageView.setImageResource(R.drawable.ic_query_builder_black_24dp);
                break;
            case UPLOADING:
                viewSwitcher.setVisibility(View.VISIBLE);
                textViewStatus.setText("Uploading " + (int) (100*videoProcess.progress/videoProcess.maxProgress) + "%");
                if (viewSwitcher.getCurrentView() != progressBar)
                    viewSwitcher.showPrevious();
                break;
            case FINISHED:
                viewSwitcher.setVisibility(View.VISIBLE);
                textViewStatus.setText("");
                if (viewSwitcher.getCurrentView() != imageView)
                    viewSwitcher.showNext();
                imageView.setImageResource(R.drawable.ic_check_circle_green_24dp);
                break;
        }
    }

    public void setProcessStatus(int position) {
        //final View view = items.get(position)
        final View view = parent.getChildAt(position);
        if (view == null)
            return;
        setProcess(view, position, false);
    }
    //////////////// MAKE HASH MAP OF VIEWS
    public void updateProgress(int position) {
        //final View view = items.get(position);
        if (position < parent.getFirstVisiblePosition() || position > parent.getLastVisiblePosition())
            return;
        final View view = parent.getChildAt(position);
        final TextView textViewStatus = (TextView) view.findViewById(R.id.textViewProcessItemStatus);
        VideoProcess videoProcess = Notifier.getList(location).get(position);
        switch(videoProcess.state) {
            case EXTRACTING:
                textViewStatus.setText("Extracting frame " + videoProcess.progress + "/" + videoProcess.maxProgress);
                break;
            case UPLOADING:
                textViewStatus.setText("Uploading " + (int) (100*videoProcess.progress/videoProcess.maxProgress) + "%");
                break;
        }
    }


}
