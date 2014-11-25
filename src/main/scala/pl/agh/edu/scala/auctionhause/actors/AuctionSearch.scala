package pl.agh.edu.scala.auctionhause.actors

import akka.actor.{ActorRef, Actor, FSM}

/**
 * Created by bj on 04.11.14.
 */
class AuctionSearch() extends Actor with FSM[AuctionSearchState, AuctionSearchData]{

  startWith(NotActivated, NoRegisteredAuctions)

  when(NotActivated){
    case Event(Register(), NoRegisteredAuctions) => {
      log.info("AuctionSearch: {} activated search engine and registered itself.", getAuctionName(sender))
      goto(SearchActivated) using RegisteredAuctions(List(sender))
    }
    case Event(_, _) =>{
      log info "AuctionSearch: not activated yet"
      stay
    }
  }

  def getAuctionName(sender: ActorRef): String = {
    sender.path.parent.name + '/' + sender.path.name
  }

  when(SearchActivated){
    case Event(Register(), RegisteredAuctions(auctions)) => {
      log.info("AuctionSearch: {} registered itself.", getAuctionName(sender))
      stay using RegisteredAuctions(sender :: auctions)
    }
    case Event(Search(phrase), RegisteredAuctions(auctions)) => {
      log.info("AuctionSearch: {} is searching for '{}' auctions", getAuctionName(sender), phrase)
      val foundAuctions: List[ActorRef] = auctions.filter(_.path.name contains phrase).toList
      sender ! Found(phrase, foundAuctions)
      stay
    }
    case Event(Unregister(), RegisteredAuctions(auctions)) =>{
      log.info("AuctionSearch: {} is now being removed from search engine", getAuctionName(sender))
      val filteredAuctions: List[ActorRef] = auctions.filter(_ != sender)
      stay using RegisteredAuctions(filteredAuctions)
    }

  }
}

sealed trait AuctionSearchData

case object NoRegisteredAuctions extends AuctionSearchData

case class RegisteredAuctions(auctions: List[ActorRef]) extends AuctionSearchData

sealed trait AuctionSearchState

case object NotActivated extends AuctionSearchState

case object SearchActivated extends AuctionSearchState

