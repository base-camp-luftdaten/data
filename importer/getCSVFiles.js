const cheerio = require('cheerio');

/**
 * Given the raw HTML page, try to extract the individual csv files as an array, such as:
 *
 * [
 * '2019-04-26_sds011_sensor_467.csv',
 * '2019-04-26_sds011_sensor_471.csv',
 * ...
 * ]
 * @return {Array<string>}
 */
function getCSVFiles(htmlString) {
  const $ = cheerio.load(htmlString);
  return $('tr a')
    .toArray()
    .map(el => (el && el.attribs && typeof el.attribs.href === 'string' ? el.attribs.href : false))
    .filter(el => {
      // only grab sds011 sensors
      return el && /_(sds011_sensor)_.+\.csv$/.test(el) && !el.includes('indoor');
    });
}

module.exports = {
  getCSVFiles: getCSVFiles
};
