package arduinosensors.tk.arduinosensors;

import android.os.Bundle;
import android.preference.PreferenceActivity;


public class PrefActive extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
    }
}
