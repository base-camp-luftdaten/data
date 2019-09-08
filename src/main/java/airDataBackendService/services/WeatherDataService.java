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
	
	public void NanFix(Date hour)
	{   

	    class PointHolder
	    {
		public double lat;
		public double lon;
		public HourlyWeatherReport report;

		public PointHolder(double la, double lo, HourlyWeatherReport rep)
		{
			lat = la;
			lon = lo;
			report = rep;
		}

		public PointHolder nearest(PointHolder[] array)
		{
			PointHolder point=this;
			double minDist=Double.MAX_VALUE;
			double dist;
			PointHolder minCord = null;

			for(int i=0;i<array.length;i++)
			{
				dist=point.distance(array[i].lat,array[i].lon);
				if(minDist>dist)
				{
					minCord=array[i];
					minDist = dist;
				}
			}
			return minCord;
		}

		public double distance(double lat2, double lon2) 
			{
			double lat1=this.lat;
			double lon1=this.lon;

			if ((lat1 == lat2) && (lon1 == lon2)) 
				{
					return 0;
				}
				else 
				{
					double theta = lon1 - lon2;
					double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
					dist = Math.acos(dist);
					dist = Math.toDegrees(dist);
					dist = dist * 111.18957696;

					return dist;
				}
			}
	    }

	    List<HourlyWeatherReport> hourSet = this.weatherReportRepository.getAllForecastsFor(hour);
		List<Sensor> allSensors = airDataService.getSensors();

		LinkedList<PointHolder> updateList = new LinkedList<PointHolder>();
		LinkedList<PointHolder>[] santasGoodList = new LinkedList[11];

	    for (int i = 0; i < 11; i++) 
	    {
		santasGoodList[i] = new LinkedList<PointHolder>();
	    } 

	    for (HourlyWeatherReport report : hourSet) 
	    {
		String SeId = report.sensor_id;
		PointHolder hold;
		boolean defect = false;
		for (Sensor Sensor : allSensors) 
		{
			if(SeId.equals(Sensor.id))
			{
				hold = new PointHolder(Sensor.lat,Sensor.lon,report);
			}
			}

		if(Double.isFinite(report.windspeed))
		{
				santasGoodList[0].add(hold);
		}
		else
		{
			defect = true;
		}

		if(Double.isFinite(report.maxWindspeed))
		{
				santasGoodList[1].add(hold);
		}
		else
		{
			defect = true;
		}

		if(Double.isFinite(report.sunIntensity))
		{
				santasGoodList[2].add(hold);
		}
		else
		{
			defect = true;
		}

		if(Double.isFinite(report.sunDuration))
		{
				santasGoodList[3].add(hold);
		}
		else
		{
			defect = true;
		}

		if(Double.isFinite(report.temperature))
		{
				santasGoodList[4].add(hold);
		}
		else
		{
			defect = true;
		}

		if(Double.isFinite(report.dewPoint))
		{
				santasGoodList[5].add(hold);
		}
		else
		{
			defect = true;
		}

		if(Double.isFinite(report.airPressure))
		{
				santasGoodList[6].add(hold);
		}
		else
		{
			defect = true;
		}

		if(Double.isFinite(report.precipitation))
		{
				santasGoodList[7].add(hold);
		}
		else
		{
			defect = true;
		}

		if(Double.isFinite(report.sleetPrecipitation))
		{
				santasGoodList[8].add(hold);
		}
		else
		{
			defect = true;
		}

		if(Double.isFinite(report.visibility))
		{
				santasGoodList[9].add(hold);
		}
		else
		{
			defect = true;
		}

		if(Double.isFinite(report.foggProbability))
		{
				santasGoodList[10].add(hold);
		}
		else
		{
			defect = true;
		}

		if(defect)
		{
			updateList.add(hold);
		}
	    }

	    PointHolder[][] santasGoodArray = new PointHolder[11][];
	    for(int i = 0; i < 11; i++)
	    {
		santasGoodArray[i] = santasGoodList[i].toArray(new PointHolder[santasGoodList[i].size()]);
	    }

	    ArrayList<HourlyWeatherReport> weatherReports = new ArrayList<HourlyWeatherReport>(updateList.size());
	    for(PointHolder holde : updateList)
	    {

		if(!Double.isFinite(holde.report.windspeed))
		{
			holde.report.windspeed = holde.nearest(santasGoodArray[0]).report.windspeed;
		}

		if(!Double.isFinite(holde.report.maxWindspeed))
		{
			holde.report.maxWindspeed = holde.nearest(santasGoodArray[1]).report.maxWindspeed;
		}

		if(!Double.isFinite(holde.report.sunIntensity))
		{
			holde.report.sunIntensity = holde.nearest(santasGoodArray[2]).report.sunIntensity;
		}

		if(!Double.isFinite(holde.report.sunDuration))
		{
			holde.report.sunDuration = holde.nearest(santasGoodArray[3]).report.sunDuration;
		}

		if(!Double.isFinite(holde.report.temperature))
		{
			holde.report.temperature = holde.nearest(santasGoodArray[4]).report.temperature;
		}

		if(!Double.isFinite(holde.report.dewPoint))
		{
			holde.report.dewPoint = holde.nearest(santasGoodArray[5]).report.dewPoint;
		}

		if(!Double.isFinite(holde.report.airPressure))
		{
			holde.report.airPressure = holde.nearest(santasGoodArray[6]).report.airPressure;
		}

		if(!Double.isFinite(holde.report.precipitation))
		{
			holde.report.precipitation = holde.nearest(santasGoodArray[7]).report.precipitation;
		}

		if(!Double.isFinite(holde.report.sleetPrecipitation))
		{
			holde.report.sleetPrecipitation = holde.nearest(santasGoodArray[8]).report.sleetPrecipitation;
		}

		if(!Double.isFinite(holde.report.visibility))
		{
			holde.report.visibility = holde.nearest(santasGoodArray[9]).report.visibility;
		}

		if(!Double.isFinite(holde.report.foggProbability))
		{
			holde.report.foggProbability = holde.nearest(santasGoodArray[10]).report.foggProbability;
		}


		weatherReports.add(holde.report);
	    }
	    this.weatherReportRepository.updateMany(weatherReports);
	}
}
