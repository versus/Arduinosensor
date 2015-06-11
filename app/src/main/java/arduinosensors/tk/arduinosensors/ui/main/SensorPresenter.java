package arduinosensors.tk.arduinosensors.ui.main;

import android.content.Intent;


public interface SensorPresenter {
    void onCreate();
    void onPause();
    void onResume(Intent intent);
    void worker(String message);

    boolean exportDB();
    boolean cleanDB();


}
