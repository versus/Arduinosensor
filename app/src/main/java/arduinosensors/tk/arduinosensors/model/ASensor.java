package arduinosensors.tk.arduinosensors.model;

import java.util.Calendar;

public class ASensor {
    public String name;
    public double value;
    public long date_time;

    public ASensor(String name, double value, long timestamp) {
        this.name = name;
        this.value = value;
        this.date_time = timestamp;
        //this.date_time = System.currentTimeMillis();

    }
}
