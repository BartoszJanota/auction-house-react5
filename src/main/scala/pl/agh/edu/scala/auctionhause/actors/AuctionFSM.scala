package pl.agh.edu.scala.auctionhause.actors

import akka.actor._
import pl.agh.edu.scala.auctionhause.actors

import scala.concurrent.duration._



/**
  * Created by bj on 21.10.14.
 */
case class AuctionFSM(bidTime: FiniteDuration, deleteTime: FiniteDuration, system: ActorSystem, auctionSearchName: String, masterSearch: ActorRef) extends Actor with FSM[FSMAuctionState, AuctionData]{

  import system.dispatcher

  val auctionSearchPath: String = "../../" + auctionSearchName

  startWith(NotInitialized, NotInitializedData)

  when(NotInitialized) {
    case Event(Start, _) => {
      log.info("Auction: {} is being started, bidTime: {}, deleteTime: {}", getName, bidTime, deleteTime)
      goto(Created) using NotBiddedYet
    }

  }

  onTransition{
    case NotInitialized -> Created => {
      for((NotInitializedData, NotBiddedYet) <- Some(stateData, nextStateData)) {
        val search: ActorSelection = context.actorSelection(auctionSearchPath)
        search ! Register()
        system.scheduler.scheduleOnce(bidTime, self, BidTimeExpired)
      }
    }
    case Ignored -> Created => {
      context.actorSelection(auctionSearchPath) ! Register
      system.scheduler.scheduleOnce(bidTime, self, BidTimeExpired)
    }
    case Created -> Ignored => {
      system.scheduler.scheduleOnce(deleteTime, self, DeleteTimeExpired)
      context.actorSelection(auctionSearchPath) ! Unregister()
    }
    case Activated -> Sold =>{
      context.actorSelection(auctionSearchPath) ! Unregister()
    }
  }

  when(Created){
    case Event(BidTimeExpired, _)=>{
      log.info("Auction: {} reached BidTime: {}", getName, bidTime)
      context.parent ! BeingIgnored
      goto(Ignored)
    }
    case Event(Bid(price), NotBiddedYet) => {
      log.info("Auction: Buyer {} is biding with ${}", sender.path.name, price)
      goto (Activated) using Bidded(List(sender), sender, price)
    }
  }

  when(Activated){
    case Event(Bid(price), Bidded(currentBuyers, topBuyer, topPrice)) => {
      log.info("Auction: Buyer {} is biding with ${}", sender.path.name, price)
      if (price > topPrice){
        (currentBuyers diff List(sender)).foreach(_ ! NewTopBuyer(price, sender))
        stay using Bidded(sender :: (currentBuyers diff List(sender)), sender, price)
      } else {
        sender ! CurrentOfferIsHigher(topPrice)
        stay
      }
    }
    case Event(BidTimeExpired, Bidded(currentBuyers, topBuyer, topPrice)) => {
      log.info("Auction: {} reached BidTime: {}", getName, bidTime)
      system.scheduler.scheduleOnce(deleteTime, self, DeleteTimeExpired)
      topBuyer ! WonTheAuction(topPrice)
      goto(Sold)
    }
  }

  def getName: String = {
    context.parent.path.name + '/' + self.path.name
  }

  when(Ignored){
    case Event(DeleteTimeExpired, _) => {
      log.info("Auction: {} reached DeleteTime: {}", getName, deleteTime)
      context stop self
      stay
    }
    case Event(Relist, _) => {
      log.info("Auction: {} is being relisted now", getName)
      goto(Created) using NotBiddedYet
    }
  }

  when(Sold){
    case Event(DeleteTimeExpired, _) => {
      log.info("Auction: {} reached DeleteTime: {}", getName, deleteTime)
      context stop self
      stay
    }
    case Event(_, _) =>
      stay
  }

}

sealed trait FSMAuctionState

case object Created extends FSMAuctionState

case object Activated extends FSMAuctionState

case object Ignored extends FSMAuctionState

case object Sold extends FSMAuctionState

case object NotInitialized extends FSMAuctionState


sealed trait AuctionData

case object NotBiddedYet extends AuctionData

case class Bidded(buyers: List[ActorRef], topBuyer: ActorRef, topPrice: Long) extends AuctionData

case object NotCreatedYet extends AuctionData

case object NotInitializedData extends AuctionData

