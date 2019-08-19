package airDataBackendService.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SensorType {
  private String manufacturer;

  private String name;

  private long id;

  public long getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public String getManufacturer() {
    return this.manufacturer;
  }
}