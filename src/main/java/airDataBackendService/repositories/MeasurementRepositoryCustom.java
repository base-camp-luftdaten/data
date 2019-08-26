package airDataBackendService.repositories;

import java.util.Date;
import java.util.List;

import airDataBackendService.database.Measurement;

public interface MeasurementRepositoryCustom {
  public List<Measurement> getBySensor(String sensor, long timestamp);

  public List<Measurement> getBySensorSingleDay(String sensor, Date day);

  public void addMeasurements(String sensor, Date day, List<Measurement> measurements);
}