import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.{Actor, ActorContext, ActorRef, ActorSystem, Props}

object PongActor {
  val props = Props(new PongActor)
}

class PongActor extends Actor {
  override def receive: Receive = {
    case msg =>
      sender() ! msg
  }
}

object PingActor {
  def props(messageCount: Int, batchSize: Int) = Props(new PingActor(messageCount, batchSize))

  case class Msg(sender: ActorRef)
  case class Start(sender: ActorRef)
}

class PingActor(var messageCount: Int, batchSize: Int) extends Actor {
  import PingActor._

  var batch = 0
  var replyTo: Option[ActorRef] = None

  override def receive: Receive = {
    case s: Start =>
      SendBatch(context, s.sender)
      replyTo = Some(sender())
    case m: Msg =>
      batch = batch - 1

      if (batch == 0) {
        if (!SendBatch(context, m.sender)) {
          replyTo.map(x => x ! true)
        }
      }
  }

  def SendBatch(actorContext: ActorContext, actorRef: ActorRef): Boolean =  {
    if (messageCount == 0) {
      return false
    }

    var m = Msg(context.self)

    for (_ <- 1 to batchSize) {
      sender() ! m
    }

    messageCount = messageCount - batchSize
    batch = batchSize

    return true
  }
}

object Root extends App {
  case class Run(num: Int)

  val system = ActorSystem("main")
  val messageCount = 1000000
  val batchSize = 100
  val clientsCount = List(1, 2, 4, 8, 16)

  println(s"Clients		Elapsed		Msg/sec")

  clientsCount.foreach(clientCount => {
    val clients = (1 to clientCount).map(c => system.actorOf(PingActor.props(messageCount, batchSize))).toIndexedSeq
    val echos = (1 to clientCount).map(c => system.actorOf(PongActor.props)).toIndexedSeq
    implicit val timeout = Timeout(5 seconds)

    val start = System.nanoTime()
    val futures = (1 to clientCount).map(c => {
      val client = clients(c - 1)
      val echo = echos(c - 1)

      val future = client ? PingActor.Start(echo)
      future
    })

    Await.ready(Future.sequence(futures), Duration.Inf)

    val stop = System.nanoTime()

    val totalMessages = messageCount * 2 * clientCount
    var elasped = (stop - start) / 1000000
    val msgSec = totalMessages / ((stop - start) / 1000000)
    println(s"$clientCount\t\t$elasped\t\t$msgSec")
  });
}
