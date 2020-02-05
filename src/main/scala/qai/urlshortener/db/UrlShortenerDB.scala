package qai.urlshortener.db

import qai.urlshortener.data.UrlShortenerData.{ShortUrlCreated, StoredShortUrl}

trait UrlShortenerDB {
  def storeUrl(originalUrl: String, id: Long): ShortUrlCreated
  def getShortUrl(original: String): Option[StoredShortUrl]
  def getOriginalUrl(shortUrl: String): Option[StoredShortUrl]
  def getLastId(): Long
}

