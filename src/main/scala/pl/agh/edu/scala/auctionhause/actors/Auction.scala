package pl.agh.edu.scala.auctionhause.actors

import java.util.Random

import akka.actor._
import akka.event.{Logging, LoggingReceive}
import akka.persistence.{Recover, RecoveryCompleted, PersistentActor}

import scala.concurrent.duration._

/**
 * Created by bj on 25.11.14.
 */
class Auction(bidTime: FiniteDuration, deleteTime: FiniteDuration, system: ActorSystem, auctionSearchName: String, masterSearch: ActorRef, name: String, sellerId: Int) extends PersistentActor with ActorLogging {
  import system.dispatcher

  val auctionSearchPath: String = "../../" + auctionSearchName

  val rand = new Random(System.currentTimeMillis() + 12565355)

  var buyers: List[ActorRef] = List()

  var topBuyer: ActorRef = null

  var topPrice: Long = 0

  var bidTimeStarted: Long = 0
  var deleteTimeStarted: Long = 0

  override def persistenceId = name + sellerId

  def updateState(event: AuctionStateChangeEvent): Unit = {
    println("updatingState" )
    context.become(
      event.state match {
        case AuctionCreated(newBidTimeStarted) => {
          bidTimeStarted = newBidTimeStarted
          println(bidTimeStarted)
          val search: ActorSelection = context.actorSelection(auctionSearchPath)
          search ! Register()
          auctionCreated
        }
        case AuctionActivated(buyer, price) => {
          if (price > topPrice){
            (buyers diff List(buyer)).foreach(_ ! NewTopBuyer(price, buyer))
            buyers = buyer :: (buyers diff List(buyer))
            topBuyer = buyer
            topPrice = price
            log.info("topBuyer: {}, topPrice: {}", topBuyer, topPrice)
          } else {
            buyer ! CurrentOfferIsHigher(topPrice)
          }
          auctionActivated
        }
        case AuctionIgnored(localBidTimeStarted) => {
          deleteTimeStarted = localBidTimeStarted
          auctionIgnored
        }
        case AuctionSold(localDeleteStarted) => {
          deleteTimeStarted = localDeleteStarted
          auctionSold
        }
        case AcutionNotInitialized => auctionNotInitialized
      }
    )
  }

  def auctionNotInitialized: Actor.Receive = LoggingReceive {
    case Start() =>
      val localBidTimeStarted = System.currentTimeMillis()
      persist(AuctionStateChangeEvent(AuctionCreated(localBidTimeStarted))) {
        event =>
          updateState(event)
          log.info("Auction: {} is being started, bidTime: {}, deleteTime: {}", getName, bidTime, deleteTime)
          system.scheduler.scheduleOnce(bidTime, self, BidTimeExpired())
      }
  }

  def auctionCreated = LoggingReceive {
    case BidTimeExpired() =>
      val localDeleteTimeStarted = System.currentTimeMillis()
      persist(AuctionStateChangeEvent(AuctionIgnored(localDeleteTimeStarted))){
        event =>
          log.info("Auction: {} reached BidTime: {}", getName, bidTime)
          context.parent ! BeingIgnored
          updateState(event)
      }
    case Bid(price) =>
      persist(AuctionStateChangeEvent(AuctionActivated(sender, price))){
        event =>
          log.info("Auction: First bid {} from {}", price, sender.path.name)
          updateState(event)
      }
  }

  def auctionActivated = LoggingReceive {
    case Bid(price) =>
      persist(AuctionStateChangeEvent(AuctionActivated(sender, price))){
        event =>
/*          if (rand.nextInt(3) == 2){
            println("Killing myself!")
            self ! Recover()
          }*/
          log.info("Auction: Bid {} from {}", price, sender.path.name)
          updateState(event)
      }
    case BidTimeExpired() =>
      val localDeleteTimeStarted = System.currentTimeMillis()
      persist(AuctionStateChangeEvent(AuctionSold(localDeleteTimeStarted))){
        event =>
          log.info("Auction: {} reached BidTime: {}", getName, bidTime)
          system.scheduler.scheduleOnce(deleteTime, self, DeleteTimeExpired)
          topBuyer ! WonTheAuction(topPrice)
          updateState(event)
      }
  }

  def auctionIgnored = LoggingReceive {
    case DeleteTimeExpired() =>
      val localDeleteTimeStarted = System.currentTimeMillis()
      persist(AuctionStateChangeEvent(AuctionIgnored(localDeleteTimeStarted))){
        event =>
          log.info("Auction: {} reached DeleteTime: {}", getName, deleteTime)
          updateState(event)
          context stop self
      }
    case Relist() =>
      val localBidTimeStarted = System.currentTimeMillis()
      persist(AuctionStateChangeEvent(AuctionCreated(localBidTimeStarted))){
        event =>
          log.info("Auction: {} is being relisted now", getName)
          updateState(event)
          system.scheduler.scheduleOnce(bidTime, self, BidTimeExpired())
      }
  }
  def auctionSold = LoggingReceive {
    case DeleteTimeExpired() =>
      val localDeleteTimeStarted = System.currentTimeMillis()
      persist(AuctionStateChangeEvent(AuctionIgnored(localDeleteTimeStarted))){
        event =>
          log.info("Auction: {} reached DeleteTime: {}", getName, deleteTime)
          updateState(event)
          context stop self
      }
  }

  def getName: String = {
    context.parent.path.name + '/' + self.path.name
  }


  override def receiveCommand: Receive = auctionNotInitialized

  override def receiveRecover: Receive = {
    case evt: AuctionStateChangeEvent => {
      println("recovering")
      updateState(evt)
    }
    case RecoveryCompleted => {
      println("recovery completed")
      val now = System.currentTimeMillis() / 1000
      val diffBid: Long = now - (bidTimeStarted / 1000)
      val diffDelete: Long = now - (deleteTimeStarted / 1000)
      if(diffDelete != 0 && diffDelete < deleteTime.length){
        log.info("Recovery: DeleteTime {} started at {}, now is {}, so is starts with new {} BidTime",
          deleteTime.length, deleteTimeStarted, now, diffDelete)
        system.scheduler.scheduleOnce(diffDelete seconds, self, DeleteTimeExpired())
      }
      else if (bidTimeStarted != 0 && diffBid < bidTime.length){
        log.info("Recovery: BidTime {} started at {}, now is {}, so is starts with new {} BidTime",
        bidTime.length, bidTimeStarted, now, diffBid)
        system.scheduler.scheduleOnce(diffBid seconds, self, BidTimeExpired())
      }
    }
  }
}

sealed trait AuctionState

case class AuctionCreated(bidTimeStarted: Long) extends AuctionState

case class AuctionActivated(firstBidder: ActorRef, price: Long) extends AuctionState

case class AuctionIgnored(deleteTimeStarted: Long) extends AuctionState

case class AuctionSold(localDeleteTimeExpired: Long) extends AuctionState

case object AcutionNotInitialized extends AuctionState

case class AuctionStateChangeEvent(state: AuctionState)


