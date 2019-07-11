const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });
const fetch = require('node-fetch');

const { WEBHOOK_URL } = process.env;

const logger = {
  log(...args) {
    if (WEBHOOK_URL === undefined) return console.log(args);

    fetch(WEBHOOK_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        text: args.join('\n')
      })
    }).catch(console.error);
  }
};

module.exports = logger;
