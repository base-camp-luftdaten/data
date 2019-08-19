package airDataBackendService.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Location {

  private long id;

  @JsonProperty("exact_location")
  private long exactLocation;

  private String country;

  private int indoor;

  private double altitude;

  private double longitude;

  public long getId() {
    return this.id;
  }

  public long getExactLocation() {
    return this.exactLocation;
  }

  public String getCountry() {
    return this.country;
  }

  public int getIndoor() {
    return this.indoor;
  }

  public double getAltitude() {
    return this.altitude;
  }

  public double getLongitude() {
    return this.longitude;
  }
}