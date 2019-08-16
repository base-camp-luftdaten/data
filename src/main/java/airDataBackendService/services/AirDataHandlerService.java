package airDataBackendService.services;

import airDataBackendService.database.HourlyWeatherReport;
import airDataBackendService.database.Measurement;
import airDataBackendService.database.Sensor;
import airDataBackendService.repositories.MeasurementRepository;
import airDataBackendService.repositories.SensorRepository;
import airDataBackendService.rest.AirDataAPIResult;
import airDataBackendService.rest.BySensorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class AirDataHandlerService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WeatherDataService weatherDataService;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private MeasurementRepository measurementRepository;

    @Value("${changeable.restUrl}")
    private String restUrl;

    public void importDataSet() {
        ResponseEntity<List<AirDataAPIResult>> response = restTemplate.exchange(
                "https://api.luftdaten.info/static/v1/filter/type=SDS011", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<AirDataAPIResult>>() {
                });

        List<AirDataAPIResult> result = response.getBody();

        System.out.println(result.size());
    }

    /**
     * Return all available sensors
     */
    public List<Sensor> getSensors() {
        List<Sensor> result = sensorRepository.findAll();
        return result;
    }

    private boolean hasMeasurementWithinTimestampWithOffset(List<Measurement> measurements, long timestampInSeconds,
            long offsetInSeconds) {
        for (Measurement m : measurements) {
            if (Math.abs(m.timestamp - timestampInSeconds) <= offsetInSeconds) {
                return true;
            }
        }
        return false;
    }

    /**
     * A list of measurements is continuous when there are no large gaps between
     * measurements. (Offset = gap)
     */
    private boolean isContinuous(List<Measurement> measurements, long startTimeInSeconds) {
        long endTimeInSeconds = startTimeInSeconds - 7 * 24 * 60 * 60;
        long offsetInSeconds = 3 * 60 * 60;// 3 hours in seconds

        for (long i = startTimeInSeconds; i >= endTimeInSeconds; i -= 60 * 60) {
            if (!this.hasMeasurementWithinTimestampWithOffset(measurements, i, offsetInSeconds)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the best fit measurement (by timestamp) for a certain timestamp.
     */
    private Measurement bestFit(List<Measurement> measurements, long timestamp) {
        Measurement result = new Measurement();
        result.timestamp = Long.MAX_VALUE;

        for (Measurement m : measurements) {
            if (Math.abs(result.timestamp - timestamp) > Math.abs(m.timestamp - timestamp)) {
                result = m;
            }
        }

        return result;
    }

    public BySensorResponse getBySensor(String sensor, long timestamp) {
        weatherDataService.test();

        // retrieve all relevant measurements from the database
        List<Measurement> allMeasurements = measurementRepository.getBySensor(sensor, timestamp);

        BySensorResponse response = new BySensorResponse();
        response.continuous = this.isContinuous(allMeasurements, timestamp);
        if (response.continuous) {
            response.measurement = this.bestFit(allMeasurements, timestamp);
            response.weatherReport = weatherDataService.getForecastFor(sensor, timestamp);
        }
        return response;
    }

}
