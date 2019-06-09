db.getCollection('measurements')
  .find({ P25: { $gte: 1000 } })
  .count();
