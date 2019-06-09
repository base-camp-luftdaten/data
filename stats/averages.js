db.getCollection('measurements').aggregate([{ $group: { _id: null, p10avg: { $avg: '$P10' }, p25avg: { $avg: '$P25' } } }]);
