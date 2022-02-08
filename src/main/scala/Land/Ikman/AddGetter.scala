package Land.Ikman

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import org.jsoup.Jsoup

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

case class Add(Member:Boolean,
               PostedOn: String,
               SubLocation: String,
               Location:String,
               Address:Option[String],
               LandType:List[String],
               LandSize:(Option[Int],Boolean),
               Amount :(Option[Int],Boolean),
              )
case class Timeout()
object AddGetter {
  def props(url:String)(implicit exec: ExecutionContextExecutor) = Props(new AddGetter(url))
}

class AddGetter(url:String)(implicit exec: ExecutionContextExecutor) extends Actor with ActorLogging{

  WebClient.get(url).pipeTo(self)
  import AddGetter._
  context.system.scheduler.scheduleOnce(60.seconds, self, Timeout())

  def phaseAdd(body: String,url:String): Add = {
    val document = Jsoup.parse(body, url)
    val part1 = Map("Member" -> document.select("div.member-type--3aRF2").isEmpty)
    val part2 = List("PostedOn","SubLocation","Location") zip
      document.select(".sub-title--37mkY").text().split(",") toMap  //Posted on 13 Jan 1:34 am, Colombo 2, Colombo
    val part3 = List("Address","LandType","LandSize") zip
      document.select(".ad-meta--17Bqm > div > div.value--1lKHt").iterator().asScala.map(el => el.text()) toMap  //.matches("^[a-zA-Z0-9]*$")//(Address, Land type, Land size)
    val part4 = Map("Amount"->document.select(".amount--3NTpl").text()) //Rs 2,750,000 per perch

    Add(
      part1("Member"), part2("PostedOn"),part2("SubLocation"),part2("Location"),
      if(part3("Address").matches("^[a-zA-Z0-9]*$")) Some(part3("Address")) else None,
      part3("LandType").split(",").toList,
      (part3.get("LandSize").flatMap("([0-9]+)".r.findFirstIn(_)).map(_.toInt),part3.get("LandSize").contains("perch")),
      (part4.get("Amount").flatMap("([0-9]+)".r.findFirstIn(_)).map(_.toInt),part4.get("Amount").contains("perch"))
    )

  }

  def receive={
    case body:String =>
      val add:Add = phaseAdd(body,url)
      log.info(s"$add")
      context.parent ! (add,self.path.name,url)
      stop()
    case _: BadStatus =>
      context.parent ! (_ ,self.path.name,url)
      stop()
    case _:Timeout =>
      context.parent ! (_ ,self.path.name,url)
      stop()
  }
  def stop() = {
    context.stop(self)
  }
}
