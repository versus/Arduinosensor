package arduinosensors.tk.arduinosensors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.XYPlot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class SensorActivity extends ActionBarActivity {

    final String COLUMN_DATETIME ="sensor_datetime";
    final String COLUMN_NAME ="sensor_name";
    final String COLUMN_VALUE ="sensor_value";
    final int DB_VERSION = 1;
    final String DB_NAME = "mSensor";
    final String TABLE_NAME_SENSOR ="sensor";

    final String CREATE_TABLE = "create table "+ TABLE_NAME_SENSOR +" ( id integer primary key autoincrement,"
            + COLUMN_DATETIME + " int, "
            + COLUMN_NAME + " text, "
            + COLUMN_VALUE  + " real );";

    Handler bluetoothIn;
    final int handlerState = 0;        				 //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @InjectView(R.id.resultFromBt)
    TextView textViewResult;

    //@InjectView(R.id.dynamicXYPlot)
    //XYPlot plot;

    SQLiteDatabase db;
    DBHelper dbh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);
        ButterKnife.inject(this);
        dbh = new DBHelper(this);
        db = dbh.getWritableDatabase();
        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    textViewResult.setText(readMessage);
                    String name ="sens1";
                    double value = 3838.32;
                    db.beginTransaction();
                    try{
                        ContentValues cv = new ContentValues();
                        Log.d("SensorActivity", "--- Insert in sensor: ---");
                        cv.put(COLUMN_NAME, name);
                        cv.put(COLUMN_VALUE, value);
                        cv.put(COLUMN_DATETIME, System.currentTimeMillis());
                        long rowID = db.insert(TABLE_NAME_SENSOR, null, cv);
                        Log.d("SensorActivity", "row inserted, ID = " + rowID);
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }

                }
            }
        };
        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sensor, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();

        //Get the MAC address from the DeviceListActivty via EXTRA
        String address = intent.getStringExtra(BtDeviceListActivity.EXTRA_DEVICE_ADDRESS);

        //create device and set the MAC address
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        // Establish the Bluetooth socket connection.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {
                //insert code to deal with this
            }
        }
        ConnectedThread mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        try
            {
            //Don't leave Bluetooth sockets open when leaving activity
              btSocket.close();
            }
        catch (IOException e2) {
                //insert code to deal with this
            }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkBTState() {
        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        }
        else {
            if (!btAdapter.isEnabled()) {
              Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
              startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
      return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

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
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);        	//read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
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
            	Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
            	finish();
            }
        }
    }


    class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
          // конструктор суперкласса
          super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
          Log.d("SensorActivity", "--- onCreate database ---");
            Log.d("SensorActivity", "CREATE TABLE: " + CREATE_TABLE);
          // создаем таблицу с полями
          db.execSQL(CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }


}
