package edu.psu.cse.vadroid;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import java.util.ArrayList;

public class Settings extends Activity implements DirectoryChooserFragment.OnFragmentInteractionListener {
    private Button btnSelect;
    private Button btnService;
    private TextView txtDir;
    private DirectoryChooserFragment mDialog;
    private String directory;
    private ProgressDialog progDialog;
    private TextView textView;
    private int predTime;
    private int extrTime;
    private TextView txtExtr;
    private TextView txtPred;

    private static final String BIN_FOLDER_PATH = "/data/data/edu.psu.cse.vadroid/bin/";
    private static final String OLD_BIN_FOLDER_PATH = "/data/data/edu.psu.cse.vadroid/assets/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        startService(new Intent(this, ListenerService.class));
        btnSelect = (Button) findViewById(R.id.btnSelect);
        btnService = (Button) findViewById(R.id.btnService);
        txtDir = (TextView) findViewById(R.id.txtDir);
        mDialog = DirectoryChooserFragment.newInstance("DialogSample", null);
        txtPred = txtPred;
        TextView v = txtPred;

        //Loader loader = new Loader();
        //loader.execute();

        btnSelect.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                mDialog.show(getFragmentManager(), null);
            }
        });

        btnService.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (ListenerService.service != null) {
                    stopService(new Intent(Settings.this, ListenerService.class));
                    btnService.setText(getString(R.string.btn_start));
                } else {
                    startService(new Intent(Settings.this, ListenerService.class));
                    btnService.setText(getString(R.string.btn_stop));
                }
            }
        });
        /*DirChooserFragmentSample frag = new DirChooserFragmentSample();
        frag.showDialog(0);*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*Uri selectedVideo = data.getData();
        String[] filePathColumn = {MediaStore.Video.Media.DATA};
        Cursor cursor = getContentResolver().query(
                selectedVideo, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String path = cursor.getString(columnIndex);
        Video vid = new Video(path);
        //((TextView) findViewById(R.id.textView)).setText("Frame rate: " + vid.fps);
        cursor.close();
        //new Predictor().execute(vid);
        //CaffeMobile caffe = new CaffeMobile(getApplicationContext(), MODEL_PATH, WEIGHTS_PATH, MEAN_PATH);*/

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSelectDirectory(String s) {
        directory = s;
        txtDir.setText(s);
        mDialog.dismiss();
    }

    @Override
    public void onCancelChooser() {
        mDialog.dismiss();
    }

    /*private class Loader extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Ffmpeg.init(OLD_BIN_FOLDER_PATH, BIN_FOLDER_PATH, getApplicationContext());
            //CaffeMobile.init();
            return null;
        }

        @Override
        protected void onPreExecute() {
            progDialog = ProgressDialog.show(Settings.this,
                    "Initializing...", "Please wait.", true);
        }

        @Override
        protected void onPostExecute(Void param) {
            progDialog.dismiss();

        }
    }*/

    /*private class Predictor extends AsyncTask<Video, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(Video... params) {
            Notifier.setProgress(0, params[0].totalFrames, "Extracting frames.");
            String dir = CaffeMobile.getExtractionDir(params[0]);
            new ExtractionProgress(dir, params[0].totalFrames).run();
            long t1 = System.nanoTime();
            Ffmpeg.extract(params[0], dir);
            long t2 = System.nanoTime();
            Notifier.setIndeterminateProgress("Predicting.");
            long t3 = System.nanoTime();
            //int[] pred = CaffeMobile.predictVideoInt(params[0], dir, BIN_FOLDER_PATH, 13);
            long t4 = System.nanoTime();
            extrTime =(int) ((t2 - t1) / 1000000);
            predTime =(int) ((t4 - t3) / 1000000);
            return new ArrayList<String>();
        }

        @Override
        protected void onPostExecute(ArrayList<String> pred) {
            Notifier.endProgress();
            textView.setText(TextUtils.join(", ", pred));
            txtExtr.setText(getString(R.string.extr_time, extrTime));
            txtPred.setText(getString(R.string.pred_time, predTime));

        }
    }*/

    private class ExtractionProgress implements Runnable {
        private String dir;
        private int frames;

        public ExtractionProgress(String dir, int frames) {
            this.dir = dir;
            this.frames = frames;
        }

        @Override
        public void run() {

            FileObserver observer = new FileObserver(dir) { // set up a file observer to watch this directory on sd card
                int i = 0;
                @Override
                public void onEvent(int event, String file) {
                    if (event == FileObserver.CREATE) {
                        i++;
                        Notifier.updateProcess(Notifier.Location.LOCAL, VideoProcess.ProcessState.EXTRACTING, i);
                    }
                }
            };
            observer.startWatching();
        }
    }

}
