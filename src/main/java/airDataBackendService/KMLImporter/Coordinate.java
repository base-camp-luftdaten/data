package airDataBackendService.KMLImporter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Coordinate implements Comparable<Object>
{
	double lat;
	double lon;
	double hight;
	
	/**
	 * Erstellt ein Koordinaten-Objekt ohne Hoehenangabe
	 * 
	 * @param lat1
	 * Latitude
	 * @param lon1
	 * Longitude
	 */
	
	public Coordinate(double lat1, double lon1)
	{
		lat=lat1;
		lon=lon1;
		hight=Double.NaN;
	}
	
	/**
	 * Erstellt ein Koordinaten-Objekt mit Hoehenangabe
	 * 
	 * @param lat1
	 * Latitude
	 * @param lon1
	 * Longitude
	 * @param H
	 * Hoehe ueber Normalnull
	 */
	
	public Coordinate(double lat1, double lon1, double H)
	{
		lat=lat1;
		lon=lon1;
		hight=H;
	}
	
	/**
	 * Berechnet die Distanz zwischen zwei Coordinaten
	 * 
	 * @param cord
	 * Koordinate zu der die Distanz berechnet wird
	 * @return
	 * DIstanz zwischen this und cord in Kilometer
	 */
	
	public double distance(Coordinate cord) 
	{
		double lat1=this.lat;
		double lon1=this.lon;
		double lat2=cord.lat;
		double lon2=cord.lon;
		
		if ((lat1 == lat2) && (lon1 == lon2)) {
			return 0;
		}
		else {
			double theta = lon1 - lon2;
			double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
			dist = Math.acos(dist);
			dist = Math.toDegrees(dist);
			dist = dist * 111.18957696;
			
			return dist;
		}
	}
	
	/**
	 * Spannt zwischen zwei Koordinaten ein Kugeloberfl�chensegment auf und pr�ft, ob
	 * diese Koordinate sich innerhalb des Segments befindet 
	 * 
	 * @param c1
	 * Eckpunkt 1 des Kugeloberfl�chensegments
	 * @param c2
	 * Eckpunkt 2 des Kugeloberfl�chensegments
	 * @return
	 * true wenn sich this innerhalb des Segments befindet und false wenn nicht.
	 */
	
	public boolean insideField(Coordinate c1, Coordinate c2)
	{
		if(this.lat>(Math.max(c1.lat, c2.lat)+0.002)||this.lon>(Math.max(c1.lon, c2.lon)+0.002)||this.lat<(Math.min(c1.lat, c2.lat)-0.002)||this.lon<(Math.min(c1.lon, c2.lon)-0.002))
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	/**
	 * Erzeugt aus dem standartisierten String in der Mosmix-KML-Datei, der die 
	 * jeweilige Position einer Station beschreibt, ein Koordinaten-Objekt
	 * 
	 * @param s
	 * Ein String aus der KML-Datei der die Position einer Station beschreibt
	 * @return
	 * Ein dem Text entsprechendes Koordinaten Objekt
	 */
	
	static public Coordinate kmlTextToCord(String s)
	{
		double h1;
		double lat1;
		double lon1;
		Pattern pattern = Pattern.compile("^([-\\d.]*),([-\\d.]*),([-\\d.]*)$");
		Matcher matcher = pattern.matcher(s);
		if(matcher.find())
		{
			lat1 = Double.parseDouble(matcher.group(2));
			lon1 = Double.parseDouble(matcher.group(1));
			
			if(matcher.groupCount()>2)
			{
				h1 = Double.parseDouble(matcher.group(3));
				return new Coordinate(lat1,lon1,h1);
			}
			else
			{
				return new Coordinate(lat1,lon1);
			}
		}
		else
		{
			return null;
		}
	}
	
	/**
	 * Gibt an, ob zwei Koordinaten in Position und Hoehe uebereinstimmen
	 * 
	 * @param cord
	 * Koordinate mit der die Gleichheit ueberprueft wird
	 * @return
	 * true wenn Position und Hoehe gleich, sonst false
	 */
	
	public boolean trueEquals(Coordinate cord)
	{
		double ydif = Math.abs(this.lat-cord.lat);
		double xdif = Math.abs(this.lon-cord.lon);
		if((xdif < 0.002) && (ydif < 0.002) && (this.hight==cord.hight))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Gibt an, ob zwei Koordinaten in Position uebereinstimmen
	 * 
	 * @param c1
	 * Koordinate mit der die Gleichheit ueberprueft wird
	 * @return
	 * true wenn Position gleich, sonst false
	 */
	
	@Override
	public boolean equals(Object c1)
	{
		Coordinate cord = (Coordinate) c1;
		double ydif = Math.abs(this.lat-cord.lat);
		double xdif = Math.abs(this.lon-cord.lon);
		if((xdif < 0.002) && (ydif < 0.002))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Vergleicht zwei Koordinaten
	 * 
	 * @return
	 * 1 wenn this eine hoehere Latitude bzw. gleiche Latitude und hoehere Longitude hat bzw.
	 * gleiche Latitude und Longitude und groessere Hoehe; 0 wenn exakt gleich; sonst -1
	 */
	
	@Override
	public int compareTo(Object c1)
	{
		Coordinate cord = (Coordinate) c1;
		if(this.lat>cord.lat || ((this.lat==cord.lat)&&(this.lon>cord.lon)) || ((this.lat==cord.lat)&&(this.lon==cord.lon)&&(this.hight>cord.hight)))
		{
			return 1;
		}
		else if((this.lat == cord.lat)&&(this.lon == cord.lon)&&(this.hight == cord.hight))
		{
			return 0;
		}
		else
		{
			return -1;
		}
	}
	
	/**
	 * Gibt einen String aus der die Koordinate beschreibt
	 * 
	 * @return
	 * String der die Koordinate beschreibt
	 */
	
	@Override
	public String toString()
	{
		return "Lat:"+this.lat+", Lon:"+this.lon;
	}

}
