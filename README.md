# URL Shortener

## Building the App

`> sh urlShortenerBuild`

## Running the App

`> sh urlshortener.sh`

## Creating new short URLs

The endpoint to create new short URLs is: `/createShortUrl`

To create a new URL, a POST request will be required with the following JSON body

```json
{
  "originalUrl": "https://averylongurl.com/thisistheverylongidofthepost",
}
```

### An Example using `curl`
```sh
curl --location --request POST 'http://127.0.0.1:8080/createShortUrl' \
--header 'Content-Type: application/json' \
--data-raw '{"originalUrl": "https://averylongurl.com/thisistheverylongidofthepost"}'
```

Response Example:

```json
{
    "event": {
        "id": 2,
        "shortUrl": "http://localhost:8080/c",
        "originalUrl": "https://averylongurl.com/thisistheverylongidofthepost"
    }
}
```

## Accessing short URLs

To access `http://localhost:8080/c` with a GET request will redirect you to `https://averylongurl.com/thisistheverylongidofthepost` (notice that this could redirect you to a not existing URL and give you error 404)

## Reviewing Amount of Clicks for a specific Short Link

The endpoint to get the click count is: `/getClicks`

To request the amount of clicks, a POST request will be required with the following JSON body

```json
{
  "shortUrl": "http://localhost:8080/c",
}
``` 

### An Example using `curl`
```sh
curl --location --request POST 'http://127.0.0.1:8080/getClicks' \
--header 'Content-Type: application/json' \
--data-raw '{"shortUrl": "http://localhost:8080/c" }'
```

Response Example:

```json
{
    "result": {
        "shortUrl": "http://localhost:8080/c",
        "count": 2
    }
}
```

