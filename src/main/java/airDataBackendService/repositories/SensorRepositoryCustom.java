package airDataBackendService.repositories;

import airDataBackendService.database.Sensor;

public interface SensorRepositoryCustom {
  public Sensor findBySensorId(String sensorId);
}