package pl.agh.edu.scala.auctionhause.actors

import java.util.Random

import akka.actor.{ActorSystem, ActorRef, Actor, FSM}
import pl.agh.edu.scala.auctionhause.AuctionProducts

import scala.concurrent.duration._

/**
 * Created by bj on 21.10.14.
 */
class Buyer(id: Int, system: ActorSystem) extends Actor with FSM[BuyerState, BuyerData] with AuctionProducts{

  import system.dispatcher

  val MAX_BID_PRICE: Int = 1500
  val SEARCH_DELAY = 3 seconds
  val rand = new Random(System.currentTimeMillis() + id * 12565355)

  startWith(NotBiding, WatchingNoAuction)

  when(NotBiding) {
    case Event(SeeAllAuctions(auctionSearch), _) => {
      log.info("Buyer: {} registered", self.path.name)
      val rand = new Random(System.currentTimeMillis() + id * 12565355)
      val chosenKeyWord = searchList(rand.nextInt(searchList.length))
      log.info("Buyer: {} chose keyword: {}", self.path.name, chosenKeyWord)
      goto(WaitingForAuction) using SearchingFor(auctionSearch, chosenKeyWord)
    }
  }


  onTransition{
    case NotBiding -> WaitingForAuction => {
      for((WatchingNoAuction, SearchingFor(auctionSearch, keyWord)) <- Some(stateData, nextStateData)) {
        system.scheduler.scheduleOnce(SEARCH_DELAY, auctionSearch, Search(keyWord))
      }
    }

    case WaitingForAuction -> Biding => {
      for((SearchingFor(auctionSearch, chosenKeyWord), Watching(auctions)) <- Some(stateData, nextStateData)) {
        val rand = new Random(System.currentTimeMillis())
        val price = rand.nextInt(MAX_BID_PRICE) * id/2 + 100/id
        log.info("Auction: {} auctions are going to be raised now", auctions.length)
        auctions.foreach(_ ! Bid(price))
        //for (i <- 1 until (rand.nextInt(id) + 2)){
        //  system.scheduler.scheduleOnce(id seconds, context.parent, Bid(auction, price + ((rand.nextInt(i + 2) + 2) * 10)))
        //}
      }
    }
  }

  when(WaitingForAuction) {
    case Event(Found(phrase, auctions), SearchingFor(auctionSearch, chosenKeyWord)) => {
      log.info("Auction: Searched {} for {} and found {} auctions: {}", sender.path.name, phrase, auctions.length, auctions.map(_.path.name))
      if (auctions.length > 0) {
        goto(Biding) using Watching(auctions)
      } else {
        stay
      }
    }
  }

  when(Biding) {
    case Event(Found(phrase, newAuctions), Watching(auctions)) => {
      log.info("Auction: Searched {} for {} and found another {} auctions: {}", sender.path.name, phrase, newAuctions.length, newAuctions.map(_.path.name))
      if (auctions.length > 0) {
        val rand = new Random(System.currentTimeMillis())
        val price = rand.nextInt(MAX_BID_PRICE) * id/2 + 50/id
        log.info("Auction: {} auctions are going to be raised now", auctions.length)
        newAuctions.foreach(_ ! Bid(price))
        stay using Watching(newAuctions ::: auctions)
      } else {
        stay
      }
    }
    case Event(WonTheAuction(price), _) => {
      log.info("Buyer: I won the {} with ${} bid", sender.path.name, price)
      stay
    }
    case Event(NewTopBuyer(topPrice, topBuyer), Watching(auctions)) => {
      log.info("Buyer: {} is a new top buyer and offered {}", topBuyer.path.name, topPrice)
      if (rand.nextBoolean() && topPrice < MAX_BID_PRICE){
        val maxBidPrice: Int = MAX_BID_PRICE - topPrice.toInt
        sender ! Bid(rand.nextInt(maxBidPrice) + topPrice)
      }
      stay
    }
    case Event(CurrentOfferIsHigher(topPrice), Watching(auctions)) => {
      log.info("Buyer: {} is still the highest offer", topPrice)
      if (rand.nextInt(3) < 3 && topPrice < MAX_BID_PRICE){
        val maxBidPrice: Int = MAX_BID_PRICE - topPrice.toInt
        sender ! Bid(rand.nextInt(maxBidPrice) + topPrice + 1)
      }
      stay
    }
  }
}

sealed trait BuyerState
case object NotBiding extends BuyerState
case object Biding extends BuyerState
case object WaitingForAuction extends BuyerState

sealed trait BuyerData
case class SearchingFor(auctionSearch: ActorRef, keyWord: String) extends BuyerData
case object WatchingNoAuction extends BuyerData
case class Watching(auctions: List[ActorRef]) extends BuyerData
