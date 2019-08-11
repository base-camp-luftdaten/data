package airDataBackendService.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import airDataBackendService.KMLImporter.Coordinate;
import airDataBackendService.KMLImporter.Forecast;
import airDataBackendService.KMLImporter.Reader;
import airDataBackendService.KMLImporter.StationData;
import airDataBackendService.database.HourlyWeatherReport;
import airDataBackendService.database.Sensor;
import airDataBackendService.repositories.WeatherReportRepository;

@Service
public class WeatherDataService {

	@Autowired
	private AirDataHandlerService airDataService;

	@Autowired
	private WeatherReportRepository weatherReportRepository;

	// Every 2 hours
	@Scheduled(cron = "0 0 */2 * * *")
	private void importWeatherReports() {

		List<Sensor> allSensors = airDataService.getSensors();

		// Fetch the weather forecast outside of loop because it is independent of the
		// dust sensor location
		Forecast aForecast = Reader.take();
		System.out.println("Stations online: " + aForecast.positionRegister.length);

		for (Sensor aSensor : allSensors) {
			this.persist(aForecast, aSensor);
		}
	}

	public void test() {
		Sensor s = new Sensor();
		s.lat = -36.002894;
		s.lon = -69.277013;
		s.id = "asdf1234";

		this.persist(Reader.take(), s);
	}

	private void persist(Forecast aForecast, Sensor aSensor) {
		Date from = aForecast.firstAvailableDate();
		Date to = aForecast.lastAvailableDate();

		double lat = aSensor.lat;
		double lon = aSensor.lon;

		// Retrieve the closest weather station to the dust sensor location
		StationData station = aForecast.getStation(lat, lon);
		double[] temp = aForecast.temperatur(from, to, station);
		// The corresponding timestamps for the weather report
		Date[] times = aForecast.zeitschritte(from, to);
		// ACHTUNG ES MUSS GELTEN:
		// forecast.firstAvailableDate() <= from <= to <= forecast.lastAvailableDate()
		// ANDERNFALLS TRITT EINE ARRAY-INDEX-OUT-OF-BOUNDS-EXEPTION AUF

		System.out.println(station.name + ": " + station.coordinate.toString() + ", Distanz: "
				+ station.coordinate.distance(new Coordinate(lat, lon)) + " km");

		ArrayList<HourlyWeatherReport> weatherReports = new ArrayList<HourlyWeatherReport>(times.length);
		for (int i = 0; i < times.length; i++) {
			// System.out.println(times[i].toString() + ": " + temp[i] + "°C");

			HourlyWeatherReport weatherReport = new HourlyWeatherReport();
			weatherReport.temperature = temp[i];
			weatherReport.hour = times[i];
			weatherReport.sensor_id = aSensor.id;
			weatherReports.add(weatherReport);
		}
		this.weatherReportRepository.updateMany(weatherReports);
	}
}
