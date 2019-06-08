package airDataBackendService.database;

import java.util.Date;
import java.util.List;

import airDataBackendService.util.Box;

public interface MeasurementRepositoryCustom {
  public List<Measurement> customQuery(int limit, int offset, Box box, Date maxage);

  public List<Sensor> getSensors();
}