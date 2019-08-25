package airDataBackendService.services;

import airDataBackendService.database.Measurement;
import airDataBackendService.database.Sensor;
import airDataBackendService.repositories.MeasurementRepository;
import airDataBackendService.repositories.SensorRepository;
import airDataBackendService.rest.AirDataAPIResult;
import airDataBackendService.rest.BySensorResponse;
import airDataBackendService.rest.SensorDataValue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    /**
     * not used yet
     */
    public void importDataSet() {
        ResponseEntity<List<AirDataAPIResult>> response;
        try {
            response = restTemplate.exchange("https://api.luftdaten.info/static/v1/filter/type=SDS011", HttpMethod.GET,
                    null, new ParameterizedTypeReference<List<AirDataAPIResult>>() {
                    });
        } catch (RestClientException rce) {
            System.out.println(rce);
            // TODO: print error message
            return;
        }

        List<AirDataAPIResult> rawResult = response.getBody();

        Predicate<AirDataAPIResult> isOutdoor = e -> Objects.nonNull(e) && Objects.nonNull(e.getLocation())
                && e.getLocation().getIndoor() == 0;

        Predicate<AirDataAPIResult> hasP1Value = e -> e.getValues().stream()
                .anyMatch(val -> val.getValueType().equals("P1"));
        Predicate<AirDataAPIResult> hasP2Value = e -> e.getValues().stream()
                .anyMatch(val -> val.getValueType().equals("P2"));

        List<AirDataAPIResult> cleanResults = rawResult.stream() // turn list into a stream
                .filter(isOutdoor) // only keep outdoor sensors
                .filter(hasP1Value) // require a p1 value
                .filter(hasP2Value) // require a p2 value
                .collect(Collectors.toList());

        System.out.println(rawResult.size());
        System.out.println(cleanResults.size());

        class Measurement {
            public double p1;
            public double p2;
        }

        class MeasurementData {
            public double lat;
            public double lon;

            public Map<Long, Measurement> timestampToMeasurement;
        }

        Map<Long, MeasurementData> sensorIDToData = new HashMap<Long, MeasurementData>();

        for (AirDataAPIResult measurement : cleanResults) {
            airDataBackendService.rest.Sensor sensor = measurement.getSensor();
            if (sensor == null) {
                continue;
            }

            Long sensorId = new Long(sensor.getId());
            Long timestampInSec = new Long((long) Math.floor(measurement.getTimestamp().getTime() / 1000));

            double p1;
            double p2;

            try {
                p1 = measurement.getValues().stream().filter(e -> e.getValueType().equals("P1")).findFirst().get()
                        .getValue();
            } catch (Exception e) {
                continue;
            }

            if (p1 < 0) {
                continue;
            }

            try {
                p2 = measurement.getValues().stream().filter(e -> e.getValueType().equals("P2")).findFirst().get()
                        .getValue();
            } catch (NoSuchElementException e) {
                continue;
            }

            if (p2 < 0) {
                continue;
            }

            if (!sensorIDToData.containsKey(sensorId)) {
                MeasurementData data = new MeasurementData();
                data.lat = measurement.getLocation().getLatitude();
                data.lon = measurement.getLocation().getLongitude();
                data.timestampToMeasurement = new HashMap<Long, Measurement>();

                sensorIDToData.put(sensorId, data);
            }

            Measurement m = new Measurement();
            m.p1 = p1;
            m.p2 = p2;

            sensorIDToData.get(sensorId).timestampToMeasurement.put(timestampInSec, m);

        }
        for (Map.Entry<Long, MeasurementData> entry : sensorIDToData.entrySet()) {
            Long sensorId = entry.getKey();
            MeasurementData data = entry.getValue();

            // first, save the sensor to the "sensors"-database
            // TODO

            // second, save the measurements
            // TODO
        }
    }

    /**
     * Return all available sensors
     */
    public List<Sensor> getSensors() {
        List<Sensor> result = sensorRepository.findAll();
        importDataSet();
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
        // retrieve all relevant measurements from the database
        List<Measurement> allMeasurements = measurementRepository.getBySensor(sensor, timestamp);

        BySensorResponse response = new BySensorResponse();
        response.continuous = this.isContinuous(allMeasurements, timestamp);
        response.weatherReport = weatherDataService.getForecastFor(sensor, timestamp);
        if (response.continuous) {
            response.measurement = this.bestFit(allMeasurements, timestamp);
        }
        return response;
    }

}
