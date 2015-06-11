package arduinosensors.tk.arduinosensors.model;

import java.util.Calendar;

public class Sensor {
    public String name;
    public double value;
    public int date_time;

    public Sensor(String name, double value) {
        this.name = name;
        this.value = value;
        Calendar c = Calendar.getInstance();
        this.date_time = c.get(Calendar.SECOND);
    }
}
