package airDataBackendService.database;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "predictions")
// make the combination of hour and sensor_id unique
@CompoundIndex(def = "{'hour':1, 'sensor_id':1}", unique = true)
public class Prediction {
  @DateTimeFormat
  @Field("hour")
  @Indexed
  public Date hour;

  @Field("sensor_id")
  @Indexed
  public String sensor_id;

  public double p10;

  public double p25;

  @Override
  public String toString() {
    return "Prediction at " + this.hour + " by " + this.sensor_id + "; P10: " + this.p10 + ", P25: " + this.p25;
  }
}
