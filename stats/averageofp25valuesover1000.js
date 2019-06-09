db.getCollection('measurements').aggregate([{ $match: { P25: { $gte: 1000 } } }, { $group: { _id: null, p25avg: { $avg: '$P25' } } }]);
