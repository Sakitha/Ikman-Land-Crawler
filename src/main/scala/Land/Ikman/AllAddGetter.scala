package Land.Ikman

import akka.actor.{Actor, ActorLogging, Props}

import scala.concurrent.ExecutionContextExecutor

object AllAddGetter{
  case object Done
  def props(urlsPerPage:List[String])(implicit exec:ExecutionContextExecutor) : Props =
    Props(new AllAddGetter(urlsPerPage:List[String]))
}
class AllAddGetter (urls:List[String])(implicit exec:ExecutionContextExecutor) extends Actor with ActorLogging{

  var phasedCount = 0
  var adds:Set[Add] = Set()
  var success:Set[String]=Set()
  var bad:Set[String]=Set()
  var timeOut:Set[String]=Set()

  urls.zipWithIndex.foreach{case (url,i) => context.actorOf(AddGetter.props(url),i.toString)}
  urls.foreach(println(_))
  def receive = {
    case (add:Add,name:String, url:String) =>
      phasedCount += 1
      success += url
      adds += add
      maybeContactParent()

    case (BadStatus(x),name:String, url:String) =>
      phasedCount += 1
      bad += url
      maybeContactParent()
      log.warning(s"$name returned $x, $phasedCount,$urls.size")

    case (t:Timeout,name:String, url:String)  =>
      phasedCount += 1
      timeOut += url
      maybeContactParent()
      log.warning(s"$name returned $t, $phasedCount,$urls.size")
  }
  def maybeContactParent() = {
    if(phasedCount == urls.size) {
      context.parent ! (adds,success,bad,timeOut)
      context.stop(self)
    }



  }
}