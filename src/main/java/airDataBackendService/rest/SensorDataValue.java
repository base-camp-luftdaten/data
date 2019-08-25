package airDataBackendService.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SensorDataValue {

  private long id;

  @JsonDeserialize(using = ValueDeserializer.class)
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