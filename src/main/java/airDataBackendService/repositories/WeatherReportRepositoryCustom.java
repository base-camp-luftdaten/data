package airDataBackendService.repositories;

import airDataBackendService.database.HourlyWeatherReport;

public interface WeatherReportRepositoryCustom {
  public void updateMany(Iterable<HourlyWeatherReport> reports);
}