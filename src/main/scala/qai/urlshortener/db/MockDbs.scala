package qai.urlshortener.db

import qai.urlshortener.data.UrlShortenerData.{ShortUrlCreated, StoredShortUrl}
import qai.urlshortener.system.UrlShortenerHelper

object MockDbs extends UrlShortenerDBs {
  def getUrlShortenerDB: UrlShortenerDB = MockUrlShortenerDB
  def getClicksDB: ClicksDB = MockClicksDB
}

object MockUrlShortenerDB extends UrlShortenerDB {
  private case class ShortUrl(id: Long, originalUrl: String, shortUrl: String)
  private var urls: List[ShortUrl] = List()

  def storeUrl(originalUrl: String, id: Long): ShortUrlCreated = {
    urls.find(_.originalUrl == originalUrl) match {
      case Some(storedUrl) =>
        ShortUrlCreated(storedUrl.id, storedUrl.shortUrl, storedUrl.originalUrl)

      case _ =>
        val nextId = getNextId()
        val shortUrl = UrlShortenerHelper.getShortUrl(originalUrl, nextId)
        val newUrl = ShortUrl(nextId, originalUrl, shortUrl)
        urls = newUrl :: urls
        ShortUrlCreated(nextId, shortUrl, originalUrl)
    }
  }

  def getShortUrl(original: String): Option[StoredShortUrl] = {
    urls.find(_.originalUrl == original).map(su => StoredShortUrl(su.shortUrl, su.originalUrl, su.id))
  }

  def getOriginalUrl(shortUrl: String): Option[StoredShortUrl] = {
    urls.find(_.shortUrl == shortUrl).map(su => StoredShortUrl(su.shortUrl, su.originalUrl, su.id))
  }

  def getLastId(): Long = if(urls.nonEmpty) urls.maxBy(_.id).id else 0L

  private var lastId = getLastId()

  def getNextId(): Long = {
    lastId = lastId + 1
    lastId
  }
}

object MockClicksDB extends ClicksDB {
  private var clicks: Map[Long, Int] = Map()

  def incrementClicks(id: Long): Int = {
    clicks.find(_._1 == id) match {
      case Some((_, count)) =>
        clicks = clicks.updated(id, count+1)
        count + 1

      case _ =>
        clicks = clicks.updated(id, 1)
        1
    }
  }

  def getClicks(id: Long): Int = {
    clicks.find(_._1 == id) match {
      case Some((_, count)) =>
        count

      case _ =>
        0
    }
  }
}
