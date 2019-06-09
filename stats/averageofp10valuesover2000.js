db.getCollection('measurements').aggregate([{ $match: { P10: { $gte: 2000 } } }, { $group: { _id: null, p10avg: { $avg: '$P10' } } }]);
