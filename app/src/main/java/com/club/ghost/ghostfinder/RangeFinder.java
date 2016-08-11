package com.club.ghost.ghostfinder;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.app.ProgressDialog;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import android.graphics.Color;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import android.graphics.Paint;
import com.androidplot.Plot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.*;
import java.io.InputStream;
import java.io.FileOutputStream;

import java.util.Observable;
import java.util.Observer;
import java.text.DecimalFormat;

public class RangeFinder extends AppCompatActivity implements BeaconRangeCallback {

    // redraws a plot whenever an update is received:
    private class MyPlotUpdater implements Observer {
        Plot plot;

        public MyPlotUpdater(Plot plot) {
            this.plot = plot;
        }

        @Override
        public void update(Observable o, Object arg) {
            plot.redraw();
        }
    }

    private XYPlot dynamicPlot;
    private MyPlotUpdater plotUpdater;
    private DataSource data;
    private ProgressDialog progress;
    private BeaconEnabledApp app;
    private String name;
    private String file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        name = getIntent().getStringExtra("name");
        file = getIntent().getStringExtra("file");
        setContentView(R.layout.activity_rangefinder);

        app = (BeaconEnabledApp)getApplication();
        app.setRangeCallback(this);

        // get handles to our View defined in layout.xml:
        dynamicPlot = (XYPlot) findViewById(R.id.dynamicXYPlot);

        plotUpdater = new MyPlotUpdater(dynamicPlot);

        // only display whole numbers in domain labels
        dynamicPlot.getGraphWidget().setDomainValueFormat(new DecimalFormat("0"));

        // getInstance and position datasets:
        data = new DataSource();
        SampleDynamicSeries rawSeries = new SampleDynamicSeries(data, 0, "");
        SampleDynamicSeries smoothSeries = new SampleDynamicSeries(data, 1, "");

        LineAndPointFormatter formatter1 = new LineAndPointFormatter(
                Color.rgb(200, 0, 0), null, null, null);
        formatter1.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
        formatter1.getLinePaint().setStrokeWidth(10);
        dynamicPlot.addSeries(rawSeries,formatter1);

        LineAndPointFormatter formatter2 =
                new LineAndPointFormatter(Color.rgb(0, 200, 200), null, null, null);
        formatter2.getLinePaint().setStrokeWidth(10);
        formatter2.getLinePaint().setStrokeJoin(Paint.Join.ROUND);

        dynamicPlot.addSeries(smoothSeries, formatter2);

        // hook up the plotUpdater to the data model:
        data.addObserver(plotUpdater);

        // thin out domain tick labels so they dont overlap each other:
        dynamicPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        dynamicPlot.setDomainStepValue(5);

        dynamicPlot.setRangeStepMode(XYStepMode.INCREMENT_BY_VAL);
        dynamicPlot.setRangeStepValue(1);

        // uncomment this line to freeze the range boundaries:
        dynamicPlot.setRangeBoundaries(-10, -1, BoundaryMode.FIXED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        app.setRangeCallback(null);
    }

    protected void copyFile(InputStream in, File to) throws IOException {
        FileOutputStream out = new FileOutputStream(to);

        // buffered block copy
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    protected void onDownloadClick(View v) {
        // Copy data
        try {
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            //Log.i("LBLA", new File(downloadDir, file).getAbsolutePath());
            copyFile(getAssets().open(file), new File(downloadDir, file));
        } catch (IOException ex) {
            Log.i("ERR FILE",ex.getMessage());
        }

        progress=new ProgressDialog(this);
        progress.setMessage("Datenaufzeichnung läuft...");
        progress.setTitle("Psionisches Ereignis '" + name + "'");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setProgress(0);
        progress.setMax(100);
        progress.setCancelable(false);
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (progress.getProgress() <= progress.getMax()) {
                        Thread.sleep(150);
                        progress.incrementProgressBy(1);

                        //handle.sendMessage(handle.obtainMessage());
                        if (progress.getProgress() == progress.getMax()) {
                            progress.dismiss();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    @Override
    public void rangePing(double distance, double rssi) {
        final boolean isClose = distance < 0.04;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button but = (Button) findViewById(R.id.button);
                but.setVisibility(isClose ? View.VISIBLE : View.INVISIBLE);
                TextView txt = (TextView) findViewById(R.id.flash);
                txt.setVisibility((isClose && txt.getVisibility() == View.INVISIBLE) ? View.VISIBLE : View.INVISIBLE);
            }
        });
        data.add(rssi * 0.1, distance);
        data.notifier.notifyObservers();
    }

    class DataSource {
        // encapsulates management of the observers watching this datasource for update events:
        class MyObservable extends Observable {
            @Override
            public void notifyObservers() {
                setChanged();
                super.notifyObservers();
            }
        }

        private static final int BUFFER_SIZE = 40;
        private static final int SMOOTH_LEN = 2;
        private static final int SMOOTH2_LEN = 20;
        private ArrayList<Double> data = new ArrayList<Double>();
        private ArrayList<Double> dataLen = new ArrayList<Double>();
        private ArrayList<Double> dataSmooth = new ArrayList<Double>();
        private ArrayList<Double> dataSmooth2 = new ArrayList<Double>();

        public DataSource() {
            for (int i=0; i<BUFFER_SIZE; i++) {
                data.add(-10.0);
                dataSmooth.add(-10.0);
                dataSmooth2.add(-10.0);
                dataLen.add(0.0);
            }
        }

        public void add(double rssi, double len) {
            data.remove(0);
            data.add(rssi);

            dataLen.remove(0);
            dataLen.add(len);

            double xs = 0, xs2 = 0;
            dataSmooth.remove(0);
            for (int i=0; i<SMOOTH_LEN; i++)
                xs += data.get(BUFFER_SIZE-1-i);
            dataSmooth.add( xs / (double)SMOOTH_LEN);

            dataSmooth2.remove(0);
            for (int i=0; i<SMOOTH2_LEN; i++)
                xs2 += data.get(BUFFER_SIZE-1-i);
            dataSmooth2.add( xs2 / (double)SMOOTH2_LEN);
        }

        public MyObservable notifier = new MyObservable();

        public int getItemCount(int series) {
            return BUFFER_SIZE;
        }

        public Number getX(int series, int index) {
            return index;
        }

        public Number getY(int series, int index) {
            if (index >= data.size())
                return 0;
            if (series == 0)
                return dataSmooth.get(index);
            else
                return dataSmooth2.get(index);
        }

        public void addObserver(Observer observer) {
            notifier.addObserver(observer);
        }
    }

    class SampleDynamicSeries implements XYSeries {
        private DataSource datasource;
        private int seriesIndex;
        private String title;

        public SampleDynamicSeries(DataSource datasource, int seriesIndex, String title) {
            this.datasource = datasource;
            this.seriesIndex = seriesIndex;
            this.title = title;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public int size() {
            return datasource.getItemCount(seriesIndex);
        }

        @Override
        public Number getX(int index) {
            return datasource.getX(seriesIndex, index);
        }

        @Override
        public Number getY(int index) {
            return datasource.getY(seriesIndex, index);
        }
    }
}


