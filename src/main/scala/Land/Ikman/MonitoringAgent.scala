package Land.Ikman

import akka.actor.{Actor, ActorLogging, ActorRef, Props}


object MonitoringAgent{
  object StartNext
  def props(startingUrl:String,persistentActor:ActorRef) :Props = Props(new MonitoringAgent(startingUrl,persistentActor))
}
class MonitoringAgent(startingUrl:String,persistentActor:ActorRef) extends Actor with ActorLogging{
  import MonitoringAgent._

  def receive = crawlingPage(1)
  def crawlingPage(pageNumber:Int): Receive ={
    case StartNext =>
      log.info("MonitoringAgent -> StartNext")
      context.actorOf(PageGetter.props(startingUrl, pageNumber),s"PageGetter")
      context.become(Waiting(pageNumber))
  }
  def Waiting(pageNumber:Int): Receive ={
    case (addUrls:List[String], pageNumberPhrased:Int) =>
      log.info(s"MonitoringAgent -> $addUrls")
      persistentActor ! (addUrls, pageNumberPhrased)
      context.become(crawlingPage(pageNumber +1))
    case (_:BadStatus, pageNumberPhrased:Int)=>
      log.info(s"MonitoringAgent -> BadStatus")
      persistentActor ! (Nil, pageNumberPhrased)
      context.become(crawlingPage(pageNumber +1))
  }

}