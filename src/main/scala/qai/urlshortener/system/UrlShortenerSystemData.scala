package qai.urlshortener.system

import akka.actor.ActorRef
import qai.urlshortener.data.UrlShortenerData.{Command, Event, Query, Result}

object UrlShortenerSystemData {
  trait UrlShortenerSystemEvent

  case class UrlShortenerCommand(cmd: Command, replyTo: ActorRef) extends UrlShortenerSystemEvent
  case class UrlShortenerEvent(event: Event) extends UrlShortenerSystemEvent
  case class UrlShortenerQuery(cmd: Query, replyTo: ActorRef) extends UrlShortenerSystemEvent
  case class UrlShortenerResult(result: Result) extends UrlShortenerSystemEvent
  case class UrlShortenerRejection(reason: String) extends UrlShortenerSystemEvent
}
