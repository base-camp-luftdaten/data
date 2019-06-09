db.getCollection('measurements')
  .find({ P10: { $gte: 2000 } })
  .count();
