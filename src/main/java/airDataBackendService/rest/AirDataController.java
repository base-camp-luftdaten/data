package airDataBackendService.rest;

import airDataBackendService.database.Sensor;
import airDataBackendService.services.AirDataHandlerService;
import airDataBackendService.util.Location;

import java.util.HashMap;
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
}
