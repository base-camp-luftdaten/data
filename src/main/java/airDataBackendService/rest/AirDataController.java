package airDataBackendService.rest;

import airDataBackendService.database.Measurement;
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
@RequestMapping(path = "api/measurements")
public class AirDataController {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AirDataHandlerService airDataHandlerService;

    @GetMapping(value = "dust", produces = "application/json")
    public List<Measurement> getDustData(@RequestParam(value = "maxage", required = false) String maxage,
            @RequestParam(value = "box", required = false) String box,
            @RequestParam(value = "country", defaultValue = "DE") String country,
            @RequestParam(value = "limit", defaultValue = "1000") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {
        return airDataHandlerService.getDustData(maxage, box, country, limit, offset);
    }

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
    public List<Measurement> getMeasurementsBySensor(@RequestParam(value = "sensor", required = true) String sensor,
            @RequestParam(value = "timestamp", required = true) long timestamp) {
        return airDataHandlerService.getBySensor(sensor, timestamp);
    }
}
