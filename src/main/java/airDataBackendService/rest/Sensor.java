package airDataBackendService.rest;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Sensor {

  private long id;

  private String pin;

  @JsonProperty("sensor_type")
  private SensorType sensorType;

  public long getId() {
    return this.id;
  }

  public String getPin() {
    return this.pin;
  }

  public SensorType getSensorType() {
    return this.sensorType;
  }
}