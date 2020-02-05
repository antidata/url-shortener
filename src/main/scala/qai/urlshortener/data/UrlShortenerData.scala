package qai.urlshortener.data

object UrlShortenerData {
  trait UrlShortenerMessage

  // Commands
  sealed trait Command extends UrlShortenerMessage
  case class CreateNewShortUrl(originalUrl: String, id: Long) extends Command
  case class SaveClick(id: Long) extends Command

  // Events
  sealed trait Event extends UrlShortenerMessage
  case class ShortUrlCreated(id: Long, shortUrl: String, originalUrl: String) extends Event
  case class ClickSaved(id: Long, count: Int) extends Event

  // Query
  sealed trait Query extends UrlShortenerMessage
  case class GetOriginalUrl(shortUrl: String) extends Query
  case class GetClicksFromShortUrl(shortUrl: String) extends Query
  case class GetClicksFromId(id: Long) extends Query

  // Results
  sealed trait Result extends UrlShortenerMessage
  case class NewShortUrlResult(shortUrl: String, originalUrl: String) extends Result
  case class ClicksCountResult(shortUrl: String, count: Int) extends Result
  case class StoredShortUrl(shortUrl: String, originalUrl: String, id: Long) extends Result
}
