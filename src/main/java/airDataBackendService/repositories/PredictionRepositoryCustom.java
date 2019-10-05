package airDataBackendService.repositories;

import java.util.Date;
import java.util.List;

import airDataBackendService.database.Prediction;

public interface PredictionRepositoryCustom {
  public void saveOrUpdate(Prediction p);

  public List<Prediction> findByHour(Date d);
}