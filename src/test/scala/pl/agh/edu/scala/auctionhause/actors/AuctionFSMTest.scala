package pl.agh.edu.scala.auctionhause.actors

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, TestFSMRef, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike, FunSuite}
import pl.agh.edu.scala.auctionhause.actors.BidTimeExpired
import scala.concurrent.duration._

/**
 * Created by bj on 06.11.14.
 */
class AuctionFSMTest extends TestKit(ActorSystem("AuctionTest"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender{

  val BID_TIME = 10 seconds
  val DELETE_TIME = 2 seconds

  val price = 10L
  val raisedPrice = price + 5L

  val fsm = TestFSMRef(new AuctionFSM(BID_TIME, DELETE_TIME, system, "testAuction", null))

  override def afterAll(): Unit = {
    system.shutdown()
  }

  "Auction" must {
    "start" in {
      fsm.setState(NotInitialized, NotInitializedData)

      fsm ! Start

      assert(fsm.stateName == Created)
      assert(fsm.stateData == NotBiddedYet)

    }

    "be ignored after bidTime" in {
      fsm.setState(Created, NotBiddedYet)

      fsm ! BidTimeExpired

      assert(fsm.stateName == Ignored)
    }

    "must be activated" in {
      fsm.setState(Created, NotBiddedYet)

      fsm ! Bid(price)

      assert(fsm.stateName == Activated)
      assume(fsm.stateData == Bidded(List(self), self, price))
    }

    "must be raised" in {
      fsm.setState(Activated, Bidded(List(self), self, price))

      fsm ! Bid(raisedPrice)

      assert(fsm.stateName == Activated)
      assume(fsm.stateData == Bidded(List(self), self, raisedPrice))
    }

    "must be sold" in {
      fsm.setState(Activated, Bidded(List(self), self, price))

      fsm ! BidTimeExpired

      assert(fsm.stateName == Sold)
    }

/*    "must be deleted" in {
      fsm.setState(Ignored)

      fsm ! DeleteTimeExpired

      expectTerminated(self)
    }*/

    "must be relisted" in {
      fsm.setState(Ignored)

      fsm ! Relist

      assert(fsm.stateName == Created)
      assert(fsm.stateData == NotBiddedYet)
    }


  }

}
