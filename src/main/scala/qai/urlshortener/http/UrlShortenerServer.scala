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
import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.util.Try

object UrlShortenerServer extends JsonExtractor with HttpHelpers {

  implicit lazy val system: ActorSystem = ActorSystem()
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()

  // needed for the future map/flatmap in the end
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def start(implicit urlShortenerSystem: UrlShortenerSystem): Unit = {
    val requestHandler: HttpRequest => Future[HttpResponse] = {
      case HttpRequest(POST, Uri.Path("/createShortUrl"), _, entity, _) =>
        entity.toStrict(10.seconds)
          .map(_.data.utf8String)
          .flatMap(json => {
            extractNewUrl(json).map(processCreateNewShortUrl)
              .getOrElse(Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, """{"error":"invalid json"}"""))))
          })

      case HttpRequest(POST, Uri.Path("/getClicks"), _, entity, _) =>
        entity.toStrict(10.seconds)
          .map(_.data.utf8String)
          .flatMap(json => {
            extractShortUrlClicks(json).map(processGetClicks)
              .getOrElse(Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, """{"error":"invalid json"}"""))))
          })

      case r@HttpRequest(GET, uri, _, _, _) =>
        r.discardEntityBytes()
        val pathArr = uri.path.toString().split("/")
        if(pathArr.size > 1) {
          processShortUrl(pathArr(1))
        } else
          Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, s"""{"error":"Url invalid"}""")))

      case r: HttpRequest =>
        r.discardEntityBytes() // important to drain incoming HTTP Entity stream
        Future.successful(HttpResponse(404, entity = "Unknown resource!"))
    }

    Http().bindAndHandleAsync(requestHandler, "localhost", 8080)
    println(s"Server online at http://localhost:8080...")
  }

  private def genericProcess[SystemType](msgFor: ActorRef => SystemType, actor: ActorRef): Future[UrlShortenerSystemEvent] = {
    val promise = Promise[UrlShortenerSystemEvent]()
    val worker = system.actorOf(Props(new RequestWorker(actor, promise)))
    worker ! msgFor(worker)
    promise.future
  }

  private def processGetClicks(getClicksFromShortUrl: GetClicksFromShortUrl)(implicit urlShortenerSystem: UrlShortenerSystem) = {
    genericProcess(worker => UrlShortenerQuery(getClicksFromShortUrl, worker), urlShortenerSystem.query)
      .map(result => HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,compactRender(Extraction.decompose(result)))))
  }

  private def processCreateNewShortUrl(cns: GetShortUrl)(implicit urlShortenerSystem: UrlShortenerSystem) = {
    genericProcess(worker => UrlShortenerCommand(CreateNewShortUrl(cns.originalUrl, 0L), worker), urlShortenerSystem.command)
      .map(result => HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,compactRender(Extraction.decompose(result)))))
  }

  private def processShortUrl(shortUrl: String)(implicit urlShortenerSystem: UrlShortenerSystem) = {
    genericProcess(worker => UrlShortenerQuery(GetOriginalUrl(UrlShortenerHelper.baseUrl + shortUrl), worker), urlShortenerSystem.query)
      .map(processRedirection)
  }

  private def processRedirection(implicit urlShortenerSystem: UrlShortenerSystem): PartialFunction[UrlShortenerSystemEvent, HttpResponse] = {
    case result: UrlShortenerResult =>
      result.result match {
        case newShortUrl: StoredShortUrl =>
          genericProcess(worker => worker ! UrlShortenerCommand(SaveClick(newShortUrl.id), worker), urlShortenerSystem.command) // Save new click
          val locationHeader = getLocationHeader(newShortUrl.originalUrl).getOrElse(getLocationHeader("/404").get)
          HttpResponse(StatusCodes.PermanentRedirect, Seq(locationHeader))

        case _ => // Log invalid case
          HttpResponse(404, entity = "Unknown resource!")
      }
  }
}

private[this] class RequestWorker(askTo: ActorRef, promise: Promise[UrlShortenerSystemEvent]) extends Actor with ActorLogging {
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

trait JsonExtractor {
  implicit val formats: DefaultFormats = DefaultFormats

  def extractNewUrl(json: String): Option[GetShortUrl] = {
    JsonParser.parse(json).extractOpt[GetShortUrl]
  }

  def extractShortUrl(json: String): Option[GetOriginalUrl] = {
    JsonParser.parse(json).extractOpt[GetOriginalUrl]
  }

  def extractShortUrlClicks(json: String): Option[GetClicksFromShortUrl] = {
    JsonParser.parse(json).extractOpt[GetClicksFromShortUrl]
  }
}

trait HttpHelpers {
  def getLocationHeader(originalUrl: String): Option[HttpHeader] = {
    HttpHeader.parse("location", originalUrl) match {
      case Ok(header, _) => Some(header)
      case _ => None
    }
  }
}