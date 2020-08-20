package AlpakkaKafka

import java.util.concurrent.atomic.AtomicReference

import AlpakkaKafka.Model.ThreadInterrupt
import akka.kafka.scaladsl.Consumer.Control
import akka.kafka.scaladsl.{Consumer, Producer, Transactional}
import akka.kafka.{ProducerMessage, Subscriptions}
import akka.stream.scaladsl.{Merge, RestartSource, Sink, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, Materializer, Supervision}
import org.apache.kafka.clients.producer.ProducerRecord

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.AnsiColor._

object Transaction extends App {

  implicit val system = akka.actor.ActorSystem("system")
  implicit val materializer: Materializer = ActorMaterializer()
  val innerControl = new AtomicReference[Control](Consumer.NoopControl)

  val thread = new ThreadInterrupt()
  new Thread(thread).start()
  val receipt = new Model.Receipt

  //#############################################################################
  //Source (producent) przsyłający produkty na topic: inputTopic1
  //############################################################################

  var products = receipt.produkty()
  val producer = {
    Source(products)
//      .throttle(1, 0.2.second)
      .map { product =>
        println(f"${WHITE}Send -> ID: ${product.id}%-7s| name: ${product.name}%-9s| amount: ${product.amount}%-3s| price: ${product.price}%-6s${RESET}")
        if (thread.flag) {
          println(s"${RED}Error was thrown. Every change within from last commit will be aborted.${RESET}")
          throw new Throwable()
        }
          ProducerMessage.multi(
          immutable.Seq(
            new ProducerRecord[String, String]("inputTopic1", product.toString),
            new ProducerRecord[String, String]("inputTopic2", product.toString)
          )
        )
      }
      .via(Producer.flexiFlow(Properties.producerSettings))
  }

  //#############################################################################
  // Trnsakcja przesyłąjąca wiadomości z sourceTopic1 do outputTopic
  //#############################################################################

  val readWriteProcess = {
    println("process START")
    Transactional
      .source(Properties.consumerSettings, Subscriptions.topics("inputTopic1"))
      .map { msg =>
//        if (msg.partitionOffset.offset == 5L) {
//          println(s"${RED}Error was thrown. Every change within from last commit will be aborted.${RESET}")
//          throw new Throwable()
//        }
        val product = msg.record.value().split(",")
        println(f"${YELLOW}ReSend <->: ${product(1)}%-9s| price: ${product(3)}%-6s| " +
          f"amount: ${product(2)}%-3s| productId: ${product(0)}|\t offset: ${msg.partitionOffset.offset}${RESET}")
        ProducerMessage.single(new ProducerRecord[String, String]("outputTopic", msg.record.key, msg.record.value + ", NO_zombie"), msg.partitionOffset)
      }
      .mapMaterializedValue(c => innerControl.set(c))
      .via(Transactional.flow(Properties.producerSettings, "transactionalId1"))
  }

  val zombie = {
    println("process Zombie START")
    Transactional
      .source(Properties.consumerSettings, Subscriptions.topics("inputTopic2"))
      .map { msg =>
        val product = msg.record.value().split(",")
        println(f"${MAGENTA}Zombie ReSend <->: ${product(1)}%-9s| price: ${product(3)}%-6s| " +
          f"amount: ${product(2)}%-3s| productId: ${product(0)}|\t offset: ${msg.partitionOffset.offset}${RESET}")
        ProducerMessage.single(new ProducerRecord[String, String]("outputTopic", msg.record.value + ", zombie"), msg.partitionOffset)
      }
      .mapMaterializedValue(c => innerControl.set(c))
      .via(Transactional.flow(Properties.producerSettings, "transactionalId1"))
  }

  //#############################################################################
  // Transakcja nasłuchujca outputTopic, licząca kwotę produktu i wysyłająca na topic: ProductTopic
  // oraz cene paragonu wysyłając na topic ProductPrice
  //#############################################################################
  var finalPrice: Double = 0
  var start = 0L
  val consumer = {
    Transactional
      .source(Properties.consumerSettings, Subscriptions.topics("outputTopic"))
      .map((msg) => {
//        if (msg.partitionOffset.offset == 5L) {
//          println(s"${RED}Error was thrown. Every change within from last commit will be aborted.${RESET}")
//          throw new ConnectException()
//        }
        val product = msg.record.value().split(",")
        val price = BigDecimal( product(2).toDouble * product(3).toDouble).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
        finalPrice += price
        println(f"${CYAN}Receive <-:${product(1)}%-9s| total price: $price%-5s| productId: ${product(0)} |\t offset: ${msg.partitionOffset.offset}${RESET}")
        if (product(0).trim.toInt == receipt.countOfProducts) {
          println(s"\n${RED}FINAL PRICE: ${BigDecimal(finalPrice).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble}${RESET}")
          val finalPriceTemp = BigDecimal(finalPrice).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
          Source.single(price)
            .map(x => new ProducerRecord[String, String]("FinalPrice",
              f"Final Price = $finalPriceTemp"))
            .runWith(Producer.plainSink(Properties.producerSettings))
          finalPrice=0
          val finish = System.currentTimeMillis()
          val time = finish - start
          println("Time =  "+time)
        }
        ProducerMessage.single(new ProducerRecord[String, String]("ProductPrice",
          f"Receive:${product(1)}%-9s| total price: $price%-5s| productId: ${product(0)}| ${product(4)}"), msg.partitionOffset)
      })
      .mapMaterializedValue(c => innerControl.set(c))
      .via(Transactional.flow(Properties.producerSettings, "transactionalId2"))
  }

  //#############################################################################
  // Wywołanie source-ów jednocześnie dzieki combine() & Merge(_) wraz z restartem w razie błędu
  //#############################################################################

  val decider: Supervision.Decider = {
    case e: Exception => {
      println("Exception handled, recovering stream: " + e.getMessage)
      Supervision.Stop
    }
    case _ => {
      Supervision.Stop
    }
  }

  val totalSource = Source.combine(consumer, readWriteProcess, producer)(Merge(_))

  RestartSource.onFailuresWithBackoff(
    minBackoff = 1.seconds,
    maxBackoff = 5.seconds,
    randomFactor = 0.2
  ) { () =>
    finalPrice=0
    start = System.currentTimeMillis()

    totalSource
      .withAttributes(ActorAttributes.supervisionStrategy(decider))
  }
    .runWith(Sink.ignore)

  // Uruchomienie procesu Zombie
//  zombie.runWith(Sink.ignore)

  sys.ShutdownHookThread {
    Await.result(innerControl.get.shutdown(), 10.seconds)
  }
}


