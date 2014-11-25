package pl.agh.edu.scala.auctionhause.actors

import akka.actor._

/**
 * Created by bj on 21.10.14.
 */

class HouseManager(system: ActorSystem) extends Actor with FSM[HouseState, HouseData]{
  val MAX_NUM_OF_AUCTIONS = 2
  val NUM_OF_BUYERS = 2
  val NUM_OF_SELLERS = 1
  val AUCTION_SEARCH_NAME: String = "masterSearch/"

  startWith(HouseIsClosed, EmptyHouseData)

  when(HouseIsClosed){
    case Event(OpenHouse, EmptyHouseData) => {
      log.info("HouseManager: House has been opened, initializing {} sellers and {} buyers", NUM_OF_SELLERS, NUM_OF_BUYERS)
      //val auctionSearch = context.actorOf(Props(new AuctionSearch()), AUCTION_SEARCH_NAME)
      val masterSearch = context.actorOf(Props(new MasterSearchPartitioning()), "masterSearch")
      val sellers = (1 to NUM_OF_SELLERS).map(num => context.actorOf(Props(new Seller(num, system, MAX_NUM_OF_AUCTIONS, AUCTION_SEARCH_NAME, masterSearch)), "seller"+num)).toList
      val buyers = (1 to NUM_OF_BUYERS).map(num => context.actorOf(Props(new Buyer(num, system)), "buyer"+num)).toList
      goto(HouseIsOpened) using NotEmptyHouseData(buyers, sellers, masterSearch)
    }
  }

  onTransition{
    case HouseIsClosed -> HouseIsOpened => {
      for ((EmptyHouseData, NotEmptyHouseData(buyers, sellers, auctionSearch)) <- Some(stateData, nextStateData)){
        log.info("HouseManager: initializing sellers")
        sellers.foreach(_ ! StartAuctioning)
        log.info("HouseManager: initializing buyers")
        buyers.foreach(_ ! SeeAllAuctions(auctionSearch))
      }
    }
  }

  when(HouseIsOpened){
    case _ => {
      stay
    }
//    case Event(Bid(auctionActor, price), NotEmptyHouseData(buyers, sellers, auctionSearch)) => {
//      log.info("HouseManager: Buyer {} is biding {} with ${}", sender.path.name, auctionActor.path.name, price)
//      auctionActor forward Bid(auctionActor, price)
//      stay using NotEmptyHouseData(buyers, sellers, auctionSearch)
//    }
    /*case Event(BidUp(auctionActor, price), NotEmptyHouseData(auctions, buyers)) => {
      log.info("Housemanager: Buyer {} is biding up the {} with ${}", sender.path.name, auctionActor.path.name, price)
      auctionActor forward BidUp(auctionActor, price)
      stay using NotEmptyHouseData(auctions, buyers)
    }*/
  }

}

sealed trait HouseState
case object HouseIsClosed extends HouseState
case object HouseIsOpened extends HouseState

sealed trait HouseData
case object EmptyHouseData extends HouseData
case class NotEmptyHouseData(buyers: List[ActorRef], sellers: List[ActorRef], auctionSearch: ActorRef) extends HouseData
