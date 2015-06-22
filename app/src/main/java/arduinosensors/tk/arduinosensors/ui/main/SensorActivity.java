package arduinosensors.tk.arduinosensors.ui.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;


import arduinosensors.tk.arduinosensors.R;
import arduinosensors.tk.arduinosensors.model.DbHelper;
import butterknife.ButterKnife;
import butterknife.InjectView;


public class SensorActivity extends Activity implements SensorView {



    Handler bluetoothIn;
    final int handlerState = 0;        				 //used to identify handler message
    SensorPresenter presenter;

    @InjectView(R.id.txtErrorText)
    TextView textViewError;

    @InjectView(R.id.mSensorXYPlot)
    XYPlot dynamicPlot;

    private MyPlotUpdater plotUpdater;
    SensorDynamicXYDatasource data;
    private Thread myThread;
    DbHelper dbHelper;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_sensor);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ButterKnife.inject(this);
        presenter = new SensorPresenterImpl(this, this);
        presenter.onCreate();

        bluetoothIn = new Handler() {
               public void handleMessage(android.os.Message msg) {
                        if (msg.what == handlerState) {
                                String readMessage = (String) msg.obj;
                                textViewError.setText(readMessage);
                        }
                    }
        };


        dbHelper = new DbHelper(this);

        plotUpdater = new MyPlotUpdater(dynamicPlot);

        // only display whole numbers in domain labels
        dynamicPlot.getGraphWidget().setDomainValueFormat(new DecimalFormat("0"));

        // getInstance and position datasets:
        data = new SensorDynamicXYDatasource();
        SensorDynamicSeries sine1Series = new SensorDynamicSeries(data, 0, "Sensor 1");
        SensorDynamicSeries sine2Series = new SensorDynamicSeries(data, 1, "Sensor 2");

        LineAndPointFormatter formatter1 = new LineAndPointFormatter(Color.rgb(200, 0, 0), null, null, null);
        formatter1.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
        formatter1.getLinePaint().setStrokeWidth(3);
        dynamicPlot.addSeries(sine1Series,formatter1);

        LineAndPointFormatter formatter2 = new LineAndPointFormatter(Color.rgb(0, 0, 200), null, null, null);
        formatter2.getLinePaint().setStrokeWidth(3);
        formatter2.getLinePaint().setStrokeJoin(Paint.Join.ROUND);

        //formatter2.getFillPaint().setAlpha(220);
        dynamicPlot.addSeries(sine2Series, formatter2);

        // hook up the plotUpdater to the data model:
        data.addObserver(plotUpdater);

        // thin out domain tick labels so they dont overlap each other:
        //dynamicPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        //dynamicPlot.setDomainStepValue(5);

        dynamicPlot.setRangeStepMode(XYStepMode.INCREMENT_BY_VAL);
        dynamicPlot.setRangeStepValue(10);

        dynamicPlot.setRangeValueFormat(new DecimalFormat("###.###"));

        // uncomment this line to freeze the range boundaries:
        dynamicPlot.setRangeBoundaries(0, 0, BoundaryMode.AUTO);

        // create a dash effect for domain and range grid lines:
        DashPathEffect dashFx = new DashPathEffect(
                new float[] {PixelUtils.dpToPix(3), PixelUtils.dpToPix(3)}, 0);
        dynamicPlot.getGraphWidget().getDomainGridLinePaint().setPathEffect(dashFx);
        dynamicPlot.getGraphWidget().getRangeGridLinePaint().setPathEffect(dashFx);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sensor, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.onResume(getIntent());
        myThread = new Thread(data);
        myThread.start();
    }

    @Override
    public void onPause() {
        data.stopThread();
        super.onPause();
        presenter.onPause();


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        if(id == R.id.menu_deleteDB){
            showCleanDBAlert();
            return true;
        }
        if(id == R.id.menu_exportDB){
            return presenter.exportDB();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void showMessage(String message) {
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
    }

    private void showCleanDBAlert(){
        AlertDialog.Builder ad;
        ad = new AlertDialog.Builder(this);
        ad.setTitle("Удаление данных");  // заголовок
        ad.setMessage("Внимание, удаленные данные нельзя будет восстановить."); // сообщение
        ad.setPositiveButton("Удалить!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                presenter.cleanDB();
            }
        });
        ad.setNegativeButton("Я передумал", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                showMessage("Возможно вы правы");

            }
        });
        ad.setCancelable(true);
        ad.show();
    }

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

    class SensorDynamicXYDatasource implements Runnable {
        private static final int SENSOR_SIZE = 600;
        public static final int SENSOR1 = 0;
        public static final int SENSOR2 = 1;
        // encapsulates management of the observers watching this datasource for update events:
        class MyObservable extends Observable {
            @Override
            public void notifyObservers() {
                setChanged();
                super.notifyObservers();
            }
        }

        private MyObservable notifier;
        private boolean keepRunning = false;

        LinkedList <Double> qS1;
        LinkedList <Double> qS2;
        private final Object lock = new Object();

        {
            notifier = new MyObservable();
            qS1=new LinkedList<Double>();
            qS2=new LinkedList<Double>();
            for(int i=0; i< SENSOR_SIZE; i++){
            qS1.add(0d);
            qS2.add(0d);
        }
        }

        public void stopThread() {
            keepRunning = false;
        }

        //@Override
        public void run() {
            try {
                keepRunning = true;
                boolean isRising = true;
                while (keepRunning) {

                    Thread.sleep(1000); // decrease or remove to speed up the refresh rate.
                    synchronized(lock) {
                        qS1 = (LinkedList<Double>) presenter.getSensorValue("s1");
                    }
                    synchronized(lock) {
                        qS2 = (LinkedList<Double>) presenter.getSensorValue("s2");
                    }
                    notifier.notifyObservers();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public int getItemCount(int series) {
            return SENSOR_SIZE;
        }

        public Number getX(int series, int index) {
            if (index >= SENSOR_SIZE) {
                throw new IllegalArgumentException();
            }
            return index;
        }

        public Number getY(int series, int index) {
            Number val;
            if (index >= SENSOR_SIZE) {
                throw new IllegalArgumentException();
            }
            switch (series) {
                case SENSOR1:
                    try{
                    val = (Number) qS1.get(index);
                    if(val != null){return  val;}
                    else{ return  0;}}
                    catch (Error e){
                        return 0;
                    }
                    catch (Exception e) {
                        return 0;
                    }
                case SENSOR2:
                    try{
                    val = (Number) qS2.get(index);
                    if(val != null){return  val;}
                    else{ return  0;}}
                    catch (Error e){
                        return 0;
                    }
                    catch (Exception e) {
                        return 0;
                    }

                default:
                    throw new IllegalArgumentException();
            }
        }

        public void addObserver(Observer observer) {
            notifier.addObserver(observer);
        }

        public void removeObserver(Observer observer) {
            notifier.deleteObserver(observer);
        }

    }

    class SensorDynamicSeries implements XYSeries {
        private SensorDynamicXYDatasource datasource;
        private int seriesIndex;
        private String title;

        public SensorDynamicSeries(SensorDynamicXYDatasource datasource, int seriesIndex, String title) {
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
