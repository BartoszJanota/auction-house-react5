package pl.agh.edu.scala.auctionhause.actors

import akka.actor._
import pl.agh.edu.scala.auctionhause.AuctionProducts
import scala.concurrent.duration._

import java.util.Random

/**
 * Created by bj on 04.11.14.
 */
class Seller(id: Int, system: ActorSystem, maxNumOfAuctions: Int, auctionSearchName: String, masterSearch: ActorRef) extends Actor with FSM[SellerState, SellerData] with AuctionProducts{

  val BID_TIME = 20 seconds
  val DELETE_TIME = 2 seconds
  val rand = new Random(System.currentTimeMillis() + id * 12565355)

  startWith(NotAuctioning, NoActiveAuctions)


  def createAuctionNames(num: Int): List[String] = {
    val names = (0 to num).map(num => sizes(rand.nextInt(sizes.length)) + ':' + colors(rand.nextInt(colors.length)) + ':' + products(rand.nextInt(products.length))).toList
    names
  }
  when(NotAuctioning) {
    case Event(StartAuctioning, NoActiveAuctions) => {
      val auctionsToBeActivated = rand.nextInt(maxNumOfAuctions)
      val auctionNames = createAuctionNames(auctionsToBeActivated)
      val filteredAuctionNames = auctionNames.distinct
      val auctions = (0 to filteredAuctionNames.length - 1).map(num => {
        val name: String = filteredAuctionNames(num)
        context.actorOf(Props(new Auction(BID_TIME, DELETE_TIME, system, auctionSearchName, masterSearch, name, id)), name)
      }).toList
      goto(Auctioning) using ActiveAuctions(auctions)
    }
  }

  onTransition{
    case NotAuctioning -> Auctioning => {
      for((NoActiveAuctions, ActiveAuctions(auctions)) <- Some(stateData, nextStateData)){
        log.info("Seller: starting auctioning")
        auctions.foreach(_ ! Start())
      }
    }
  }

  when(Auctioning) {
    case Event(BeingIgnored, ActiveAuctions(auctions)) => {
      log.info("Seller: {} is being ignored now", sender.path.name)
      if (rand.nextBoolean()){
        sender ! Relist
      }
      stay
    }
  }

}


sealed trait SellerState

sealed trait SellerData

case object NotAuctioning extends SellerState

case object Auctioning extends SellerState

case object NoActiveAuctions extends SellerData

case class ActiveAuctions(auctions: List[ActorRef]) extends SellerData



