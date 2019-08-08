package airDataBackendService.database;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.Id;

import airDataBackendService.database.DayHourlySensorData;

@Document(collection = "hourlyDataCollection")
public class HourlySensorData
{
	@Id
	public String sensor_id;

	public double Latitude;
	public double Longitude;

	public List<DayHourlySensorData> recordedDays;
}