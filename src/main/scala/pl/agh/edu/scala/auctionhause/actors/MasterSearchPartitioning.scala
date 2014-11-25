package pl.agh.edu.scala.auctionhause.actors

import akka.actor.{Props, Actor}
import akka.event.LoggingReceive
import akka.routing._


/**
 * Created by bj on 11.11.14.
 */
class MasterSearchPartitioning extends Actor{

  val NUM_OF_ROUTEES = 3

  val routees = Vector.fill(NUM_OF_ROUTEES){
    val r = context.actorOf(Props[AuctionSearch])
    context watch r
    ActorRefRoutee(r)
  }
  var registerRouter = {
    Router(RoundRobinRoutingLogic(), routees)
  }

  var searchRouter = {
    Router(BroadcastRoutingLogic(), routees)
  }

  override def receive: Receive = LoggingReceive{
    case register: Register => {
      registerRouter.route(register, sender())
    }
    case unregister: Unregister => {
      searchRouter.route(unregister, sender())
    }
    case search: Search => {
      searchRouter.route(search, sender())
    }
  }
}
