package airDataBackendService.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import airDataBackendService.interpolation.MatAccess;
import airDataBackendService.repositories.HeatmapRepository;

@Controller
@CrossOrigin(origins = "*")
@RequestMapping(path = "heatmap")
public class HeatmapController {

  @Autowired
  HeatmapRepository heatmapRepository;

  @Value("${secrets.apiKey}")
  private String apiKey;

  /**
   * Takes timestamp in seconds, returns timestamp in milliseconds
   */
  private long roundToNearestHour(long startTime) {
    long nearestHour = (long) Math.round(startTime / 3600) * (long) 3600000;
    if (startTime % 3600 > 1800) {
      nearestHour += 3600000;
    }

    return nearestHour;
  }

  @GetMapping("/")
  public ResponseEntity<String> getSingleHeatmap(@RequestParam(value = "timestamp", required = true) long timestamp) {

    long nearestHour = Math.round(roundToNearestHour(timestamp) / 1000);
    InputStream inputStream = heatmapRepository.findByFilename("data-" + nearestHour);

    if (inputStream == null) {
      return null;
    }

    MatAccess m = new MatAccess(inputStream);

    JSONArray l = m.pointArray(16, 55, 5, 47, 10000, true, false);
    // public JSONArray pointArray(double lonMax, -> Longitude Obergrenze
    // double latMax, -> Latitude Obergrenze
    // double lonMin, -> Longitude Untergrenze
    // double latMin, -> Latitude Untergrenze
    // int posiblePoints, -> Anzahl der gewünschten Heatmap Punkte n
    // boolean finish, -> True: Fügt Extrapunkte Am Rand ein um Streifen am Rand zu
    // vermeiden, es können jedoch <sqrt(n)*2 Punkte dazu kommen
    // boolean P1P2) -> False: Heatmap für P1; True: Heatmap für P2

    return ResponseEntity.ok().body(l.toJSONString());
  }

  @PostMapping("/")
  public ResponseEntity<String> uploadHeatmap(@RequestParam(value = "file", required = true) MultipartFile file,
      @RequestParam(value = "apiKey", required = true) String apiKey,
      @RequestParam(value = "timestamp", required = true) long timestamp) {

    long nearestHour = Math.round(roundToNearestHour(timestamp) / 1000);

    if (!this.apiKey.equals(apiKey)) {
      return ResponseEntity.status(401).body("invalid api key");
    }

    try {
      InputStream inputStream = file.getInputStream();
      heatmapRepository.store(inputStream, "data-" + nearestHour);
    } catch (IOException e) {
      return ResponseEntity.status(500).body(e.toString());
    }

    return ResponseEntity.ok().body("ok");
  }

}