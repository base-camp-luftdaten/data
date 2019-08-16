# Data import

## Installation / Setup

You need [docker-compose](https://docs.docker.com/compose/install/) installed to run the MongoDB database. To start it, run `docker-compose up` in the project root directory. This should start the MongoDB docker container.

To import data into the database, you need [Node.js](https://nodejs.org/en/). First, run `npm install` in the project root. You can run the import script by using `node importer/24h.js --single-day`. This should feed yesterdays dataset into the database.

The `--single-day` flag indicates that only a single day should be imported.

It is also possible to specify the day that should be imported by passing the date in the following format: `node importer/24h.js 2019-08-15 --single-day`. The default value is yesterday.

When run without using `--single-day`, the importer will import the entire dataset (day-by-day), starting with the passed date (or yesterday, if no date was specified).

To inspect the MongoDB database using a GUI, I recommend [Robo 3T](https://robomongo.org/).

Finally, to compile and run the Java backend, run:

```sh
mvn clean package && java -jar target/AirDataBackendService.war
```

## REST Endpoints

### GET `/api/measurements/dust{query}`

Returns all _dust only_ measurements.

Request headers:

```
Accept: application/json
```

Query parameters:

- limit: `int`, max amount of measurements returned
  - _default_: `1000`
  - capped at `100 000`, which is about 25MB of JSON
- offset: `int`, ignore the first x values (works well together with limit)
  - _default_: `0`
- maxage: only return measurements up to this timestamp
  - _default_: -1 day
  - Format: `yyyy-MM-dd'T'HH:mm:ssZ` (make sure to url-encode)
  - Example: `2019-04-01T00:00:00+0000` --> `/api/measurements/dust?maxage=2019-04-01T00%3A00%3A00%2B0000`
- box: only include sensors in a 'box' with the given coordinates
  - Format: 4 comma separated values: `lat1,lon1,lat2,lon2`, `lat1/lon1` represent the bottom left point, `lat2/lon2` the top right point
  - Example: `/api/measurements/dust?box=48.7820,9.1920,51.0440,13.7460`
- country: only return measurements within the specified countries
  - Format: ISO 3166-1 Alpha-2 country codes, comma separated
  - Example: `/api/measurements/dust?country=DE`
  - Example: `/api/measurements/dust?country=DE,NL`

Result:

```json
[
  {
    "id": "23432-SDS011-2019-04-28T23:59:59",
    "sensorId": "23432",
    "sensorType": "SDS011",
    "lat": 45.411,
    "lon": 10.992,
    "timestamp": 1556495999000,
    "p10": 0.57,
    "p25": 0.28,
    "fromDataset": "https://archive.luftdaten.info/2019-04-28/2019-04-28_sds011_sensor_23432.csv"
  },
  {
    "id": "24435-SDS011-2019-04-28T23:59:59",
    "sensorId": "24435",
    "sensorType": "SDS011",
    "lat": 52.47,
    "lon": 13.436,
    "timestamp": 1556495999000,
    "p10": 7.05,
    "p25": 6.35,
    "fromDataset": "https://archive.luftdaten.info/2019-04-28/2019-04-28_sds011_sensor_24435.csv"
  }
]
```

## Deployment

1. ```sh
   rsync -avz importer/ username@server:/srv/data/luftdaten19/importer
   rsync -avz package.json username@server:/srv/data/luftdaten19/
   ```
2. Login on the server via `ssh`, change into the `/srv/data/luftdaten19` directory and run `npm install`
