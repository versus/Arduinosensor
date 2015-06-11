package arduinosensors.tk.arduinosensors.ui.main;

import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.XYPlot;

import arduinosensors.tk.arduinosensors.R;
import butterknife.ButterKnife;
import butterknife.InjectView;


public class SensorActivity extends ActionBarActivity implements SensorView {



    Handler bluetoothIn;
    final int handlerState = 0;        				 //used to identify handler message
    SensorPresenter presenter;

    @InjectView(R.id.resultFromBt)
    TextView textViewResult;

    //@InjectView(R.id.dynamicXYPlot)
    //XYPlot plot;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);
        ButterKnife.inject(this);
        presenter = new SensorPresenterImpl(this, this);
        presenter.onCreate();

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    textViewResult.setText(readMessage);
                    presenter.worker(readMessage.toString());

                }
            }
        };
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
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.onPause();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void showMessage(String message) {
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
    }
}
