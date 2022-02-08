package Model1

import Model1.MonitoringAgent._
import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import akka.persistence.{PersistentActor, SnapshotOffer}
import org.apache.kafka.common.serialization.StringSerializer

import java.util.Date
import scala.collection.immutable.HashMap

object IkmanPersistentActor{
  def props(producer:SendProducer[String,String]) = Props(new IkmanPersistentActor(producer))
}

class IkmanPersistentActor(producer:SendProducer[String,String]) extends PersistentActor with ActorLogging{
  override def persistenceId = "id-1"
  implicit val exec = context.dispatcher
  var allAddUrls:HashMap[String,Option[Date]] = HashMap()
  var cache : Set[String] = Set()

  val monitoringAgent = context.actorOf(MonitoringAgent.props("https://ikman.lk/en/ads/sri-lanka/land?sort=date&order=desc&buy_now=0&urgent=0&page=", self),"MonitoringAgent")
  monitoringAgent ! StartNext
  val receiveRecover: Receive ={
    case SnapshotOffer(_, snapshot:HashMap[String,Option[Date]]) =>
      allAddUrls = snapshot
  }

  val receiveCommand: Receive = {
    case (addUrls: List[String], pageNumberPhrased:Int) =>
      updateWithNew(addUrls)
      context.actorOf(AllAddGetter.props(cache.toList),s"AllAddGetter$pageNumberPhrased")


    case (adds:Set[Add],success:Set[String],bad:Set[String],timeout:Set[String] ) =>
      kafka ! adds
      updateWithPast(success,bad,timeout)
      persist(allAddUrls){event=>
        saveSnapshot(event)
      }
      monitoringAgent ! StartNext
    case Nil =>
      log.info(s"IkmanPersistentActor -> Nil")
      context.stop(self)
  }
  def updateWithNew(addUrls: List[String]) ={
    addUrls.foreach(url =>
      allAddUrls.get(url) match {
        case None => allAddUrls += (url -> None)
          cache += url
      }
    )
  }
  def updateWithPast(success:Set[String],bad:Set[String],timeout:Set[String])  = {
    success.foreach(url =>
      allAddUrls.get(url) match {
        case Some(None) => allAddUrls += (url -> Some(new Date()))
      }
    )
  }

}

object PersistentActors extends App {
  implicit val system = ActorSystem("PersistentActors")
  val bootstrapServers = "localhost:9092"
  val config = system.settings.config.getConfig("akka.kafka.producer")
  val producerSettings =
    ProducerSettings(config, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers)
  val producer = SendProducer(producerSettings)
  system.actorOf(IkmanPersistentActor.props(producer),"IkmanPersistentActor")
}


