package airDataBackendService.database;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

@Document(collection = "measurements")
public class Measurement {

  @Id
  public String id;

  @Field("sensor_id")
  public String sensorId;

  @Field("sensor_type")
  public String sensorType;

  public double lat;
  public double lon;

  @DateTimeFormat
  @Field("timestamp")
  public Date timestamp;

  @Field("P10")
  public double p10;

  @Field("P25")
  public double p25;

  public String fromDataset;

  @Override
  public String toString() {
    return String.format("Measurement[id=%s]", id);
  }
}