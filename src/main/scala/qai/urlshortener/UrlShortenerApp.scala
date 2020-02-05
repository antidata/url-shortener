package qai.urlshortener

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import qai.urlshortener.db.MockDbs
import qai.urlshortener.http.UrlShortenerServer
import qai.urlshortener.system.{UrlShortenerCommandProcessor, UrlShortenerQueryProcessor}

object UrlShortenerApp extends App {
  // Override the configuration of the port when specified as program argument
  val port = if (args.isEmpty) "2551" else args(0)

  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
    withFallback(ConfigFactory.parseString("akka.cluster.roles = [urlshortener]")).
    withFallback(ConfigFactory.load("urlshortener"))

  val system = ActorSystem("ClusterSystem", config)

  val urlShortenerCmd = system.actorOf(Props(new UrlShortenerCommandProcessor(10, MockDbs)), name = "urlShortenerCmdActor")
  val urlShortenerQuery = system.actorOf(Props(new UrlShortenerQueryProcessor(10, MockDbs)), name = "urlShortenerQueryActor")

//  system.actorOf(Props[MetricsListener], name = "metricsListener")

  UrlShortenerServer.start(urlShortenerCmd, urlShortenerQuery)
}
