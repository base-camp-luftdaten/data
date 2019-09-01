package airDataBackendService.services;

import airDataBackendService.database.Measurement;
import airDataBackendService.database.Sensor;
import airDataBackendService.repositories.MeasurementRepository;
import airDataBackendService.repositories.SensorRepository;
import airDataBackendService.rest.AirDataAPIResult;
import airDataBackendService.rest.BySensorResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

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

    @Scheduled(fixedRate = 1000 * 60 * 4)
    public void importDataSet() {
        try {
            ResponseEntity<List<AirDataAPIResult>> response;
            try {
                response = restTemplate.exchange("https://api.luftdaten.info/static/v1/filter/type=SDS011",
                        HttpMethod.GET, null, new ParameterizedTypeReference<List<AirDataAPIResult>>() {
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
                m.p10 = p1;
                m.p25 = p2;
                m.timestamp = timestampInSec;

                sensorIDToData.get(sensorId).timestampToMeasurement.put(timestampInSec, m);

            }

            int duplicates = 0;
            int newMeasurements = 0;

            for (Map.Entry<Long, MeasurementData> entry : sensorIDToData.entrySet()) {
                Long sensorId = entry.getKey();
                MeasurementData data = entry.getValue();

                // first, save the sensor to the "sensors"-database
                Sensor s = new Sensor(sensorId.toString(), data.lat, data.lon);

                try {
                    sensorRepository.save(s);
                } catch (DuplicateKeyException e) {
                    duplicates++;
                }

                // second, save the measurements

                // sort the measurements into "buckets" of days where each "bucket" represents a
                // single day and contains a list of measurements
                Map<Date, List<Measurement>> daysToMeasurements = new HashMap<Date, List<Measurement>>();
                for (Map.Entry<Long, Measurement> measurementEntry : data.timestampToMeasurement.entrySet()) {
                    Long timestampInSec = measurementEntry.getKey();
                    Measurement measurement = measurementEntry.getValue();
                    long dayTimestamp = timestampInSec - (timestampInSec % 86400);
                    Date day = new Date(dayTimestamp * 1000);

                    if (!daysToMeasurements.containsKey(day)) {
                        daysToMeasurements.put(day, new ArrayList<Measurement>());
                    }

                    daysToMeasurements.get(day).add(measurement);
                }

                for (Map.Entry<Date, List<Measurement>> measurementEntry : daysToMeasurements.entrySet()) {
                    Date day = measurementEntry.getKey();
                    List<Measurement> measurements = measurementEntry.getValue();

                    List<Measurement> measurementsFromDB = measurementRepository
                            .getBySensorSingleDay(sensorId.toString(), day);

                    List<Measurement> nonDuplicates = new ArrayList<Measurement>();
                    for (Measurement a : measurements) {
                        if (!this.containsMeasurementWithSameTimestamp(a, measurementsFromDB)) {
                            nonDuplicates.add(a);
                        }
                    }

                    measurementRepository.addMeasurements(sensorId.toString(), day, nonDuplicates);
                    newMeasurements += nonDuplicates.size();
                }

            }

            System.out.println("Added " + (sensorIDToData.size() - duplicates) + " new sensors.");
            System.out.println("Added " + newMeasurements + " new measurements.");

        } catch (Exception e) {
            // TODO: print error message
            System.out.println(e);
        }
    }

    private boolean containsMeasurementWithSameTimestamp(Measurement a, List<Measurement> measurements) {
        for (Measurement b : measurements) {
            if (a.timestamp == b.timestamp) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return all available sensors
     */
    public List<Sensor> getSensors() {
        List<Sensor> result = sensorRepository.findAll();
        importDataSet();
        return result;
    }

    private Result hasMeasurementWithinTimestampWithOffset(List<Measurement> measurements, long timestampInSeconds,
            long offsetInSeconds) {
        for (Measurement m : measurements) {
            if (Math.abs(m.timestamp - timestampInSeconds) <= offsetInSeconds) {
                return new Result(true, "");
            }
        }
        return new Result(false, "No measurements found for timestamp: " + timestampInSeconds + " within +-"
                + offsetInSeconds + " seconds (" + offsetInSeconds / 3600 + " hours)");
    }

    /**
     * A list of measurements is continuous when there are no large gaps between
     * measurements. (Offset = gap)
     */
    private Result isContinuous(List<Measurement> measurements, long startTimeInSeconds) {
        long endTimeInSeconds = startTimeInSeconds - 7 * 24 * 60 * 60;
        long offsetInSeconds = 3 * 60 * 60;// 3 hours in seconds

        for (long i = startTimeInSeconds; i >= endTimeInSeconds; i -= 60 * 60) {
            Result r = this.hasMeasurementWithinTimestampWithOffset(measurements, i, offsetInSeconds);
            if (!r.getResult()) {
                return r;
            }
        }

        return new Result(true, "");
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
        Result continuousResult = this.isContinuous(allMeasurements, timestamp);
        response.continuous = continuousResult.getResult();
        response.weatherReport = weatherDataService.getForecastFor(sensor, timestamp);
        if (response.continuous) {
            response.measurement = this.bestFit(allMeasurements, timestamp);
        } else {
            response.reason = continuousResult.getReason();
        }
        return response;
    }

}

class Result {
    private boolean result;
    private String reason;

    public Result(boolean result, String reason) {
        this.result = result;
        this.reason = reason;
    }

    public boolean getResult() {
        return this.result;
    }

    public String getReason() {
        return this.reason;
    }
}