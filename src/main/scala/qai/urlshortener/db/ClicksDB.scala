package qai.urlshortener.db

trait ClicksDB {
  def incrementClicks(id: Long): Int
  def getClicks(id: Long): Int
}
