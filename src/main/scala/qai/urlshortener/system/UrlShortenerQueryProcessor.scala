package qai.urlshortener.system

import akka.actor.{Actor, ActorLogging}
import qai.urlshortener.data.UrlShortenerData._
import qai.urlshortener.db.UrlShortenerDBs
import qai.urlshortener.system.UrlShortenerSystemData.{UrlShortenerQuery, UrlShortenerRejection, UrlShortenerResult}

class UrlShortenerQueryProcessor(workers: Int)(implicit urlShortenerDBs: UrlShortenerDBs) extends Actor with ActorLogging {

  def receive: Receive = {
    case UrlShortenerQuery(query, replyTo) =>
      query match {
        case GetOriginalUrl(shortUrl) =>
          urlShortenerDBs.getUrlShortenerDB.getOriginalUrl(shortUrl)
            .map(su => replyTo ! UrlShortenerResult(su))
            .getOrElse(replyTo ! UrlShortenerRejection(s"$shortUrl does not exists"))

        case GetClicksFromShortUrl(shortUrl) =>
          urlShortenerDBs.getUrlShortenerDB.getOriginalUrl(shortUrl)
            .map(su => {
              val clicks = urlShortenerDBs.getClicksDB.getClicks(su.id)
              replyTo ! UrlShortenerResult(ClicksCountResult(shortUrl, clicks))
            }).getOrElse(replyTo ! UrlShortenerRejection(s"$shortUrl does not exists"))
      }
  }
}
