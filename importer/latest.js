const { downloadLatest } = require('./downloader');

const fallBackObject = Object.create(null);

function isP1(obj) {
  return obj.value_type === 'P1';
}

function isP2(obj) {
  return obj.value_type === 'P2';
}

/**
 * changes a measurement that comes from the `getLatest()` call
 * to look like it came from the archive
 */
function restructure(measurement) {
  const result = Object.create(null);

  result.P1 = (measurement.sensordatavalues.find(isP1) || fallBackObject).value;
  result.P2 = (measurement.sensordatavalues.find(isP2) || fallBackObject).value;
  result.timestamp = measurement.timestamp.replace(/ /, 'T');
  result.sensor_id = String(measurement.sensor.id);
  result.sensor_type = measurement.sensor.sensor_type.name;
  result.lat = measurement.location.latitude;
  result.lon = measurement.location.longitude;

  return result;
}

/**
 * Returns the measurements of the last 5 minutes
 */
async function getLatest() {
  const latest = await downloadLatest();

  return latest.map(restructure);
}

module.exports = {
  getLatest: getLatest
};
