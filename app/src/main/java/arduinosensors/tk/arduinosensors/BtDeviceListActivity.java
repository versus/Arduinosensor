package arduinosensors.tk.arduinosensors;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import arduinosensors.tk.arduinosensors.model.DbHelper;
import arduinosensors.tk.arduinosensors.ui.main.SensorActivity;
import butterknife.ButterKnife;
import butterknife.InjectView;

import com.crittercism.app.Crittercism;


public class BtDeviceListActivity extends ActionBarActivity {

        // Debugging for LOGCAT
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;
    SQLiteDatabase db;
    DbHelper dbHelper;


    private boolean doubleBackToExitPressedOnce = false;

    @InjectView(R.id.connecting)
    TextView textView1;

    @InjectView(R.id.paired_devices)
    ListView pairedListView;


    // EXTRA string to send on to mainactivity
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_device_list);
        ButterKnife.inject(this);
        Crittercism.initialize(getApplicationContext(), "55af5e9ba046e30a00dc2a5b");
        checkBTState();
        dbHelper = new DbHelper(this);
        db = dbHelper.getWritableDatabase();
        textView1.setTextSize(40);
    	textView1.setText(" ");
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
    	pairedListView.setOnItemClickListener(mDeviceClickListener);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
    		for (BluetoothDevice device : pairedDevices) {
    			mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
    		}
    	} else {
    		String noDevices = getResources().getText(R.string.none_paired).toString();
    		mPairedDevicesArrayAdapter.add(noDevices);
    	}

    }

    @Override
    protected void onResume() {
        super.onResume();
        this.doubleBackToExitPressedOnce = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sensor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menu_deleteDB){
            showCleanDBAlert();
            return true;
        }
        if(id == R.id.menu_exportDB){
            return exportDB();
        }
        if(id== R.id.menu_preference){
            Intent i = new Intent(BtDeviceListActivity.this, PrefActive.class);
            startActivity(i);

        }
        return super.onOptionsItemSelected(item);
    }


    private void showCleanDBAlert(){
        AlertDialog.Builder ad;
        ad = new AlertDialog.Builder(this);
        ad.setTitle("Удаление данных");  // заголовок
        ad.setMessage("Внимание, удаленные данные нельзя будет восстановить."); // сообщение
        ad.setPositiveButton("Удалить!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                cleanDB();
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

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Нажми еще раз для выхода", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    private void checkBTState() {
        mBtAdapter=BluetoothAdapter.getDefaultAdapter();
        if(mBtAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
          if (mBtAdapter.isEnabled()) {
            Log.d(TAG, "...Bluetooth ON...");
          } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
            }
          }
        }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        finish();
        startActivity(getIntent());
  }


        private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

        	textView1.setText("Соединение...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
			Intent i = new Intent(BtDeviceListActivity.this, SensorActivity.class);
            i.putExtra(EXTRA_DEVICE_ADDRESS, address);
            Log.d(TAG, "Bluetooth Address = " + address);
			startActivity(i);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    textView1.setText("");
                }
            }, 2000);

        }
    };

    public boolean exportDB() {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File folder = new File(Environment.getExternalStorageDirectory().toString()+"/Arduinosensors/Dump");
            folder.mkdirs();
            File data = Environment.getDataDirectory();
            FileChannel source=null;
            FileChannel destination=null;
            String currentDBPath = "/data/"+ "arduinosensors.tk.arduinosensors" +"/databases/"+DbHelper.DB_NAME;
            String backupDBPath = DbHelper.DB_NAME+timeStamp+".sqlite";
            File currentDB = new File(data, currentDBPath);
            File backupDB = new File(folder, backupDBPath);

        Log.d("EXPORTDB","BackupDB path = " + backupDB.toString());
            try {
                source = new FileInputStream(currentDB).getChannel();
                destination = new FileOutputStream(backupDB).getChannel();
                destination.transferFrom(source, 0, source.size());
                source.close();
                destination.close();
                showMessage("DB Exported!");
            } catch(IOException e) {
                e.printStackTrace();
            }
        return true;
    }

    public boolean  cleanDB() {
        if(db.delete(DbHelper.TABLE_NAME_SENSOR, "1", null) > 0){
            showMessage("База данных очищена!");
            return true;
        }
        return false;
    }

    public void showMessage(String message) {
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
    }

}
