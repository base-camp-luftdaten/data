package airDataBackendService.database;

import java.util.ArrayList;
import java.util.List;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import airDataBackendService.database.Measurement;
import airDataBackendService.util.Box;

public class MeasurementRepositoryCustomImpl implements MeasurementRepositoryCustom {
  @Autowired
  MongoTemplate mongoTemplate;

  @Override
  public List<Measurement> customQuery(int limit, int offset, Box box, Date maxage) {
    List<AggregationOperation> operations = new ArrayList<AggregationOperation>();

    operations.add(Aggregation.sort(Direction.ASC, "timestamp"));
    operations.add(Aggregation.match(Criteria.where("timestamp").gte(maxage)));

    if (box != null) {
      operations.add(Aggregation.match(
          new Criteria().andOperator(Criteria.where("lat").gte(box.getLat1()), Criteria.where("lat").lte(box.getLat2()),
              Criteria.where("lon").gte(box.getLon1()), Criteria.where("lon").lte(box.getLon2()))));
    }

    // operations.add(Aggregation.group("sensorId").avg("p25").as("P25"));

    operations.add(Aggregation.skip((long) offset));
    operations.add(Aggregation.limit(limit));

    Aggregation agg = Aggregation.newAggregation(operations);

    return mongoTemplate.aggregate(agg, Measurement.class, Measurement.class).getMappedResults();
  }

  @Override
  public List<Sensor> getSensors() {
    return mongoTemplate.aggregate(
        Aggregation.newAggregation(Aggregation.group("sensorId").last("lon").as("lon").last("lat").as("lat")),
        Measurement.class, Sensor.class).getMappedResults();
  }

  // @Override
  // public List<Measurement> getBySensor(String sensor, int timestamp) {
  // long startTime = System.nanoTime();

  // List<Measurement> result = mongoTemplate.find(
  // Query.query(Criteria.where("sensorId").is(sensor).and("timestamp").gte(timestamp
  // - 10 * 60 * 1000)),
  // Measurement.class);

  // long endTime = System.nanoTime();
  // long duration = (endTime - startTime) / 1000000;

  // System.out.println("/bySensor took " + duration + " ms!");

  // return result;
  // }
}
