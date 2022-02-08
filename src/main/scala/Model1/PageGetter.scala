package Model1

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import org.jsoup.Jsoup

import scala.collection.JavaConverters._
import scala.concurrent.duration.DurationInt

object PageGetter {
  object Timeout extends BadStatus(0)
  def props(url:String,pageNumber:Int) = Props(new PageGetter(url, pageNumber))
}
class PageGetter(url:String, pageNumber:Int) extends Actor with ActorLogging{
  import PageGetter._
  implicit val exec = context.dispatcher

  WebClient.get(url + pageNumber).pipeTo(self)
  context.system.scheduler.scheduleOnce(60.seconds, self, Timeout)

  def find(body: String,url:String): Iterator[String] = {
    val document = Jsoup.parse(body, url)
    val links = document.select("a[href]")
    for {
      link <- links.iterator().asScala
    } yield link.absUrl("href")
  }
  def receive={
    case body:String =>
      val addUrls = find(body,url).filter(url => url.contains("https://ikman.lk/en/ad/")).toList
      context.parent ! (addUrls, pageNumber)
      stop()
    case _: BadStatus =>
      context.parent ! (BadStatus, pageNumber)
      stop()
  }
  def stop() = {
    context.stop(self)
  }
}
