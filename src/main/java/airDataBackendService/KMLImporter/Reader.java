package airDataBackendService.KMLImporter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.zip.ZipInputStream;


public class Reader 
{
	
	/**
	 * Extrahiert die neusten Wettervorhersagedaten von der opendata-Website des DWD und
	 * erstellt ein Forecast-Daten-Obbjekt. Es werden keine Daten auf der Harddrive abgelegt.
	 * 
	 * @return
	 * Ein Forecast-Daten-Obbjekt mit allen aktuellen Wettervorhersagedaten fuer Deutschland
	 */
	
	public static Forecast take()
	{
		try (BufferedInputStream in = new BufferedInputStream(new URL("https://opendata.dwd.de/weather/local_forecasts/mos/MOSMIX_S/all_stations/kml/MOSMIX_S_LATEST_240.kmz").openStream()))
				{
					ZipInputStream zin = new ZipInputStream(in);
					zin.getNextEntry();
					InputStream data = zin;
					
					DocumentBuilderFactory factory;
					DocumentBuilder builder = null;
					factory = DocumentBuilderFactory.newInstance();
					try 
					{
						builder = factory.newDocumentBuilder();
					} 
					catch (ParserConfigurationException e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
						in.close();
						zin.close();
						data.close();
						return null;
					}
					
					Document doc = null;
					try 
					{
						doc = builder.parse(data);
					} 
					catch (SAXException | IOException e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
						in.close();
						zin.close();
						data.close();
						return null;
					}
					
					Forecast forecast = new Forecast();
					
					forecast.times = getTimes(doc);
					
					getData(doc,forecast);
					in.close();
					zin.close();
					data.close();
					return forecast;
					
				} catch (IOException e) {
				    // handle exception
					return null;
				}
	}
	
	/**
	 * Extrahiert die Wettervorhersagedaten aus einer Mosmix-KML-Datei und
	 * erstellt ein Forecast-Daten-Obbjekt. 
	 * 
	 * @param kmlFile
	 * Datei die eingelesen werden soll
	 * @return
	 * Ein Forecast-Daten-Obbjekt mit allen Wettervorhersagedaten fuer Deutschland aus der Datei
	 */
	
	public static Forecast takeKML(File kmlFile)
	{
		
		DocumentBuilderFactory factory;
		DocumentBuilder builder = null;
		factory = DocumentBuilderFactory.newInstance();
		try 
		{
			builder = factory.newDocumentBuilder();
		} 
		catch (ParserConfigurationException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Document doc = null;
		try 
		{
			doc = builder.parse(kmlFile);
		} 
		catch (SAXException | IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Forecast forecast = new Forecast();
		
		forecast.times = getTimes(doc);
		
		getData(doc,forecast);
		
		return forecast;
	}
	
	/**
	 * Extrahiert die Wettervorhersagedaten aus einer Mosmix-KMZ-Datei und
	 * erstellt ein Forecast-Daten-Obbjekt. 
	 * 
	 * @param kmzFile
	 * Datei die eingelesen werden soll
	 * @return
	 * Ein Forecast-Daten-Obbjekt mit allen Wettervorhersagedaten fuer Deutschland aus der Datei
	 */
	
	public static Forecast takeKMZ(File kmzFile)
	{
		InputStream in;
		ZipInputStream zin;
		InputStream data;
		
		try 
		{
			in = new FileInputStream(kmzFile);
			zin = new ZipInputStream(in);
			zin.getNextEntry();
			data = zin;
		
			DocumentBuilderFactory factory;
			DocumentBuilder builder = null;
			factory = DocumentBuilderFactory.newInstance();
			try 
			{
				builder = factory.newDocumentBuilder();
			} 
			catch (ParserConfigurationException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				in.close();
				zin.close();
				data.close();
				return null;
			}
			
			Document doc = null;
			try 
			{
				doc = builder.parse(data);
			} 
			catch (SAXException | IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				in.close();
				zin.close();
				data.close();
				return null;
			}
			
			Forecast forecast = new Forecast();
			
			forecast.times = getTimes(doc);
			
			getData(doc,forecast);
			
			in.close();
			zin.close();
			data.close();
			
			return forecast;
			
		} 
		catch (IOException e1) 
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Liest das der KML-Datei zugrundeliegende Dokument ein und
	 * uebertraegt die Daten auf ein Forecast-Objekt
	 * 
	 * @param d
	 * Dokument das eingelesen wird
	 * @param f
	 * Forecast-Objekt auf das die Daten uebertragen werden
	 */
	
	private static void getData(Document d, Forecast f)
	{
		NodeList stationtable = d.getElementsByTagName("kml:Placemark");
		
		Element[] GerStaArray = reduceStationtableByLocation(stationtable,58.0,44.0,19.0,2.0);
		Element station;
		Node cordNode;
		Coordinate coordinate;
		StationData stationData;
		LinkedList<Coordinate> cordList = new LinkedList<Coordinate>();
		int dupIndex;
		
		for(int i=0; i<GerStaArray.length; i++)
		{
			station = GerStaArray[i];
			
			cordNode = station.getElementsByTagName("kml:coordinates").item(0);
			coordinate = Coordinate.kmlTextToCord(cordNode.getTextContent());
			
			dupIndex = cordList.indexOf(coordinate);

			if(dupIndex==-1)
			{
				cordList.add(coordinate);
				stationData = getStationData(station);
				f.cordConect.put(coordinate, stationData);
			}
			else if((!coordinate.trueEquals(cordList.get(dupIndex)))&&(coordinate.hight<cordList.get(dupIndex).hight))
			{
				cordList.remove(dupIndex);
				cordList.add(dupIndex,coordinate);
				stationData = getStationData(station);
				f.cordConect.put(coordinate, stationData);
			}	
		}
		
		Coordinate[] cordArray = cordList.toArray(new Coordinate[cordList.size()]);
		f.positionRegister = cordArray;
	}
	
	/**
	 * Liest die Stationsdaten aus einem Stationselement ein und erzeugt ein Stationsdaten-Objekt
	 * 
	 * @param stationElement
	 * Stationselement aus dem Dokument aus dem die Daten extrahiert werden
	 * @return
	 * Stationsdaten-Objekt mit allen relevanten Daten
	 */
	
	private static StationData getStationData(Element stationElement) 
	{
		StationData stationData = new StationData(elementName(stationElement));
		stationData.coordinate = Coordinate.kmlTextToCord(stationElement.getElementsByTagName("kml:coordinates").item(0).getTextContent());
		
		Element parameterRoot = (Element) stationElement.getElementsByTagName("kml:ExtendedData").item(0);
		NodeList parameterList = parameterRoot.getElementsByTagName("dwd:Forecast");
		Element parameterElement;
		Node parameterValNode;
		
		
		for(int i=0; i<parameterList.getLength(); i++)
		{
			parameterElement = (Element) parameterList.item(i);
			String att = parameterElement.getAttribute("dwd:elementName");
			
			if(att.equals("FF"))
			{
				parameterValNode = parameterElement.getElementsByTagName("dwd:value").item(0);
				String windSpeedString = parameterValNode.getTextContent();
				double[] windSpeedArray = StationData.stringToDoubArray(windSpeedString);
				
				stationData.windSpeedDataErrorClass = StationData.DoubArrayLinearComplete(windSpeedArray);
				stationData.windSpeedData = windSpeedArray;
			}
			else if(att.equals("FX1"))
			{
				parameterValNode = parameterElement.getElementsByTagName("dwd:value").item(0);
				String maxWindSpeedString = parameterValNode.getTextContent();
				double[] maxWindSpeedArray = StationData.stringToDoubArray(maxWindSpeedString);
				
				stationData.maxWindSpeedDataErrorClass = StationData.DoubArrayLinearComplete(maxWindSpeedArray);
				stationData.maxWindSpeedData = maxWindSpeedArray;
			}
			else if(att.equals("Rad1h"))
			{
				parameterValNode = parameterElement.getElementsByTagName("dwd:value").item(0);
				String sunIntensityString = parameterValNode.getTextContent();
				double[] sunIntensityArray = StationData.stringToDoubArray(sunIntensityString);
				
				stationData.sunIntensityDataErrorClass = StationData.DoubArrayLinearComplete(sunIntensityArray);
				stationData.sunIntensityData = sunIntensityArray;
			}
			else if(att.equals("SunD1"))
			{
				parameterValNode = parameterElement.getElementsByTagName("dwd:value").item(0);
				String sunDurationString = parameterValNode.getTextContent();
				double[] sunDurationArray = StationData.stringToDoubArray(sunDurationString);
				
				stationData.sunDurationDataErrorClass = StationData.DoubArrayLinearComplete(sunDurationArray);
				for(int x=0;x<sunDurationArray.length;x++)
				{
					sunDurationArray[x]=sunDurationArray[x]/60;
				}
				stationData.sunDurationData = sunDurationArray;
			}
			else if(att.equals("TTT"))
			{
				parameterValNode = parameterElement.getElementsByTagName("dwd:value").item(0);
				String temperatureString = parameterValNode.getTextContent();
				double[] temperatureArray = StationData.stringToDoubArray(temperatureString);
				
				stationData.temperatureDataErrorClass = StationData.DoubArrayLinearComplete(temperatureArray);
				for(int x=0;x<temperatureArray.length;x++)
				{
					temperatureArray[x]=Math.floor((temperatureArray[x]-273.15)*1000)/1000;
				}
				stationData.temperatureData = temperatureArray;
			}
			else if(att.equals("Td"))
			{
				parameterValNode = parameterElement.getElementsByTagName("dwd:value").item(0);
				String tauPunktString = parameterValNode.getTextContent();
				double[] tauPunktArray = StationData.stringToDoubArray(tauPunktString);
				
				stationData.tauPunktDataErrorClass = StationData.DoubArrayLinearComplete(tauPunktArray);
				for(int x=0;x<tauPunktArray.length;x++)
				{
					tauPunktArray[x]=Math.floor((tauPunktArray[x]-273.15)*1000)/1000;
				}
				stationData.tauPunktData = tauPunktArray;
			}
			else if(att.equals("PPPP"))
			{
				parameterValNode = parameterElement.getElementsByTagName("dwd:value").item(0);
				String luftdruckString = parameterValNode.getTextContent();
				double[] luftdruckArray = StationData.stringToDoubArray(luftdruckString);
				
				stationData.luftdruckDataErrorClass = StationData.DoubArrayLinearComplete(luftdruckArray);
				stationData.luftdruckData = luftdruckArray;
			}
			else if(att.equals("RR1c"))
			{
				parameterValNode = parameterElement.getElementsByTagName("dwd:value").item(0);
				String niederschlagString = parameterValNode.getTextContent();
				double[] niederschlagArray = StationData.stringToDoubArray(niederschlagString);
				
				stationData.niederschlagDataErrorClass = StationData.DoubArrayLinearComplete(niederschlagArray);
				stationData.niederschlagData = niederschlagArray;
			}
			else if(att.equals("RRS1c"))
			{
				parameterValNode = parameterElement.getElementsByTagName("dwd:value").item(0);
				String schneeregenNiederschlagString = parameterValNode.getTextContent();
				double[] schneeregenNiederschlagArray = StationData.stringToDoubArray(schneeregenNiederschlagString);
				
				stationData.schneeregenNiederschlagDataErrorClass = StationData.DoubArrayLinearComplete(schneeregenNiederschlagArray);
				stationData.schneeregenNiederschlagData = schneeregenNiederschlagArray;
			}
			else if(att.equals("VV"))
			{
				parameterValNode = parameterElement.getElementsByTagName("dwd:value").item(0);
				String visibilityString = parameterValNode.getTextContent();
				double[] visibilityArray = StationData.stringToDoubArray(visibilityString);
				
				stationData.visibilityDataErrorClass = StationData.DoubArrayLinearComplete(visibilityArray);
				stationData.visibilityData = visibilityArray;
			}
			else if(att.equals("wwM"))
			{
				parameterValNode = parameterElement.getElementsByTagName("dwd:value").item(0);
				String foggProbabilityString = parameterValNode.getTextContent();
				double[] foggProbabilityArray = StationData.stringToDoubArray(foggProbabilityString);
				
				stationData.foggProbabilityDataErrorClass = StationData.DoubArrayLinearComplete(foggProbabilityArray);
				stationData.foggProbabilityData = foggProbabilityArray;
			}
		}
		//
		return stationData;
	}
	
	/**
	 * Selektiert die Stationselemente in der Nodelist abhaengig davon, ob die Stationen sich innerhalb eines
	 * geografischen Quadrats befinden und erstellt ein Array mit allen positiven Ergebnissen.
	 * 
	 * @param nodeList
	 * Nodelist mit den Stationselementen die selektiert werden sollen
	 * @param latMax
	 * Latitude-Obergrenze des Quadrats
	 * @param latMin
	 * Latitude-Untergrenze des Quadrats
	 * @param lonMax
	 * Longitude-Obergrenze des Quadrats
	 * @param lonMin
	 * Longitude-Untergrenze des Quadrats
	 * @return
	 * Array mit allen Stationselementen innerhalb des Quadrats
	 */
	
	private static Element[] reduceStationtableByLocation(NodeList nodeList, double latMax, double latMin, double lonMax, double lonMin)
	{
		Element station;
		boolean valid;
		LinkedList<Element> list = new LinkedList<Element>();
		Element[] nodeArray = null;
		
		for(int i = 0; i<nodeList.getLength(); i++)
		{
			station = (Element) nodeList.item(i);
			NodeList cnl = station.getElementsByTagName("kml:coordinates");
			Node cordNode = cnl.item(0);
			String cordText = cordNode.getTextContent();
			Coordinate cord = Coordinate.kmlTextToCord(cordText);
			valid = cord.insideField(new Coordinate(latMax,lonMax), new Coordinate(latMin,lonMin));
			if(valid)
			{
				list.add(station);
			}
		}

		nodeArray = (Element[]) list.toArray(new Element[list.size()]);
		return nodeArray;
	}
	
	/**
	 * Erstellt einen Array mit allen Vorhersagezeitpunkten in den Daten
	 * 
	 * @param d
	 * Dokument das eingelesen wird
	 * @return
	 * Array mit allen vorhandenen Vorhersagezeitpunkten
	 */
	
	private static Date[] getTimes(Document d)
	{
		Date[] dates = new Date[240];
		Pattern pattern = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})[.]*");
		Matcher matcher;
		
		Node timetable = d.getElementsByTagName("dwd:ForecastTimeSteps").item(0);
		NodeList timelist = timetable.getChildNodes();
		int datecount = 0;

		for(int i=0; i<timelist.getLength(); i++)
		{
			Node time = timelist.item(i);
			matcher = pattern.matcher(time.getTextContent());
			
			if(matcher.find())
			{
				int year = Integer.parseInt(matcher.group(1))-1900;
				int month = Integer.parseInt(matcher.group(2))-1;
				int day = Integer.parseInt(matcher.group(3));
				int hour = Integer.parseInt(matcher.group(4));
				int min = Integer.parseInt(matcher.group(5));
				int sec = Integer.parseInt(matcher.group(6));
			
			Date date = new Date(year,month,day,hour,min,sec);
			dates[datecount] = date;
			datecount++;
			}
		}
		
		if(datecount<240)
		{
			return null;
		}
		
		return dates;
	}
	
	/**
	 * Gibt den Namen der Station aus dem Stationselement aus
	 * 
	 * @param e
	 * Gegebenes Stationselement
	 * @return
	 * Name der Station als String
	 */
	
	static private String elementName(Element e)
	{
		Node eNameN = e.getElementsByTagName("kml:description").item(0);
		return eNameN.getTextContent();
	}
	
}
	
	
