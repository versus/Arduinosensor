package arduinosensors.tk.arduinosensors.ui.main;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import arduinosensors.tk.arduinosensors.BtDeviceListActivity;
import arduinosensors.tk.arduinosensors.R;
import arduinosensors.tk.arduinosensors.model.ASensor;
import arduinosensors.tk.arduinosensors.model.DbHelper;

public class SensorPresenterImpl implements SensorPresenter {

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    SensorView rootView;
    SQLiteDatabase db;
    DbHelper dbh;
    SensorActivity activity;
    ThreadPoolExecutor executor;
    ConnectedThread mConnectedThread;
    Queue <Double> qSensor1;
    Queue <Double> qSensor2;
    private static final int SENSOR_Queue_SIZE = 600;

    public SensorPresenterImpl(SensorView rootView, SensorActivity activity) {
        this.rootView = rootView;
        this.activity = activity;
        qSensor1=new LinkedList<Double>();
        qSensor2=new LinkedList<Double>();
        dbh = new DbHelper(this.activity.getBaseContext());
        db = dbh.getWritableDatabase();
        int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        executor = new ThreadPoolExecutor(
            NUMBER_OF_CORES*2,
            NUMBER_OF_CORES*2,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>()
        );
    }

    @Override
    public void onCreate() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();
        for(int i=0; i<SENSOR_Queue_SIZE; i++){
            qSensor1.add(0d);
            qSensor2.add(0d);
        }
    }

    @Override
    public void onPause() {
        try
        {
            btSocket.close();
        }
        catch (IOException e2) {
            //insert code to deal with this
        }
    }

    @Override
    public void onResume(Intent intent) {
        //Get the MAC address from the DeviceListActivty via EXTRA
        String address = intent.getStringExtra(BtDeviceListActivity.EXTRA_DEVICE_ADDRESS);
        Log.d("SensorPresenter", "intent address = " + address);
        //create device and set the MAC address
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            rootView.showMessage("Socket creation failed");
        }
        // Establish the Bluetooth socket connection.
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                //insert code to deal with this
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.button:
                mConnectedThread.write("1");
                break;
            case R.id.button2:
                mConnectedThread.write("2");
                break;
            case R.id.button3:
                mConnectedThread.write("3");
                break;
            case R.id.button4:
                mConnectedThread.write("4");
                break;
        }
    }

    @Override
    public void takeScreenShot() {
        saveScreenShot(screenShot(activity.rootLayout));
    }

    public Bitmap screenShot(View view) {
        Bitmap bitmap;
        view.setDrawingCacheEnabled(true);
        bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }

    public boolean saveScreenShot(Bitmap bm) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File folder = new File(Environment.getExternalStorageDirectory().toString()+"/Arduinosensors/images");
        folder.mkdirs();
        OutputStream fout = null;
        File imageFile = new File(folder, "screenshot" + timeStamp+".png");

        try {
            fout = new FileOutputStream(imageFile);
            //bm.compress(Bitmap.CompressFormat.JPEG, 90, fout);
            bm.compress(Bitmap.CompressFormat.PNG, 100, fout);
            fout.flush();
            fout.close();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return true;
    }


    @Override
    public synchronized Queue getSensorValue(String sensorName) {
        if(sensorName.equals("s1")){
            return qSensor1;
        }
        else if(sensorName.equals("s2")){
            return qSensor2;
        }
        else return null;
    }



    private void checkBTState() {
        if(btAdapter==null) {
            rootView.showMessage("Device does not support bluetooth");
        }
        else {
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final InputStreamReader readerInStream;
        private final LineNumberReader lineNumberReader;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            readerInStream = new InputStreamReader(mmInStream);
            lineNumberReader = new LineNumberReader(readerInStream);
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    //bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = lineNumberReader.readLine();
                    //Log.d("TREAD", "buffer = " + buffer);
                    //bytes = readMessage.length();

                    executor.execute(new jsonWorker(readMessage, System.currentTimeMillis()));

                    // Send the obtained bytes to the UI Activity via handler
                } catch (IOException e) {
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                rootView.showMessage("Connection Failure");
                activity.finish();
            }
        }
    }

    private class jsonWorker implements Runnable {
        private String json;
        private final Object lock = new Object();
        private long timestamp;
        private Boolean stop = false;

        public jsonWorker(String message, long timestamp) {
            Log.d("JSON_WORKER", "message = " + message);
            this.timestamp = timestamp;
            if(message.substring(0,1).contains("[")){
            this.json = message;
            }
            else{
                activity.bluetoothIn.obtainMessage(activity.handlerState, message.length(), -1, message).sendToTarget();
                stop = true;
            }

        }

        @Override
        public void run() {
            if (!stop){
                try {
                    JSONArray urls = new JSONArray(json);
                    for (int i = 0; i < urls.length(); i++) {
                        JSONObject jsonobj = urls.getJSONObject(i);
                        Iterator<String> keys = jsonobj.keys();
                        while (keys.hasNext()) {
                            String key = (String) keys.next();
                            double value = (double) jsonobj.getDouble(key);
                            ASensor sensor = new ASensor(key, value, timestamp);
                            if (sensor.name.equals("s1")) {
                                synchronized (lock) {
                                    qSensor1.add(sensor.value);
                                    qSensor1.remove();
                                }
                            }
                            if (sensor.name.equals("s2")) {
                                synchronized (lock) {
                                    qSensor2.add(sensor.value);
                                    qSensor2.poll();
                                }
                            }
                            ContentValues cv = new ContentValues();
                            //Log.d("SensorActivity", "key = " + sensor.name + " value = " +sensor.value + " date_time = " + sensor.date_time);

                            cv.put(DbHelper.COLUMN_DATETIME, sensor.name);
                            cv.put(DbHelper.COLUMN_VALUE, sensor.value);
                            cv.put(DbHelper.COLUMN_DATETIME, sensor.date_time);
                            long rowID = db.insert(DbHelper.TABLE_NAME_SENSOR, null, cv);
                            //Log.d("SensorActivity", "row inserted, ID = " + rowID);

                        }
                    }
                } catch (JSONException e) {

                    e.printStackTrace();
                }
        }}

    }
}
