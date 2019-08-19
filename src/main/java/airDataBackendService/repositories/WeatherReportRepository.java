package airDataBackendService.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import airDataBackendService.database.HourlyWeatherReport;

public interface WeatherReportRepository extends MongoRepository<HourlyWeatherReport, String>, WeatherReportRepositoryCustom {
  public void updateMany(Iterable<HourlyWeatherReport> reports);
}