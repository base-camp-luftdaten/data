package airDataBackendService.repositories;

import java.util.List;

import airDataBackendService.database.Measurement;

public interface MeasurementRepositoryCustom {
  public List<Measurement> getBySensor(String sensor, long timestamp);
}