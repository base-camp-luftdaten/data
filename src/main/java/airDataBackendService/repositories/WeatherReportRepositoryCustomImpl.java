package airDataBackendService.repositories;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import airDataBackendService.database.HourlyWeatherReport;

public class WeatherReportRepositoryCustomImpl implements WeatherReportRepositoryCustom {
  @Autowired
  MongoTemplate mongoTemplate;

  public void updateMany(Iterable<HourlyWeatherReport> reports) {

    for (HourlyWeatherReport report : reports) {
      Query query = new Query(Criteria.where("sensor_id").is(report.sensor_id).and("hour").is(report.hour));

      Update update = new Update().set("windspeed", report.windspeed).set("maxWindspeed", report.maxWindspeed)
          .set("sunIntensity", report.sunIntensity).set("sunDuration", report.sunDuration)
          .set("temperature", report.temperature).set("dewPoint", report.dewPoint)
          .set("airPressure", report.airPressure).set("precipitation", report.precipitation)
          .set("sleetPrecipitation", report.sleetPrecipitation).set("visibility", report.visibility)
          .set("foggProbability", report.foggProbability).set("station_name", report.station_name);

      mongoTemplate.upsert(query, update, HourlyWeatherReport.class);
    }
  }

  public HourlyWeatherReport getForecastFor(String aSensorId, Date hour) {
    return mongoTemplate.findOne(new Query(Criteria.where("sensor_id").is(aSensorId).and("hour").is(hour)),
        HourlyWeatherReport.class);
  }
  public List<HourlyWeatherReport> getAllForecastsFor(Date hour) 
  {
      List<HourlyWeatherReport> hourSet = mongoTemplate.findAll(new Query(Criteria.where("hour").is(hour)), HourlyWeatherReport.class);
      return hourSet;
  }
}
