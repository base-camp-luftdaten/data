package airDataBackendService.rest;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AirDataAPIResult {

  private long id;

  @JsonProperty("sensorDataValues")
  private List<SensorDataValue> values;

  @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
  private Date timestamp;

  private Location location;

  public long getId() {
    return this.id;
  }

  public List<SensorDataValue> getValues() {
    return this.values;
  }

  public Date getTimestamp() {
    return this.timestamp;
  }

  public Location getLocation() {
    return this.location;
  }
}