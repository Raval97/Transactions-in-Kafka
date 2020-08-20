package AlpakkaKafka

import akka.kafka.ProducerMessage
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.producer.ProducerRecord

import scala.collection.immutable
import scala.io.AnsiColor._

object ProducerToTesting extends App {

  implicit val system = akka.actor.ActorSystem("system")

  val receipt = new Model.Receipt
  var i = 0

  val products = receipt.produkty()
  val producer = {
    Source(products)
      //      .throttle(1, 0.2.second)
      .map { product =>
        println(f"${WHITE}Send -> ID: ${product.id}%-7s| name: ${product.name}%-9s| amount: ${product.amount}%-3s| price: ${product.price}%-6s${RESET}")
        ProducerMessage.multi(
          immutable.Seq(
            new ProducerRecord[String, String]("inputTopic1", product.toString),
            new ProducerRecord[String, String]("inputTopic2", product.toString)
          )
        )
      }
      .via(Producer.flexiFlow(Properties.producerSettings))
  }
  .runWith(Sink.ignore)

}
