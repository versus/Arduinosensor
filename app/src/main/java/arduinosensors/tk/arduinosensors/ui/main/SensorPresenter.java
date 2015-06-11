package arduinosensors.tk.arduinosensors.ui.main;

import android.content.Intent;


public interface SensorPresenter {
    public void onCreate();
    public void onPause();
    public void onResume(Intent intent);
    public void worker(String message);
}
