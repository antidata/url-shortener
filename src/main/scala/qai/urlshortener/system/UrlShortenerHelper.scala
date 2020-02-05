package qai.urlshortener.system

import scala.annotation.tailrec

trait UrlShortenerHelper {
  val baseUrl = "http://localhost:8080/" // Load from config
  def getShortUrl(originalUrl: String, id: Long): String = {
    s"$baseUrl${encode(id)}"
  }

  val ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
  val BASE: Int = ALPHABET.length

  def encode(n: Long): String = {
    if(n == 0L) ALPHABET(0).toString
    else {
      tailEncode(n, "")
    }
  }

  @tailrec
  private def tailEncode(n: Long, res: String): String = {
    if(n > 0) {
      val rm = n % BASE
      tailEncode(n / BASE, ALPHABET(rm.toInt).toString + res)
    } else {
      res
    }
  }

  def decode(str: String): Long = {
    tailDecode(str.replace(baseUrl,"").reverse.toCharArray.toList, 0L, 0)
  }

  @tailrec
  private def tailDecode(chars: List[Char], n: Long, pos: Int): Long = {
    val toBase10 = (m: Int, pow: Int) => {
      m * Math.pow(BASE, pow).toLong
    }
    chars match {
      case h :: t =>
        tailDecode(t, n + toBase10(ALPHABET.indexOf(h), pos), pos + 1)

      case Nil => n
    }
  }

}

object UrlShortenerHelper extends UrlShortenerHelper

