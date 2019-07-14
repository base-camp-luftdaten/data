package airDataBackendService.KMLImporter;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class KMLPersistenceService {

//    @Scheduled(cron = "0 */2 * * *")
    public void importKMLFiles(){
        Forecast forecast = Reader.take();
        //TODO persistance into database
    }
}



//EXAMPLE
/*
		Forecast forecast = Reader.take();
		//Forecast forecast = Reader.takeKML(new File("MOSMIX_S_2019060515_240.kml"));
		//Forecast forecast = Reader.takeKMZ(new File("MOSMIX_S_2019060515_240.kmz"));

		System.out.println("Stationen online: "+forecast.positionRegister.length);

		Date von = forecast.firstAvailableDate();
		Date bis = forecast.lastAvailableDate();

		//Date(int year -1900, int month -1, int date, int hrs, int min)
		Date example = new Date(119, 5, 3, 12, 0); // 03.06.2019, 12:00

		double lat = 53.599688;
		double lon = 9.933204;

		//Aus effizienzgruenden aufgeteilt, um mehre Funktionsaufrufe mit dem selben
		//StationData-Objekt zu ermoeglichen, ohne das dies neu gesucht werden muss.
		StationData station = forecast.getStation(lat,lon);
		double[] temp = forecast.temperatur(von, bis, station);
		//Die zugehoerigen Zeitpunkte zu den Vorhersagen
		Date[] times = forecast.zeitschritte(von, bis);
		//ACHTUNG ES MUSS GELTEN: forecast.firstAvailableDate() <= von <= bis <= forecast.lastAvailableDate()
		//ANDERNFALLS TRITT EINE ARRAY-INDEX-OUT-OF-BOUNDS-EXEPTION AUF

		//Konsolen Ausgabe
		System.out.println(station.name+": "+station.coordinate.toString()+
				", Distanz: "+station.coordinate.distance(new Coordinate(lat,lon))+" km");
		for(int i = 0; i < times.length; i++)
		{
			System.out.println(times[i].toString()+": "+temp[i]+"�C");
		}
 */
