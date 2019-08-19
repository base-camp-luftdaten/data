package airDataBackendService.database;

import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

@Document(collection = "sensordata")
// make the combination of day and sensor_id unique
@CompoundIndex(def = "{'day':1, 'sensor_id':1}", unique = true)
public class DailyMeasurements {

  @Field("sensor_id")
  public String sensor_id;

  @DateTimeFormat
  @Field("day")
  public Date day;

  public List<Measurement> measurements;
}