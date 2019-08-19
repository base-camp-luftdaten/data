package airDataBackendService.rest;

import airDataBackendService.database.HourlyWeatherReport;
import airDataBackendService.database.Measurement;

public class BySensorResponse {
  public boolean continuous;
  public Measurement measurement;
  public HourlyWeatherReport weatherReport;
}