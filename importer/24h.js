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

// uh oh, global state
let addedMeasurements = 0;

/**
 * @param {string} fileName
 * @param {string} dateString
 * @param {import('mongodb').Collection} dbCollection
 */
function processFile(fileName, dateString, dbCollection) {
  const url = `https://archive.luftdaten.info/${dateString}/${fileName}`;

  return new Promise(async resolve => {
    try {
      const rawText = await downloadPlain(url);
      const measurements = await parseCSV(rawText, {
        delimiter: ';',
        columns: true,
        skip_empty_lines: true
      });
      const records = await cleanMeasurements(measurements, url);

      const day = new Date(`${dateString}T00:00:00Z`);

      await Promise.all(
        records.map(async function(record) {
          try {
            // TODO insert/update sensor location
            const date = new Date(record.timestamp).getTime() / 1000;

            const query = { sensor_id: record.sensor_id, day: day };
            const measurementToAdd = { P10: record.P10, P25: record.P25, timestamp: date };

            try {
              await dbCollection.updateOne(query, { $set: { day: day } }, { upsert: true });
            } catch (err) {
              if (err.code === 11000) {
                await dbCollection.updateOne(query, { $set: { day: day } }, { upsert: true });
              } else {
                throw err;
              }
            }

            const sameTimestampMeasurement = await dbCollection.findOne({ ...query, measurements: { $elemMatch: { timestamp: date } } });
            if (sameTimestampMeasurement !== null) {
              // measurement is already in db or there are 2 measurements at the exact same timstamp (in which case we ignore the second measurement)
              return;
            }

            const result = await dbCollection.updateOne(query, { $push: { measurements: measurementToAdd } });
            addedMeasurements += result.modifiedCount;
          } catch (err) {
            console.log(`Got "${err.message}" when trying to write ${url} into database`);
          }
        })
      );
    } catch (err) {
      console.error(`An error occured while processing ${url}:\n${err}\nNotice: this did not stop the processing of the current set of CSV files\n\n`);
    }

    resolve();
  });
}

function processSequentially(promises) {
  const last = promises.reduce(async (prevPromise, nextFn) => {
    await prevPromise;
    return nextFn();
  }, Promise.resolve());

  return last;
}

async function getEntireDay(dateString) {
  let dbClient;

  try {
    const html = await downloadFromArchive(dateString);
    const csvFiles = getCSVFiles(html);
    /** @type {Array<Array<string>>} */
    const fileChunks = chunkArray(csvFiles, 1);

    // open db connection
    const [client, collection] = await connectToCollection('sensordata');
    collection.createIndex({ sensor_id: 1, day: 1 }, { unique: true });
    dbClient = client;

    const batchedFunctions = fileChunks.map(function(chunk) {
      return function() {
        const processes = chunk.map(function(fileName) {
          return processFile(fileName, dateString, collection);
        });
        return Promise.all(processes);
      };
    });

    // Send off x requests at once, then wait until they're done before starting the next x requests
    await processSequentially(batchedFunctions);
  } catch (err) {
    console.error(err);
  }

  await dbClient.close();
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
  console.log(`--- Downloading latest measurements from https://archive.luftdaten.info (${startDate}) ---`);
  getEntireDay(startDate)
    .then(() => {
      console.log(`--- Added ${addedMeasurements} new measurements to the database ---`);
      console.log(`--- Total time: ${Math.floor((new Date().getTime() - before.getTime()) / 1000)} seconds---`);
    })
    .catch(console.error);
} else {
  downloadPlain('https://archive.luftdaten.info').then(function(text) {
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
          console.log(`--- Added ${addedMeasurements} new measurements to the database ---`);
          console.log(`--- Total time: ${Math.floor((new Date().getTime() - before.getTime()) / 1000)} seconds---`);

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
