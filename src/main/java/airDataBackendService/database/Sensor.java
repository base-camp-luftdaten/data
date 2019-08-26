package airDataBackendService.database;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sensors")
public class Sensor {
    public Sensor(String id, double lat, double lon) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
    }

    @Indexed(unique = true)
    @Field("sensor_id")
    public String id;

    @Field("lat")
    public double lat;

    @Field("lon")
    public double lon;
}