package airDataBackendService.repositories;

import java.util.Date;

import airDataBackendService.database.HourlyWeatherReport;

public interface WeatherReportRepositoryCustom {
  public void updateMany(Iterable<HourlyWeatherReport> reports);

  public HourlyWeatherReport getForecastFor(String aSensorId, Date hour);
}