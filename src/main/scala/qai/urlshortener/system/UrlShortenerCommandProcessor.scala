package qai.urlshortener.system

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import qai.urlshortener.data.UrlShortenerData._
import qai.urlshortener.db.UrlShortenerDBs
import qai.urlshortener.system.UrlShortenerSystemData.{UrlShortenerCommand, UrlShortenerEvent}

class UrlShortenerCommandProcessor(workersCount: Int)(implicit urlShortenerDBs: UrlShortenerDBs) extends Actor with ActorLogging {
  // Init Workers
  var workers: Map[Int, ActorRef] = (0 until workersCount).map(i => {
    i -> this.context.system.actorOf(Props(new UrlShortenerWorker(urlShortenerDBs)), s"UrlShortenerWorkser$i${(new Date()).getTime}")
  }).toMap

  def receive: Receive = {
    case e@UrlShortenerCommand(scmd, _) =>
      scmd match {
        case c@CreateNewShortUrl(oUrl, _) =>
          workers(Math.abs(oUrl.hashCode % workersCount)) ! e

        case SaveClick(id) =>
          workers(Math.abs(id.hashCode % workersCount)) ! e
      }
  }
}

private[this] class UrlShortenerWorker(urlShortenerDBs: UrlShortenerDBs) extends Actor with ActorLogging {
  def receive: Receive = {
    case UrlShortenerCommand(cmd, replyTo) =>
      cmd match {
        case CreateNewShortUrl(originalUrl, id) =>
          // Validate existance
          val stored = urlShortenerDBs.getUrlShortenerDB.getShortUrl(originalUrl)
          if(stored.isEmpty) {
            val suc = urlShortenerDBs.getUrlShortenerDB.storeUrl(originalUrl, id)
            replyTo ! UrlShortenerEvent(ShortUrlCreated(suc.id, suc.shortUrl, suc.originalUrl))
          } else {
            replyTo ! UrlShortenerEvent(ShortUrlCreated(stored.get.id, stored.get.shortUrl, stored.get.originalUrl)) // stored.get safe, validated before
          }

        case SaveClick(id) =>
          val count = urlShortenerDBs.getClicksDB.incrementClicks(id)
          replyTo ! UrlShortenerEvent(ClickSaved(id, count))
      }
  }
}