package qai.urlshortener

import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import qai.urlshortener.db.MockDbs
import qai.urlshortener.http.UrlShortenerServer
import qai.urlshortener.system.UrlShortenerSystemData.UrlShortenerSystem
import qai.urlshortener.system.{UrlShortenerCommandProcessor, UrlShortenerQueryProcessor}

object UrlShortenerApp extends App with UConfig with UrlShortenerAppDb with UrlShortenerAppSystem {
  UrlShortenerServer.start
}

trait UConfig {
  val config: Config = getConfig

  private def getConfig =
    ConfigFactory.parseString(s"akka.remote.netty.tcp.port=2551").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [urlshortener]")).
      withFallback(ConfigFactory.load("urlshortener"))
}

trait UrlShortenerAppDb {
  implicit val mockDbs: MockDbs.type = MockDbs
}

trait UrlShortenerAppSystem { self: UConfig with UrlShortenerAppDb =>
  protected val system: ActorSystem = ActorSystem("ClusterSystem", config)

  private val urlShortenerCmd: ActorRef = system.actorOf(Props(new UrlShortenerCommandProcessor(10)), name = "urlShortenerCmdActor")
  private val urlShortenerQuery: ActorRef = system.actorOf(Props(new UrlShortenerQueryProcessor(10)), name = "urlShortenerQueryActor")

  protected implicit val urlShortenerSystem: UrlShortenerSystem = UrlShortenerSystem(urlShortenerCmd, urlShortenerQuery)

  system.actorOf(Props[MetricsListener], name = "metricsListener")
}