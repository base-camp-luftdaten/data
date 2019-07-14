package airDataBackendService.database;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import airDataBackendService.database.DailyMeasurements;
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

  private Date subtractDays(long timestampInMillis, int daysToSubtract) {
    long daysToSubtractInMillis = daysToSubtract * 24 * 60 * 60 * 1000;
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    calendar.setTimeInMillis(timestampInMillis - daysToSubtractInMillis);
    calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 0, 0,
        0);

    return calendar.getTime();
  }

  @Override
  public List<Measurement> getBySensor(String sensor, long timestampInSeconds) {
    long threshold = 3 * 60 * 60; // 3 hours represented in seconds
    long from = timestampInSeconds - 7 * 24 * 60 * 60 - threshold;
    long to = timestampInSeconds + threshold;

    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    calendar.setTimeInMillis((long) timestampInSeconds * 1000);
    calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 0, 0,
        0);
    long startTimeInMillis = calendar.getTimeInMillis();

    Query query = new Query(Criteria.where("sensor_id").is(sensor).orOperator(
        Criteria.where("day").is(this.subtractDays(startTimeInMillis, -1)),
        Criteria.where("day").is(this.subtractDays(startTimeInMillis, 0)),
        Criteria.where("day").is(this.subtractDays(startTimeInMillis, 1)),
        Criteria.where("day").is(this.subtractDays(startTimeInMillis, 2)),
        Criteria.where("day").is(this.subtractDays(startTimeInMillis, 3)),
        Criteria.where("day").is(this.subtractDays(startTimeInMillis, 4)),
        Criteria.where("day").is(this.subtractDays(startTimeInMillis, 5)),
        Criteria.where("day").is(this.subtractDays(startTimeInMillis, 6)),
        Criteria.where("day").is(this.subtractDays(startTimeInMillis, 7)),
        Criteria.where("day").is(this.subtractDays(startTimeInMillis, 8))));

    List<DailyMeasurements> days = mongoTemplate.find(query, DailyMeasurements.class);

    if (days == null) {
      return new ArrayList<Measurement>(0);
    }

    List<Measurement> allMeasurements = new ArrayList<Measurement>(0);

    for (DailyMeasurements dm : days) {
      for (Measurement m : dm.measurements) {
        if (m.timestamp >= from && m.timestamp <= to) {
          allMeasurements.add(m);
        }
      }
    }

    return allMeasurements;
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
