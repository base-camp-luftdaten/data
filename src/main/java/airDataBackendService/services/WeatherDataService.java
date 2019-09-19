package airDataBackendService.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
		if (aForecast.doIContainErrors()) {
			// TODO: don't just print it to the console
			System.out.println("Forcast contains errors");
		}
		

		for (Sensor aSensor : allSensors) {
			this.persist(aForecast, aSensor);
		}
	}

	/**
	 * Takes a forecast and a sensor and saves the forecast data in the database
	 */
	private void persist(Forecast aForecast, Sensor aSensor) {
		Date from = aForecast.firstAvailableDate();
		Date to = aForecast.lastAvailableDate();

		double lat = aSensor.lat;
		double lon = aSensor.lon;

		// Retrieve the closest weather station to the dust sensor location
		StationData station = aForecast.getStation(lat, lon);
		double[] windspeeds = aForecast.windgeschwindigkeit(from, to, station);
		double[] maxWindspeeds = aForecast.maxWindgeschwindigkeit(from, to, station);
		double[] sunIntensities = aForecast.sonnenEinstrahlung(from, to, station);
		double[] sunDurations = aForecast.sonnenDauer(from, to, station);
		double[] temperatures = aForecast.temperatur(from, to, station);
		double[] dewPoints = aForecast.taupunkt(from, to, station);
		double[] airPressures = aForecast.luftdruck(from, to, station);
		double[] precipitations = aForecast.niederschlag(from, to, station);
		double[] sleetPrecipitations = aForecast.schneeregenNiederschlag(from, to, station);
		double[] visibilities = aForecast.sichtweite(from, to, station);
		double[] foggProbabilities = aForecast.nebelWahrscheinlichkeit(from, to, station);
		// The corresponding timestamps for the weather report
		Date[] times = aForecast.zeitschritte(from, to);

		ArrayList<HourlyWeatherReport> weatherReports = new ArrayList<HourlyWeatherReport>(times.length);

		for (int i = 0; i < times.length; i++) {
			HourlyWeatherReport weatherReport = new HourlyWeatherReport();
			weatherReport.windspeed = windspeeds[i];
			weatherReport.maxWindspeed = maxWindspeeds[i];
			weatherReport.sunIntensity = sunIntensities[i];
			weatherReport.sunDuration = sunDurations[i];
			weatherReport.temperature = temperatures[i];
			weatherReport.dewPoint = dewPoints[i];
			weatherReport.airPressure = airPressures[i];
			weatherReport.precipitation = precipitations[i];
			weatherReport.sleetPrecipitation = sleetPrecipitations[i];
			weatherReport.visibility = visibilities[i];
			weatherReport.foggProbability = foggProbabilities[i];

			weatherReport.hour = times[i];
			weatherReport.sensor_id = aSensor.id;
			weatherReport.station_name = station.name;

			weatherReports.add(weatherReport);
		}
		this.weatherReportRepository.updateMany(weatherReports);
	}

	public HourlyWeatherReport getForecastFor(String aSensorId, long aTimestampInSeconds) {
		// round timestamp to nearest hour
		long nearestHour = (long) Math.round(aTimestampInSeconds / 3600) * (long) 3600000;

		if (aTimestampInSeconds % 3600 > 1800) {
			nearestHour += 3600000;
		}

		Date d = new Date(nearestHour);

		return this.weatherReportRepository.getForecastFor(aSensorId, d);
	}
}
