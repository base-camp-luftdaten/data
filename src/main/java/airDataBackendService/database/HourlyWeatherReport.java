package airDataBackendService.database;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "hourly-weather-reports")
// make the combination of hour and sensor_id unique
@CompoundIndex(def = "{'hour':1, 'sensor_id':1}", unique = true)
public class HourlyWeatherReport {
	@DateTimeFormat
	@Field("hour")
	public Date hour;

	@Field("sensor_id")
	public String sensor_id;

	@Field("windspeed")
	public double windspeed;

	@Field("maxWindspeed")
	public double maxWindspeed;

	@Field("sunIntensity")
	public double sunIntensity;

	@Field("sunDuration")
	public double sunDuration;

	@Field("temperature")
	public double temperature;

	@Field("dewPoint")
	public double dewPoint;

	@Field("airPressure")
	public double airPressure;

	@Field("precipitation")
	public double precipitation;

	@Field("sleetPrecipitation")
	public double sleetPrecipitation;

	@Field("visibility")
	public double visibility;

	@Field("foggProbability")
	public double foggProbability;
}
