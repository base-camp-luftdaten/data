// @ts-check

/**
 * @type {(url: string | Request, init?: RequestInit) => Promise<Response>}
 */
// @ts-ignore
const { downloadFromArchive, downloadPlain } = require('./downloader');
const { getCSVFiles } = require('./getCSVFiles');
const { chunkArray } = require('./utils');
const { cleanMeasurements } = require('./cleanMeasurements');
const { connectToCollection } = require('./database');
const { zeroPad } = require('./utils');
const { parseCSV } = require('./utils');

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

      dbCollection
        .insertMany(records, { ordered: false })
        .then(res => {
          addedMeasurements += res.insertedCount;
          // console.log(`Written to DB: ${res.insertedCount}`);
          resolve();
        })
        .catch(err => {
          if (err.code !== 11000) {
            console.log(`Got "${err.message}" when trying to write ${url} into database`);
          }
        });
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
    const [client, collection] = await connectToCollection();
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
const today = new Date();
const yesterday = new Date(new Date().setDate(today.getDate() - 1));
const yesterdayFormatted = `${yesterday.getFullYear()}-${zeroPad(yesterday.getMonth() + 1)}-${zeroPad(yesterday.getDate())}`;

addedMeasurements = 0;
console.log(`--- Downloading latest measurements from https://archive.luftdaten.info (${yesterdayFormatted}) ---`);
getEntireDay(yesterdayFormatted)
  .then(() => {
    console.log(`--- Added ${addedMeasurements} new measurements to the database ---`);
    console.log(`--- Total time: ${Math.floor((new Date().getTime() - today.getTime()) / 1000)} seconds---`);
  })
  .catch(console.error);
