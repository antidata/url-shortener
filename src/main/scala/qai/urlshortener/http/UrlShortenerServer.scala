package qai.urlshortener.http

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import net.liftweb.json.DefaultFormats
import net.liftweb.json._

import scala.concurrent.duration._
import net.liftweb.json._
import qai.urlshortener.data.UrlShortenerData._
import qai.urlshortener.system.UrlShortenerHelper
import qai.urlshortener.system.UrlShortenerSystemData._

import scala.collection.immutable.Seq
import scala.concurrent.{Future, Promise}
import scala.util.Try

object UrlShortenerServer {
  implicit val formats = DefaultFormats

  def start(urlShortenerCmd: ActorRef, urlShortenerQuery: ActorRef)= {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    // needed for the future map/flatmap in the end
    implicit val executionContext = system.dispatcher

    val requestHandler: HttpRequest => Future[HttpResponse] = {
      case HttpRequest(POST, Uri.Path("/createShortUrl"), _, entity, _) =>
        entity.toStrict(10.seconds)
          .map(_.data.utf8String)
          .flatMap(json => {
            extractNewUrl(json).map(cns => {
              val promise = Promise[UrlShortenerSystemEvent]()
              val worker = system.actorOf(Props(new RequestWorker(urlShortenerCmd, promise)))
              worker ! UrlShortenerCommand(CreateNewShortUrl(cns.originalUrl, 0L), worker)
              promise.future.map(result => HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,compactRender(Extraction.decompose(result)))))
            }).getOrElse(Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, """{"error":"invalid json"}"""))))
          })

      case HttpRequest(POST, Uri.Path("/getClicks"), _, entity, _) =>
        entity.toStrict(10.seconds)
          .map(_.data.utf8String)
          .flatMap(json => {
            extractShortUrlClicks(json).map(cns => {
              val promise = Promise[UrlShortenerSystemEvent]()
              val worker = system.actorOf(Props(new RequestWorker(urlShortenerQuery, promise)))
              worker ! UrlShortenerQuery(cns, worker)
              promise.future.map(result => HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,compactRender(Extraction.decompose(result)))))
            }).getOrElse(Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, """{"error":"invalid json"}"""))))
          })

      case r@HttpRequest(GET, uri, _, _, _) =>
        r.discardEntityBytes()
        val pathArr = uri.path.toString().split("/")
        if(pathArr.size > 1) {
          val promise = Promise[UrlShortenerSystemEvent]()
          val worker = system.actorOf(Props(new RequestWorker(urlShortenerQuery, promise)))
          worker ! UrlShortenerQuery(GetOriginalUrl(UrlShortenerHelper.baseUrl + pathArr(1)), worker)
          promise.future.map(result => {
            val queryPromise = Promise[UrlShortenerSystemEvent]()
            val clickWorker = system.actorOf(Props(new RequestWorker(urlShortenerCmd, queryPromise))) // Getting original URL means that someone clicked on it, so we save a new click
            val newShortUrl = result.asInstanceOf[UrlShortenerResult].result.asInstanceOf[StoredShortUrl]
            clickWorker! UrlShortenerCommand(SaveClick(newShortUrl.id), clickWorker)
            val locationHeader = getLocationHeader(newShortUrl.originalUrl)
            HttpResponse(StatusCodes.PermanentRedirect, Seq(locationHeader))
          })
        } else
          Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, s"""{"error":"Url invalid"}""")))

      case r: HttpRequest =>
        r.discardEntityBytes() // important to drain incoming HTTP Entity stream
        Future.successful(HttpResponse(404, entity = "Unknown resource!"))
    }

    Http().bindAndHandleAsync(requestHandler, "localhost", 8080)
    println(s"Server online at http://localhost:8080...")
  }

  def extractNewUrl(json: String): Option[GetShortUrl] = {
    JsonParser.parse(json).extractOpt[GetShortUrl]
  }

  def extractShortUrl(json: String): Option[GetOriginalUrl] = {
    JsonParser.parse(json).extractOpt[GetOriginalUrl]
  }

  def extractShortUrlClicks(json: String): Option[GetClicksFromShortUrl] = {
    JsonParser.parse(json).extractOpt[GetClicksFromShortUrl]
  }

  def getLocationHeader(originalUrl: String) = {
    HttpHeader.parse("location", originalUrl) match {
      case Ok(header, _) => header
      case _ => throw new Exception(s"Header invalid")
    }
  }

  class RequestWorker(askTo: ActorRef, promise: Promise[UrlShortenerSystemEvent]) extends Actor with ActorLogging {
    import scala.concurrent.ExecutionContext.Implicits.global

    context.system.scheduler.scheduleOnce(120.seconds, self, PoisonPill)

    def receive: Receive = {
      case e:UrlShortenerSystemEvent =>
        e match {
          case cmd@UrlShortenerCommand(_, _) =>
            askTo ! cmd

          case query@UrlShortenerQuery(_,_) =>
            askTo ! query

          case event@UrlShortenerEvent(_) =>
            promise.complete(Try(event))

          case result@UrlShortenerResult(_) =>
            promise.complete(Try(result))

          case UrlShortenerRejection(reason) =>
            promise.failure(new Throwable(reason))

          case other =>
            println(s"other $other")
        }
    }
  }

  case class GetShortUrl(originalUrl: String)
}