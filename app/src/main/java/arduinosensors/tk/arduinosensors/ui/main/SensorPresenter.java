package arduinosensors.tk.arduinosensors.ui.main;

import android.content.Intent;

import java.util.Queue;


public interface SensorPresenter {
    void onCreate();
    void onPause();
    void onResume(Intent intent);
    void worker(String message);

    boolean exportDB();
    boolean cleanDB();


    Queue getSensorValue(String sensorName);
}
