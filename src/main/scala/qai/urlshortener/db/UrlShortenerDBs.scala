package qai.urlshortener.db

trait UrlShortenerDBs {
  def getUrlShortenerDB: UrlShortenerDB
  def getClicksDB: ClicksDB
}