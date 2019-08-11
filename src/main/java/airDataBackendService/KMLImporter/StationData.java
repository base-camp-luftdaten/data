package airDataBackendService.KMLImporter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StationData {
	public String name;
	public Coordinate coordinate;
	public double[] windSpeedData;
	public int windSpeedDataErrorClass;
	public String windSpeedUnit = "m/s";
	public double[] maxWindSpeedData;
	public int maxWindSpeedDataErrorClass;
	public String maxWindSpeedUnit = "m/s";
	public double[] sunIntensityData;
	public int sunIntensityDataErrorClass;
	public String sunIntensityUnit = "kJ/m2";
	public double[] sunDurationData;
	public int sunDurationDataErrorClass;
	public String sunDurationUnit = "Min";
	public double[] temperatureData;
	public int temperatureDataErrorClass;
	public String temperatureUnit = "�C";
	public double[] tauPunktData;
	public int tauPunktDataErrorClass;
	public String tauPunktUnit = "�C";
	public double[] luftdruckData;
	public int luftdruckDataErrorClass;
	public String luftdruckUnit = "Pascal";
	public double[] niederschlagData;
	public int niederschlagDataErrorClass;
	public String niederschlagUnit = "kg/m2";
	public double[] schneeregenNiederschlagData;
	public int schneeregenNiederschlagDataErrorClass;
	public String schneeregenNiederschlagUnit = "kg/m2";
	public double[] visibilityData;
	public int visibilityDataErrorClass;
	public String visibilityUnit = "Meter";
	public double[] foggProbabilityData;
	public int foggProbabilityDataErrorClass;
	public String foggProbabilityUnit = "%";

	/**
	 * Erzeugt ein Stationsdatenobjekt
	 * 
	 * @param n Name der Station deren Daten hier in diesem Objekt gespeichert
	 *          werden sollen
	 */

	public StationData(String n) {
		name = n;
	}

	/**
	 * Erzeugt aus einem Daten-String einer Mosmix-KML-Datei einen Array mit
	 * Double-Werten
	 * 
	 * @param s Daten-String aus einer Mosmix-KML-Datei
	 * @return Array mit 240 Double-Werten
	 */

	static double[] stringToDoubArray(String s) {
		Pattern pattern = Pattern.compile("([-\\d.]+)");
		Matcher matcher = pattern.matcher(s);
		double[] d = new double[240];
		int count = 0;
		String number;
		while (matcher.find()) {
			number = matcher.group();

			if (number.equals("-")) {
				d[count] = Double.NaN;
			} else {
				d[count] = Double.parseDouble(number);
			}
			count = count + 1;
		}
		return d;
	}

	/**
	 * Komplettiert einen Array mit Double-Werten bei dem Eintraege fehlen linear.
	 * 
	 * @param d Array der komplettiert werden soll
	 * @return Gibt -1 zurueck wenn der Array keine Daten enthielt, 0 wenn keine
	 *         Daten fehlten und 1-3 fuer verschiedene Schaedigungsgrade am
	 *         gegebenen Array
	 */

	static int DoubArrayLinearComplete(double[] d) {
		double lastMemory = Double.NaN;
		double direction = Double.NaN;
		int interruptionDuration = 0;
		int errorCount = 0;

		double stepLength;

		for (int i = 0; i < d.length; i++) {
			if (!Double.isNaN(d[i])) {
				if (!Double.isNaN(lastMemory) && (interruptionDuration > 0)) {
					direction = d[i] - lastMemory;

					stepLength = direction / (interruptionDuration + 1);
					for (int j = 1; interruptionDuration >= j; j++) {
						d[i - j] = d[i - j + 1] - stepLength;
					}
				} else if (Double.isNaN(lastMemory) && (interruptionDuration > 0)) {
					errorCount = errorCount + 300;
					for (int p = 1; interruptionDuration >= p; p++) {
						d[i - p] = d[i - p + 1];
					}
				}
				lastMemory = d[i];
				interruptionDuration = 0;
			} else {
				if (i == d.length - 1) {
					if (!Double.isNaN(lastMemory)) {
						errorCount = errorCount + 300;
						d[i] = lastMemory;
						for (int q = 1; interruptionDuration >= q; q++) {
							d[i - q] = d[i - q + 1];
						}
					} else {
						errorCount = errorCount + 100000;
					}
				}
				interruptionDuration = interruptionDuration + 1;
				errorCount = errorCount + 1;
			}
		}

		if (errorCount == 0) {
			return 0;
		} else if (errorCount < 300) {
			return 1;
		} else if (errorCount < 600) {
			return 2;
		} else if (errorCount < 1000) {
			return 3;
		} else {
			return -1;
		}
	}

}
