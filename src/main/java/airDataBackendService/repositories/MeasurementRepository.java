package airDataBackendService.repositories;

// import java.util.Date;
// import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
// import org.springframework.data.mongodb.repository.Query;

import airDataBackendService.database.Measurement;

public interface MeasurementRepository extends MongoRepository<Measurement, String>, MeasurementRepositoryCustom {

  // @Query("{'sensor_id': {$eq: ?0}, 'timestamp': {$gte: ?1, $lte: ?2}}")
}