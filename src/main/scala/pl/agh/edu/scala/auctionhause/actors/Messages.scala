package pl.agh.edu.scala.auctionhause.actors

import akka.actor.ActorRef

import scala.concurrent.duration._

/**
 * Created by bj on 21.10.14.
 */

sealed trait HouseManagementSystemMessage

sealed trait BuyersManagementSystemMessage

sealed trait AuctionsManagementSystemMessage

sealed trait AuctionSearchManagementSystemMessage

sealed trait SellerManagementSystemMessage


case class OpenHouse() extends HouseManagementSystemMessage

case class SeeAllAuctions(auctionSearch: ActorRef) extends BuyersManagementSystemMessage

//case class BidUp(auctionActor: ActorRef, price: Long) extends AuctionsManagementSystemMessage

case class Bid(price: Long) extends AuctionsManagementSystemMessage

case class Start() extends AuctionsManagementSystemMessage

case class BidTimeExpired() extends AuctionsManagementSystemMessage

case class DeleteTimeExpired() extends AuctionsManagementSystemMessage

case class WonTheAuction(price: Long) extends AuctionsManagementSystemMessage

case class StartAuctioning() extends SellerManagementSystemMessage

case class StartSearch() extends  AuctionSearchManagementSystemMessage

case class Register() extends AuctionsManagementSystemMessage

case class Unregister() extends AuctionSearchManagementSystemMessage

case class Search(phrase: String) extends AuctionSearchManagementSystemMessage

case class Found(phrase: String, auctions: List[ActorRef]) extends AuctionSearchManagementSystemMessage

case class Deactivated() extends AuctionSearchManagementSystemMessage

case class BeingIgnored() extends AuctionSearchManagementSystemMessage

case class Relist() extends AuctionSearchManagementSystemMessage

case class NewTopBuyer(topPrice: Long, topBuyer: ActorRef) extends AuctionSearchManagementSystemMessage

case class CurrentOfferIsHigher(topPrice: Long) extends  AuctionSearchManagementSystemMessage