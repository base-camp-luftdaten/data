package airDataBackendService.repositories;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import airDataBackendService.database.Prediction;

public class PredictionRepositoryCustomImpl implements PredictionRepositoryCustom {
  @Autowired
  MongoTemplate mongoTemplate;

  public void saveOrUpdate(Prediction p) {
    Query query = new Query(Criteria.where("sensor_id").is(p.sensor_id).and("hour").is(p.hour));

    Update update = new Update().set("p10", p.p10).set("p25", p.p25);

    mongoTemplate.upsert(query, update, Prediction.class);
  }

  public List<Prediction> findByHour(Date d) {
    Query query = new Query(Criteria.where("hour").is(d));
    return mongoTemplate.find(query, Prediction.class);
  }
}