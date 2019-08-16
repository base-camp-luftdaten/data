package airDataBackendService.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SensorDataValue {

  private long id;

  private double value;

  @JsonProperty("value_type")
  private String valueType;

  public long getId() {
    return this.id;
  }

  public double getValue() {
    return this.value;
  }

  public String getValueType() {
    return this.valueType;
  }
}