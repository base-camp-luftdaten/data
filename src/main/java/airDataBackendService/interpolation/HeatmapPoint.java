package airDataBackendService.interpolation;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HeatmapPoint {
  @JsonProperty("x")
  public double lon;

  @JsonProperty("y")
  public double lat;

  @JsonProperty("v")
  public double value;
}