package airDataBackendService.rest;

import airDataBackendService.database.Prediction;
import airDataBackendService.database.Sensor;
import airDataBackendService.services.AirDataHandlerService;
import airDataBackendService.util.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "api/measurements")
public class AirDataController {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AirDataHandlerService airDataHandlerService;

    @GetMapping(value = "sensors", produces = "application/json")
    public Map<String, Location> getAllSensors() {
        Map<String, Location> map = new HashMap<String, Location>();

        for (Sensor s : airDataHandlerService.getSensors()) {
            Location location = new Location();
            location.lat = s.lat;
            location.lon = s.lon;

            map.put(s.id, location);
        }

        return map;
    }

    @GetMapping(value = "bySensor", produces = "application/json")
    public BySensorResponse getMeasurementsBySensor(@RequestParam(value = "sensor", required = true) String sensor,
            @RequestParam(value = "timestamp", required = true) long timestamp) {

        return airDataHandlerService.getBySensor(sensor, timestamp);
    }

    @GetMapping(value = "bySensorUntilNow", produces = "application/json")
    public List<BySensorResponse> getMeasurementsBySensorUntilNow(@RequestParam(value = "sensor", required = true) String sensor,
            @RequestParam(value = "timestamp", required = true) long timestamp) {

        return airDataHandlerService.getBySensorUntilNow(sensor, timestamp);
    }

    @GetMapping(value = "bySensorWithoutContinuous", produces = "application/json")
    public BySensorResponse getMeasurementsBySensorWithoutContinuous(
            @RequestParam(value = "sensor", required = true) String sensor,
            @RequestParam(value = "timestamp", required = true) long timestamp) {

        return airDataHandlerService.getBySensorWithoutContinuous(sensor, timestamp);
    }

    @PostMapping(value = "updatePredictions")
    public void updatePredictions(@RequestBody PredictionUpdate prediction) {
        airDataHandlerService.updatePredictions(prediction);
    }

    @GetMapping(value = "predictions", produces = "application/json")
    public List<Prediction> getPredictions(@RequestParam(value = "timestamp", required = true) long timestamp) {
        return airDataHandlerService.getPredictions(timestamp);
    }
}
