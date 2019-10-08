package airDataBackendService.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import airDataBackendService.database.Sensor;

public class SensorRepositoryCustomImpl implements SensorRepositoryCustom {
  @Autowired
  MongoTemplate mongoTemplate;

  public Sensor findBySensorId(String sensorId) {
    Query query = new Query(Criteria.where("sensor_id").is(sensorId));
    return mongoTemplate.findOne(query, Sensor.class);
  }
}