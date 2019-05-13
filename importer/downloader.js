// @ts-check

/**
 * @type {(url: string | Request, init?: RequestInit) => Promise<Response>}
 */
// @ts-ignore
const fetch = require('node-fetch');

/**
 * Returns the measurements of the last 5 minutes
 */
async function downloadLatest() {
  const response = await fetch('https://api.luftdaten.info/static/v1/filter/type=SDS011');
  return await response.json();
}

async function downloadPlain(url) {
  const response = await fetch(url);
  return await response.text();
}

/**
 * Downloads the HTML directory listing for the specified date from the archive
 * @param {string} dateString A date in the format of "YYYY-MM-DD"
 * @return {Promise<string>} HTML as a string
 * @throws Throws if page couldn't be downloaded
 */
async function downloadFromArchive(dateString) {
  const res = await fetch(`https://archive.luftdaten.info/${dateString}/`);

  if (res.status !== 200) {
    throw new Error(`Error while trying to get measurements from https://archive.luftdaten.info/${dateString}/\nStatus code: ${res.status}`);
  }

  return res.text();
}

module.exports = {
  downloadLatest: downloadLatest,
  downloadFromArchive: downloadFromArchive,
  downloadPlain: downloadPlain
};
