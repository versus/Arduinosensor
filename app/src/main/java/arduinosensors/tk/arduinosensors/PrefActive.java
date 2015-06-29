package arduinosensors.tk.arduinosensors;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;


public class PrefActive extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener  {

    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        summaryPref();
    }

    private void summaryPref(){
        Preference line1 = (Preference) findPreference("line1");
        line1.setSummary("Значение: " +sp.getString("line1", "Sensor 1"));
        Preference line2 = (Preference) findPreference("line2");
        line2.setSummary("Значение: " +sp.getString("line2", "Sensor 2"));
        Preference btSend1 = (Preference) findPreference("button1");
        Preference btSend2 = (Preference) findPreference("button2");
        Preference btSend3 = (Preference) findPreference("button3");
        Preference btSend4 = (Preference) findPreference("button4");
        btSend1.setSummary("Значение: " +sp.getString("button1", "One"));
        btSend2.setSummary("Значение: " +sp.getString("button2", "Two"));
        btSend3.setSummary("Значение: " +sp.getString("button3", "Three"));
        btSend4.setSummary("Значение: " +sp.getString("button4", "Four"));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
         Log.d("PREF", "change key =" + s);
        summaryPref();
    }
}
