package airDataBackendService.KMLImporter;

import java.util.Date;
import java.util.TreeMap;

public class Forecast {
	public Coordinate[] positionRegister;
	TreeMap<Coordinate, StationData> cordConect;
	public Date[] times;

	/**
	 * Erzeugt ein Wettervorhersagedaten-Objekt
	 * 
	 */
	public Forecast() {
		cordConect = new TreeMap<Coordinate, StationData>();
		times = new Date[240];
	}

	/**
	 * Findet die Koordinaten der naechstgelegenen Station ausgehend von der
	 * gegebenen Koordinate
	 * 
	 * @param cord Koordinate von der aus die naehste Station gesucht wird
	 * @return Koordinate der naechstgelegenen Station
	 */
	public Coordinate getNearest(Coordinate cord) {
		double minDist = Double.MAX_VALUE;
		double dist;
		Coordinate minCord = null;
		for (int i = 0; i < positionRegister.length; i++) {
			dist = cord.distance(positionRegister[i]);
			if (minDist > dist) {
				minCord = positionRegister[i];
				minDist = dist;
			}
		}
		return minCord;
	}

	/**
	 * Gibt die Stationsdaten der naechstgelegenen Station aus ausgehend von der
	 * gegebenen Latitude und Longitude
	 * 
	 * @param lat Latitude der gegebenen Koordinate
	 * @param lon Longitude der gegebenen Koordinate
	 * @return Stationsdatenobjekt der naechstgelegenen Station
	 */
	public StationData getStation(double lat, double lon) {
		Coordinate cord = getNearest(new Coordinate(lat, lon));
		return cordConect.get(cord);
	}

	/**
	 * Gibt fuer ein angegebenes Datum den naheliegensten Messzeitpunkt zurueck
	 * 
	 * @param date Das Datum fuer das ein Messzeitpunkt gesucht wird
	 * @return Datum des naheliegensten Messzeitpunkts
	 */
	static private Date roundDate(Date date) {
		Date ergebnis = (Date) date.clone();
		if (!((date.getMinutes() < 29) || ((date.getMinutes() == 29) && (date.getSeconds() <= 59)))) {
			ergebnis.setHours(date.getHours() + 1);
		}
		ergebnis.setMinutes(0);
		ergebnis.setSeconds(0);
		return ergebnis;
	}

	/**
	 * Gibt den Index des Messzeitpunkts in dem "times"-Register-Array an, der am
	 * naehsten an des gegebene Datum herankommt.
	 * 
	 * @param e Datum fuer den der Index gesucht wird
	 * @return Index des naehsten Messzeitpunkts
	 */
	private int dateIndex(Date e) {
		Date date = roundDate(e);
		long longIndex = ((date.getTime() - times[0].getTime()) / 1000) / 3600;
		int intIndex = (int) longIndex;
		return intIndex;
	}

	/**
	 * Gibt das erste verfuegbare Datum in den Wettervorhersagedaten zurueck
	 * 
	 * @return Datum-Objekt des ersten verfuegaberen Datensatzes
	 */
	public Date firstAvailableDate() {
		return times[0];
	}

	/**
	 * Gibt das letzte verfuegbare Datum in den Wettervorhersagedaten zurueck
	 * 
	 * @return Datum-Objekt des letzten verfuegaberen Datensatzes
	 */
	public Date lastAvailableDate() {
		return times[239];
	}

	/**
	 * Gibt fuer einen Daten-Array mit Double-Werten einen Array zurueck der nurnoch
	 * die entsprechenden Werte innerhalb eines bestimmten Zeitraums enthaelt
	 * 
	 * @param start     Start-Datum des Zeitraums
	 * @param end       End-Datum des Zeitraums
	 * @param dataArray Daten-Array aus dem der gewuenschte Datensatz extrahiert
	 *                  wird
	 * @return Datensatzausschnitt der zum angegebenen Zeitraum gehoert
	 */
	private double[] dateDouble(Date start, Date end, double[] dataArray) {
		int indexStart = 0;
		int indexEnde = 239;

		if (start == null && end == null) {
			return dataArray;
		} else if (start == null) {
			indexStart = 0;
			indexEnde = dateIndex(end);
		} else if (end == null) {
			indexStart = dateIndex(start);
			indexEnde = 239;
		} else {
			indexStart = dateIndex(start);
			indexEnde = dateIndex(end);
		}

		double[] d = new double[indexEnde - indexStart + 1];

		for (int i = indexStart; i <= indexEnde; i++) {
			d[i - indexStart] = dataArray[i];
		}
		return d;
	}

	/**
	 * Gibt einen Array mit den Windvorhersagedaten aus dem angegebenen Zeitraum
	 * zurueck
	 * 
	 * @param start Start-Datum des Zeitraums
	 * @param end   End-Datum des Zeitraums
	 * @param s     Stationsdaten aus dem die Werte entnommen werden
	 * @return Array mit den Windvorhersagedaten aus dem angegebenen Zeitraum
	 */
	public double[] windgeschwindigkeit(Date start, Date end, StationData s) {
		return dateDouble(start, end, s.windSpeedData);
	}

	/**
	 * Gibt einen Array mit den Maximums-Windvorhersagedaten aus dem angegebenen
	 * Zeitraum zurueck
	 * 
	 * @param start Start-Datum des Zeitraums
	 * @param end   End-Datum des Zeitraums
	 * @param s     Stationsdaten aus dem die Werte entnommen werden
	 * @return Array mit den Maximums-Windvorhersagedaten aus dem angegebenen
	 *         Zeitraum
	 */
	public double[] maxWindgeschwindigkeit(Date start, Date end, StationData s) {
		return dateDouble(start, end, s.maxWindSpeedData);
	}

	/**
	 * Gibt einen Array mit den Sonneneinstrahlungsvorhersagedaten aus dem
	 * angegebenen Zeitraum zurueck
	 * 
	 * @param start Start-Datum des Zeitraums
	 * @param end   End-Datum des Zeitraums
	 * @param s     Stationsdaten aus dem die Werte entnommen werden
	 * @return Array mit den Sonneneinstrahlungsvorhersagedaten aus dem angegebenen
	 *         Zeitraum
	 */
	public double[] sonnenEinstrahlung(Date start, Date end, StationData s) {
		return dateDouble(start, end, s.sunIntensityData);
	}

	/**
	 * Gibt einen Array mit den Sonnendauervorhersagedaten aus dem angegebenen
	 * Zeitraum zurueck
	 * 
	 * @param start Start-Datum des Zeitraums
	 * @param end   End-Datum des Zeitraums
	 * @param s     Stationsdaten aus dem die Werte entnommen werden
	 * @return Array mit den Sonnendauervorhersagedaten aus dem angegebenen Zeitraum
	 */
	public double[] sonnenDauer(Date start, Date end, StationData s) {
		return dateDouble(start, end, s.sunDurationData);
	}

	/**
	 * Gibt einen Array mit den Temperaturvorhersagedaten aus dem angegebenen
	 * Zeitraum zurueck
	 * 
	 * @param start Start-Datum des Zeitraums
	 * @param end   End-Datum des Zeitraums
	 * @param s     Stationsdaten aus dem die Werte entnommen werden
	 * @return Array mit den Temperaturvorhersagedaten aus dem angegebenen Zeitraum
	 */
	public double[] temperatur(Date start, Date end, StationData s) {
		return dateDouble(start, end, s.temperatureData);
	}

	/**
	 * Gibt einen Array mit den Taupunktvorhersagedaten aus dem angegebenen Zeitraum
	 * zurueck
	 * 
	 * @param start Start-Datum des Zeitraums
	 * @param end   End-Datum des Zeitraums
	 * @param s     Stationsdaten aus dem die Werte entnommen werden
	 * @return Array mit den Taupunktvorhersagedaten aus dem angegebenen Zeitraum
	 */
	public double[] taupunkt(Date start, Date end, StationData s) {
		return dateDouble(start, end, s.tauPunktData);
	}

	/**
	 * Gibt einen Array mit den Luftdruckvorhersagedaten aus dem angegebenen
	 * Zeitraum zurueck
	 * 
	 * @param start Start-Datum des Zeitraums
	 * @param end   End-Datum des Zeitraums
	 * @param s     Stationsdaten aus dem die Werte entnommen werden
	 * @return Array mit den Luftdruckvorhersagedaten aus dem angegebenen Zeitraum
	 */
	public double[] luftdruck(Date start, Date end, StationData s) {
		return dateDouble(start, end, s.luftdruckData);
	}

	/**
	 * Gibt einen Array mit den Niederschlagvorhersagedaten aus dem angegebenen
	 * Zeitraum zurueck
	 * 
	 * @param start Start-Datum des Zeitraums
	 * @param end   End-Datum des Zeitraums
	 * @param s     Stationsdaten aus dem die Werte entnommen werden
	 * @return Array mit den Niederschlagvorhersagedaten aus dem angegebenen
	 *         Zeitraum
	 */
	public double[] niederschlag(Date start, Date end, StationData s) {
		return dateDouble(start, end, s.niederschlagData);
	}

	/**
	 * Gibt einen Array mit den Schneeregenniederschlagvorhersagedaten aus dem
	 * angegebenen Zeitraum zurueck
	 * 
	 * @param start Start-Datum des Zeitraums
	 * @param end   End-Datum des Zeitraums
	 * @param s     Stationsdaten aus dem die Werte entnommen werden
	 * @return Array mit den Schneeregenniederschlagvorhersagedaten aus dem
	 *         angegebenen Zeitraum
	 */
	public double[] schneeregenNiederschlag(Date start, Date end, StationData s) {
		return dateDouble(start, end, s.schneeregenNiederschlagData);
	}

	/**
	 * Gibt einen Array mit den Sichtweitevorhersagedaten aus dem angegebenen
	 * Zeitraum zurueck
	 * 
	 * @param start Start-Datum des Zeitraums
	 * @param end   End-Datum des Zeitraums
	 * @param s     Stationsdaten aus dem die Werte entnommen werden
	 * @return Array mit den Sichtweitevorhersagedaten aus dem angegebenen Zeitraum
	 */
	public double[] sichtweite(Date start, Date end, StationData s) {
		return dateDouble(start, end, s.visibilityData);
	}

	/**
	 * Gibt einen Array mit den Nebelwahrscheinlichkeitvorhersagedaten aus dem
	 * angegebenen Zeitraum zurueck
	 * 
	 * @param start Start-Datum des Zeitraums
	 * @param end   End-Datum des Zeitraums
	 * @param s     Stationsdaten aus dem die Werte entnommen werden
	 * @return Array mit den Nebelwahrscheinlichkeitvorhersagedaten aus dem
	 *         angegebenen Zeitraum
	 */
	public double[] nebelWahrscheinlichkeit(Date start, Date end, StationData s) {
		return dateDouble(start, end, s.foggProbabilityData);
	}

	/**
	 * Gibt einen Array mit den Zeitschritten der Vorhersagedaten aus dem
	 * angegebenen Zeitraum zurueck
	 * 
	 * @param start Start-Datum des Zeitraums
	 * @param end   End-Datum des Zeitraums
	 * @return Array mit den Vorhersagezeitpunkten aus dem angegebenen Zeitraum als
	 *         Datum-Array
	 */
	public Date[] zeitschritte(Date start, Date end) {
		int startIndex = 0;
		int endIndex = 239;

		if (start == null && end == null) {
			return times.clone();
		} else if (start == null) {
			startIndex = 0;
			endIndex = dateIndex(end);
		} else if (end == null) {
			startIndex = dateIndex(start);
			endIndex = 239;
		} else {
			startIndex = dateIndex(start);
			endIndex = dateIndex(end);
		}

		Date[] datums = new Date[endIndex - startIndex + 1];

		for (int i = startIndex; i <= endIndex; i++) {
			datums[i - startIndex] = times[i];
		}
		return datums;
	}
}
