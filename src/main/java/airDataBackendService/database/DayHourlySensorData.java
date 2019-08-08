package airDataBackendService.database;

import java.util.List;

import org.springframework.format.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

public class DayHourlySensorData 
{
	@DateTimeFormat
	public Date day;
	
	public boolean complete;
	
	@Field("P1")
	public final double[] p1 = new double[24];
	@Field("P2")
	public final double[] p2 = new double[24];
	@Field("P1Vorhersage")
	public final double[] p1V = new double[24];
	@Field("P2Vorhersage")
	public final double[] p2V = new double[24];
	@Field("Windgeschw")
	public final double[] windspeed = new double[24];
	@Field("MaxWindgeschw")
	public final double[] maxWindspeed = new double[24];
	@Field("SonnenIntens")
	public final double[] sunIntensity = new double[24];
	@Field("Sonnendauer")
	public final double[] sunDuration = new double[24];
	@Field("Temperatur")
	public final double[] temperature = new double[24];
	@Field("Taupunkt")
	public final double[] dewPoint = new double[24];
	@Field("Luftdruck")
	public final double[] airPressure = new double[24];
	@Field("Niederschlag")
	public final double[] precipitation = new double[24];
	@Field("SchneeRegenNied")
	public final double[] sleetPrecipitation = new double[24];
	@Field("Sichtweite")
	public final double[] visibility = new double[24];
	@Field("Neberwahrsch")
	public final double[] foggProbability = new double[24];
}
