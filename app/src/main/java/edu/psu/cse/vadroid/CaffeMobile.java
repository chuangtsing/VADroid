package edu.psu.cse.vadroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import org.opencv.core.Mat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CaffeMobile {
    private static final ArrayList<String> classes = new ArrayList<String>();
    private static boolean initialized = false;
    private ExecutorService extractService, classifyService;

    public static CaffeMobile caffeMobile = new CaffeMobile();
    public static final String RES_PATH = "/storage/emulated/0/vadroid/models/";
    public static final String TMP_PATH = "/storage/emulated/0/vadroid/tmp/";
    public static final String MODEL_NAME = "model.prototxt";
    public static final String WEIGHTS_NAME = "weights.caffemodel";
    public static final String MEAN_NAME = "mean.binaryproto";
    public static final String SYNSET_NAME = "synset.txt";
    public static final String VIDEOS_DIR = "/storage/emulated/0/DCIM/Camera/";
    public static final String DEFAULT_MODEL_PATH = RES_PATH + MODEL_NAME;
    public static final String DEFAULT_WEIGHTS_PATH = RES_PATH + WEIGHTS_NAME;
    public static final String DEFAULT_MEAN_PATH = RES_PATH + MEAN_NAME;
    public static final String SYNSET_PATH = RES_PATH + SYNSET_NAME;
    private static final String EXTRACT_PATH = "/storage/emulated/0/vadroid/extracted/";
    private static final double db_fps = 1.0;
    private int kResults;

    private Initializer initializer = new Initializer(new InitCallable());
    private Predictor predictor;
    private Semaphore queuedFrames = new Semaphore(100, true);


    public enum ProcessMessage {
        FRAME,
        FINISH;
    }

    public enum ProcessMode {
        EXTRACT_ONLY,
        EXTRACT_AND_CLASSIFY;
    }

    public void setkResults(int kResults) {
        this.kResults = kResults;
    }


    public static String getClass(int i) {
        if (!initialized) {
            CaffeMobile caffe = new CaffeMobile();
            caffe.startInit();
            if (!caffe.getInit())
                return null;
        }
        String c = null;
        try {
            c = classes.get(i);
        } catch (IndexOutOfBoundsException e) {
            Log.e(VADroidUtils.TAG, "Caffe getClass index out of bounds");
        }
        return c;
    }

    public ArrayList<String> predictImage(String imgPath, int N) {
        if (!initialized) {
            startInit();
            if (getInit())
                return null;
        }
        int[] top_k = predImage(imgPath, N);
        ArrayList<String> pred = new ArrayList<>();
        for (int i : top_k) {
            pred.add(CaffeMobile.getClass(i));
        }
        return pred;
    }


    public int[] predictVideoInt(Video vid, String dir, String ffmpegPath, int N) {
        startInit();
        return predVideo(vid.path, dir, vid.fps, ffmpegPath, N);
    }

    public String getExtractionDir(Video vid) {
        String basePath = EXTRACT_PATH + vid.name.split("\\.")[0];
        String path = basePath;
        File dir = new File(basePath);
        int i = 1;
        while (dir.exists()) {
            path = basePath + "_" + i;
            dir = new File(path);
            i++;
        }
        path += "/";
        path.replace(' ', '_');

        VADroidUtils.buildDir(path);
        return path;
    }

    public void startInit() {

        if (initialized == true)
            return;

        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(initializer);
    }

    public boolean getInit() {
        if (initializer == null)
            return false;
        if (initialized == true)
            return true;
        try {
            return initializer.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isInit() {
        return initialized;
    }


    private static native int loadModel(String model, String weights, String mean);

    private static native int[] predImage(String imgPath, int N);

    private static native int[] predVideo(String vidPath, String extractionPath, double fps, String ffmpegPath, int N);

    private static native float[] predImageMat(long img, int topk);

    private class Initializer extends FutureTask<Boolean> {

        public Initializer(Callable<Boolean> callable) {
            super(callable);
        }

        @Override
        protected void done() {
            try {
                initialized = get();
                return;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            initialized = false;
        }
    }


    private class InitCallable implements Callable<Boolean> {
        @Override
        public Boolean call() throws Exception {
            try {
                System.loadLibrary("caffe");
                System.loadLibrary("caffe_jni");
            } catch (Exception e) {
                Log.e(VADroidUtils.TAG, "init failed: could not load caffe mobile libraries");
                return false;
            }
            Context context = VADroid.getContext();
            SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.package_name), Context.MODE_PRIVATE);

            String modelPath = prefs.getString("MODEL", DEFAULT_MODEL_PATH);
            String weightsPath = prefs.getString("WEIGHTS", DEFAULT_WEIGHTS_PATH);
            String meanPath = prefs.getString("MEAN", DEFAULT_MEAN_PATH);


            File model = new File(modelPath);
            File weights = new File(weightsPath);
            File mean = new File(meanPath);
            if (model.exists() && weights.exists() && mean.exists())
                loadModel(modelPath, weightsPath, meanPath);
            else {
                Log.e(VADroidUtils.TAG, "init failed: data files not found");
                return false;
            }

            try {
                InputStream is = new FileInputStream(SYNSET_PATH);
                Scanner sc = new Scanner(is);
                while (sc.hasNextLine()) {
                    final String temp = sc.nextLine();
                    classes.add(temp.substring(temp.indexOf(" ") + 1));
                }
                if (classes.size() == 0)
                    throw new IOException("Synset file of size 0");
            } catch (IOException e) {
                Log.e(VADroidUtils.TAG, "init failed: failed to read synset file");
                //return false;
            }
            return true;
        }
    }

    private class Predictor extends FutureTask<int[]> {

        public Predictor(Callable<int[]> callable) {
            super(callable);
        }
    }


    public void addVideoProcessAll(VANet vanet, ExecutorService sendService, Video vid, ProcessMode mode) {
        if (extractService == null)
            extractService = Executors.newSingleThreadExecutor();
        if (classifyService == null)
            classifyService = Executors.newSingleThreadExecutor();

        extractService.submit(new AsyncExtractor(vanet, sendService, vid, mode));
    }

    public void waitAll() {
        if (extractService != null) {
            extractService.shutdown();
            try {
                extractService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            extractService = null;
        }

        if (classifyService != null) {
            classifyService.shutdown();
            try {
                classifyService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            classifyService = null;
        }
    }

    private class AsyncExtractor implements Callable<Void> {

        private VANet vanet;
        private Video vid;
        private ExecutorService sendService;
        private Extractor extractor;
        private AsyncClassifier classifier;
        private ProcessMode mode;
        private Vector<Mat> vecMat;
        private BlockingQueue<Pair<ProcessMessage, Mat>> extractQueue, classifyQueue, sendQueue;

        public AsyncExtractor(VANet vanet, ExecutorService sendService, Video vid, ProcessMode mode) {
            this.vanet = vanet;
            this.vid = vid;
            this.sendService = sendService;
            this.mode = mode;
            this.extractor = new Extractor(vid);
            this.classifier = new AsyncClassifier(vanet, sendService, vid);
        }

        @Override
        public Void call() throws Exception {
            long processtime;
            long extractime;
            SortedMap<String, Integer> mapText = new TreeMap<String, Integer>();
            int n_frame_extracted = 0;
            int n_completed = 0;
            int frame = 0;


            if (mode == ProcessMode.EXTRACT_AND_CLASSIFY) {
                classifier = new AsyncClassifier(vanet, sendService, vid);
                try {
                    classifyService.submit(classifier);
                    classifyQueue = classifier.getQueue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            extractQueue = new LinkedBlockingQueue<>();

            new Thread() {
                public void run() {
                    try {
                        extractor.extractMpegFrames(extractQueue, queuedFrames);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();

            while (true) {
                Pair<ProcessMessage, Mat> pair = extractQueue.take();
                classifyQueue.put(pair);
                if (pair.first == ProcessMessage.FINISH) {
                    break;
                }
            }

            return null;
        }


    }

    private class AsyncClassifier implements Callable<Void> {
        private VANet vanet;
        private ExecutorService sendService;
        private Video vid;
        private Vector<float[]> vec;
        private BlockingQueue<Pair<ProcessMessage, Mat>> queue;

        public AsyncClassifier(VANet vanet, ExecutorService sendService, Video vid) {
            this.vanet = vanet;
            this.sendService = sendService;
            this.vid = vid;
            vec = new Vector<>();
            this.queue = new LinkedBlockingQueue<>();
        }

        @Override
        public Void call() throws Exception {
            if (!CaffeMobile.this.getInit()) {
                Notifier.endCaffeInit();
                return null;
            }
            Pair pair;
            Notifier.setProcess(Notifier.Location.LOCAL, VideoProcess.ProcessState.CLASSIFYING, 0);
            while (true) {
                pair = queue.take();
                if (pair.first == ProcessMessage.FINISH) {
                    //finished = true;
                    break;
                } else if (pair.first == ProcessMessage.FRAME) {
                    if (!CaffeMobile.this.getInit()) {
                        Notifier.endCaffeInit();
                        return null;
                    }
                    Notifier.endCaffeInit();
                    Mat mat = (Mat) pair.second;
                    if (mat != null) {
                        float[] result = predImageMat(mat.getNativeObjAddr(), kResults);
                        vec.add(result);
                    }
                }
            }


            float[] total = new float[kResults];
            Vector<Pair<Integer, Float>> sorted = new Vector<>();
            for (float[] frame : vec) {
                for (int i = 0; i < kResults; i++) {
                    total[i] += frame[i];
                }
            }

            for (int i = 0; i < kResults; i++) {
                sorted.add(new Pair<>(i, total[i]));
            }
            Collections.sort(sorted, new Comparator<Pair<Integer, Float>>() {
                @Override
                public int compare(Pair<Integer, Float> floatIntegerPair, Pair<Integer, Float> t1) {
                    return Float.compare(floatIntegerPair.second, t1.second);
                }
            });

            vid.tags.clear();
            for (Pair<Integer, Float> p : sorted) {
                vid.tags.add(p.first);
            }

            sendService.submit(vanet.getAsyncSender(VANet.UploadTask.VIDEO_INFO, vid));
            queuedFrames.release(vec.size());
            Notifier.setProcess(Notifier.Location.LOCAL, VideoProcess.ProcessState.FINISHED, 0);
            return null;
        }

        public BlockingQueue<Pair<ProcessMessage, Mat>> getQueue() {
            return this.queue;
        }

    }

}