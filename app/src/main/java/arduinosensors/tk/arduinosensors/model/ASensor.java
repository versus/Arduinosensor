package arduinosensors.tk.arduinosensors.model;

import java.util.Calendar;

public class ASensor {
    public String name;
    public double value;
    public long date_time;

    public ASensor(String name, double value) {
        this.name = name;
        this.value = value;
        Calendar c = Calendar.getInstance();
        this.date_time = System.currentTimeMillis();
    }
}
