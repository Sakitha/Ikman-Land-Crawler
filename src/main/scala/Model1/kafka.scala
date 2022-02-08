package Model1

import akka.actor.{Actor, ActorLogging}
import akka.kafka.scaladsl.SendProducer
import net.liftweb.json.{DefaultFormats, Serialization}
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import akka.pattern.pipe
import scala.concurrent.{Await, Future}

class kafka(producer:SendProducer[String,String]) extends Actor with ActorLogging{

  implicit val formats = DefaultFormats
  implicit val exec = context.dispatcher
  val topic = "Ikman"

  def receive: Receive = {
    case adds:Set[Add] =>
      val lstfut: Set[Future[RecordMetadata]] = adds
        .map(add => Serialization.write(add))
        .map(addJsonString => new ProducerRecord[String, String](topic, addJsonString))
        .map(msg => producer.send(msg))
      Future.sequence(lstfut).pipeTo(self)
    case _:Set[RecordMetadata] =>
      context.parent ! "Done"
  }

  producer.close()

}
