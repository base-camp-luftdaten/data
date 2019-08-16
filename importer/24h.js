// @ts-check

/**
 * @type {(url: string | Request, init?: RequestInit) => Promise<Response>}
 */
// @ts-ignore
const cheerio = require('cheerio');
const { downloadFromArchive, downloadPlain } = require('./downloader');
const { getCSVFiles } = require('./getCSVFiles');
const { chunkArray } = require('./utils');
const { cleanMeasurements } = require('./cleanMeasurements');
const { connectToCollection } = require('./database');
const { zeroPad } = require('./utils');
const { parseCSV } = require('./utils');
const fs = require('fs');
const logger = require('./logger');

// uh oh, global state
let addedMeasurements = 0;

/**
 * @param {string} fileName
 * @param {string} dateString
 * @param {import('mongodb').Collection} sensorDataCollection
 * @param {import('mongodb').Collection} sensorCollection
 */
async function processFile(fileName, dateString, sensorDataCollection, sensorCollection) {
  const url = `https://archive.luftdaten.info/${dateString}/${fileName}`;

  try {
    const rawText = await downloadPlain(url);
    const measurements = await parseCSV(rawText, {
      delimiter: ';',
      columns: true,
      skip_empty_lines: true
    });
    const records = await cleanMeasurements(measurements, url);

    const day = new Date(`${dateString}T00:00:00Z`);
    const sensors = new Map();

    records.forEach(function(record) {
      if (sensors.get(record.sensor_id) === undefined) {
        sensors.set(record.sensor_id, { lat: record.lat, lon: record.lon, measurements: new Map() });
      }

      const date = new Date(record.timestamp).getTime() / 1000;

      sensors.get(record.sensor_id).measurements.set(date, { P10: record.P10, P25: record.P25 });
    });

    await Promise.all(
      Array.from(sensors.entries()).map(async function([sensor_id, value]) {
        try {
          await sensorCollection.insertOne({ sensor_id: sensor_id, lat: value.lat, lon: value.lon });
        } catch (err) {
          if (err.code !== 11000) {
            console.error(err);
          }
        }

        const query = { sensor_id: sensor_id, day: day };
        const measurementsFromDB = await sensorDataCollection.findOne(query);
        if (measurementsFromDB !== null) {
          measurementsFromDB.measurements.forEach(function(measurement) {
            value.measurements.delete(measurement.timestamp);
          });
        }

        const measurements = [];
        value.measurements.forEach(function(value, key) {
          measurements.push({ timestamp: key, ...value });
        });

        const writeResult = await sensorDataCollection.updateOne(query, { $push: { measurements: { $each: measurements } } }, { upsert: true });

        if (writeResult.modifiedCount > 0 || writeResult.upsertedCount > 0) {
          addedMeasurements += measurements.length;
        }
      })
    );
  } catch (err) {
    logger.log(`An error occured while processing ${url}:\n${err}\nNotice: this did not stop the processing of the current set of CSV files\n\n`);
  }
}

function processSequentially(promises) {
  const last = promises.reduce(async (prevPromise, nextFn) => {
    await prevPromise;
    return nextFn();
  }, Promise.resolve());

  return last;
}

async function getEntireDay(dateString) {
  let dataClient;
  let sensorClient;

  try {
    const html = await downloadFromArchive(dateString);
    const csvFiles = getCSVFiles(html);
    /** @type {Array<Array<string>>} */
    const fileChunks = chunkArray(csvFiles, 1);

    // open db connection
    const [dataDB, sensorDataCollection] = await connectToCollection('sensordata');
    const [sensorDB, sensorCollection] = await connectToCollection('sensors');
    sensorDataCollection.createIndex({ sensor_id: 1, day: 1 }, { unique: true });
    sensorCollection.createIndex({ sensor_id: 1 }, { unique: true });
    dataClient = dataDB;
    sensorClient = sensorDB;

    const batchedFunctions = fileChunks.map(function(chunk, i) {
      return function() {
        const processes = chunk.map(function(fileName) {
          return processFile(fileName, dateString, sensorDataCollection, sensorCollection);
        });
        console.log(`Progress: ${i} / ${fileChunks.length}. Imported ${addedMeasurements} measurements so far. (${dateString})`);
        return Promise.all(processes);
      };
    });

    // Send off x requests at once, then wait until they're done before starting the next x requests
    await processSequentially(batchedFunctions);
  } catch (err) {
    logger.log(err);
  }

  await dataClient.close();
  await sensorClient.close();
}

/**
 * If today is 27.04.2019, the following snippet will turn the current date into a
 * formatted string, representing yesterday: "2019-04-26", because it is the date format
 * used by https://archive.luftdaten.info. We need to use yesterdays date because todays
 * dataset is not uploaded yet.
 */
function getLatestDate() {
  const today = new Date();
  const yesterday = new Date(new Date().setDate(today.getDate() - 1));
  return `${yesterday.getFullYear()}-${zeroPad(yesterday.getMonth() + 1)}-${zeroPad(yesterday.getDate())}`;
}

const singleDay = process.argv.includes('--single-day');

const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
let startDate = process.argv[2];
if (startDate === undefined || !dateRegex.test(startDate)) {
  startDate = getLatestDate();
}

if (singleDay) {
  addedMeasurements = 0;
  const before = new Date();
  console.log('Running in single-day mode.');
  console.log(`--- Downloading latest measurements from https://archive.luftdaten.info (${startDate}) ---`);
  getEntireDay(startDate)
    .then(() => {
      logger.log(
        `--- Added ${addedMeasurements} new measurements to the database (day ${startDate}) ---`,
        `--- Total time: ${Math.floor((new Date().getTime() - before.getTime()) / 1000)} seconds---`
      );
    })
    .catch(logger.log);
} else {
  downloadPlain('https://archive.luftdaten.info').then(function(text) {
    // @ts-ignore
    const $ = cheerio.load(text);

    const list = $('td a')
      .toArray()
      .map(function(element) {
        return element.children[0].data.replace('/', '');
      })
      .filter(function(text) {
        return !text.includes('.md');
      });

    const startDateIndex = list.findIndex(function(date) {
      return date === startDate;
    });

    list.splice(startDateIndex + 1);
    list.reverse();

    const todolist = list.map(function(date) {
      return async function() {
        console.log(`--- Downloading measurements from https://archive.luftdaten.info (${date}) ---`);
        const before = new Date();
        addedMeasurements = 0;
        try {
          await getEntireDay(date);
          logger.log(
            `--- Added ${addedMeasurements} new measurements to the database (day: ${date}) ---`,
            `--- Total time: ${Math.floor((new Date().getTime() - before.getTime()) / 1000)} seconds---`
          );

          fs.writeFile('./latest', date, function(err) {
            if (err) {
              return console.log(err);
            }
          });
        } catch (err) {
          console.error(err);
        }
      };
    });

    processSequentially(todolist)
      .then(console.log)
      .catch(console.error);
  });
}
