package arduinosensors.tk.arduinosensors.ui.main;

import android.content.Intent;
import android.view.View;

import java.util.Queue;


public interface SensorPresenter {
    void onCreate();
    void onPause();
    void onResume(Intent intent);
    void onClick(View view);
    void takeScreenShot();
    Queue getSensorValue(String sensorName);
}
