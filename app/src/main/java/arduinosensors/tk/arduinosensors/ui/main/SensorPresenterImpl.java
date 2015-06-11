package arduinosensors.tk.arduinosensors.ui.main;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import arduinosensors.tk.arduinosensors.BtDeviceListActivity;
import arduinosensors.tk.arduinosensors.SensorApp;
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

    public SensorPresenterImpl(SensorView rootView, SensorActivity activity) {
        this.rootView = rootView;
        this.activity = activity;
        dbh = new DbHelper(this.activity.getBaseContext());
        db = dbh.getWritableDatabase();
    }


    @Override
    public void onCreate() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();
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
            Toast.makeText(activity.getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
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
    public void worker(String message) {
        String name ="sens1";
        double value = 3838.32;
        db.beginTransaction();
        try{
            ContentValues cv = new ContentValues();
            Log.d("SensorActivity", "--- Insert in sensor: ---");
            cv.put(DbHelper.COLUMN_DATETIME, name);
            cv.put(DbHelper.COLUMN_VALUE, value);
            cv.put(DbHelper.COLUMN_DATETIME, System.currentTimeMillis());
            long rowID = db.insert(DbHelper.TABLE_NAME_SENSOR, null, cv);
            Log.d("SensorActivity", "row inserted, ID = " + rowID);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
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
                    activity.bluetoothIn.obtainMessage(activity.handlerState, bytes, -1, readMessage).sendToTarget();
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
}
