package pl.agh.edu.scala.auctionhause

/**
 * Created by bj on 04.11.14.
 */
trait AuctionProducts {

  //val products = List("Notebook", "Tablet", "Phone", "Smartphone", "Screen", "Keyboard", "PC", "Printer")
  val products = List("Notebook", "Tablet")
  //val sizes = List("Small", "Big", "Average")
  val sizes = List("Small")
  //val colors = List("Black", "White", "Gray", "Red", "Orange")
  val colors = List("Black", "White")
  val searchList = products ::: sizes ::: colors

}
