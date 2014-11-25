package pl.agh.edu.scala.auctionhause

import akka.actor.{Props, ActorSystem}
import akka.event.Logging
import pl.agh.edu.scala.auctionhause.actors.{OpenHouse, HouseManager}

/**
 * Created by bj on 21.10.14.
 */
object AuctionHouse {

  val system = ActorSystem("AuctionHouse")

  val log = Logging(system, AuctionHouse.getClass.getName)

  def main(args: Array[String]) {
    log info "AuctionHouse: Auction House has been initialized!"

    val houseManager = system.actorOf(Props(new HouseManager(system)), "manager")

    houseManager ! OpenHouse
  }
}
