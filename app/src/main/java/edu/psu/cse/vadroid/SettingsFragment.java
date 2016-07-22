package edu.psu.cse.vadroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import java.io.File;


public class SettingsFragment extends Fragment {
    private DirectoryChooserFragment mDialog;
    private Button btnService;
    private Button btnSelect;
    private Button btnModel, btnWeights, btnMean;
    private TextView txtDir;
    private MainActivity mainActivity;
    private int select;

    @Override
    public void onAttach(Activity activity) {
        mainActivity = (MainActivity) activity;
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        setRetainInstance(true);
        btnSelect = (Button) view.findViewById(R.id.btnSelect);
        btnModel = (Button) view.findViewById(R.id.btnModel);
        btnWeights = (Button) view.findViewById(R.id.btnWeights);
        btnMean = (Button) view.findViewById(R.id.btnMean);
        btnService = (Button) view.findViewById(R.id.btnService);
        final SharedPreferences prefs = VADroid.getContext().getSharedPreferences(getString(R.string.package_name), Context.MODE_PRIVATE);
        String directory = prefs.getString(getString(R.string.pref_directory), null);
        String model = prefs.getString("MODEL", CaffeMobile.DEFAULT_MODEL_PATH);
        String weights = prefs.getString("WEIGHTS", CaffeMobile.DEFAULT_WEIGHTS_PATH);
        String mean = prefs.getString("MEAN", CaffeMobile.DEFAULT_MEAN_PATH);
        txtDir = (TextView) view.findViewById(R.id.txtDir);
        if (directory == null) {
            directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/Camera";
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getString(R.string.pref_directory), directory);
            editor.commit();
        }
        txtDir.setText(directory);
        mDialog = DirectoryChooserFragment.newInstance("Choose a video directory.", null);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.showDirectoryChooser(SettingsFragment.this);
            }
        };

        final FileChooser.FileSelectedListener fileSelectedListener = new FileChooser.FileSelectedListener() {
            @Override
            public void fileSelected(File file) {
                        SharedPreferences.Editor editor = VADroid.getContext().getSharedPreferences(getString(R.string.package_name), Context.MODE_PRIVATE).edit();
                        String mode = "";
                        switch (select) {
                            case 1:
                                mode = "MODEL";
                                break;
                            case 2:
                                mode = "WEIGHTS";
                                break;
                            case 3:
                                mode = "MEAN";
                                break;
                        }
                        if (!mode.isEmpty()) {
                            editor.putString(mode, file.getAbsolutePath());
                            editor.commit();
                        }
                    }
            };

        View.OnClickListener listener2 = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mode = "";
                String def = "/";
                if (v == btnModel) {
                    select = 1;
                    mode = "MODEL";
                    def = CaffeMobile.DEFAULT_MODEL_PATH;
                }
                else if (v == btnWeights) {
                    select = 2;
                    mode = "WEIGHTS";
                    def = CaffeMobile.DEFAULT_WEIGHTS_PATH;
                }
                else if (v == btnMean) {
                    select = 3;
                    mode = "MEAN";
                    def = CaffeMobile.DEFAULT_MEAN_PATH;
                }
                new FileChooser(mainActivity, prefs.getString(mode, def)).setFileListener(fileSelectedListener).showDialog();
            }
        };

        btnSelect.setOnClickListener(listener);
        btnModel.setOnClickListener(listener2);
        btnWeights.setOnClickListener(listener2);
        btnMean.setOnClickListener(listener2);
        btnService.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Context context = VADroid.getContext();
                if (ListenerService.service != null) {
                    context.stopService(new Intent(context, ListenerService.class));
                    btnService.setText(getString(R.string.btn_start));
                } else {
                    context.startService(new Intent(context, ListenerService.class));
                    btnService.setText(getString(R.string.btn_stop));
                }
            }
        });
        return view;
    }

    public void directoryCallback(String dir) {
        SharedPreferences.Editor editor = VADroid.getContext().getSharedPreferences(getString(R.string.package_name), Context.MODE_PRIVATE).edit();
        String mode = "";

        txtDir.setText(dir);

    }
}