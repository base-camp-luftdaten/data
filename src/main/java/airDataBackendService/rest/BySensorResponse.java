package airDataBackendService.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import airDataBackendService.database.HourlyWeatherReport;
import airDataBackendService.database.Measurement;

public class BySensorResponse {
  public boolean continuous;
  public Measurement measurement;
  public HourlyWeatherReport weatherReport;
  @JsonProperty("notContinuousBecause")
  public String reason;
}