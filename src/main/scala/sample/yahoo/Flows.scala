package sample.yahoo

import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import sample.flow.Statistics

import scala.collection.immutable
import akka.stream.scaladsl.FlowGraph.Implicits._
import Bollinger._
import sample.flow._


object Flows {

  // Calculates and adds Bollinger points to a Quote stream
  // Note: Assumes quotes are in descending date order, as provided by Yahoo Finance
  // Note: The number of quotes emitted will be reduced by the window size - 1 (dropped from the tail)
  def bollinger(window: Int = 14) = Flow() { implicit b =>

    val in = b.add(Broadcast[Quote](2))
    val extract = b.add(Flow[Quote].map(_("Close").toDouble))
    val statistics = b.add(Flow[Double].slidingStatistics(window))
    val bollinger = b.add(Flow[Statistics[Double]].map(Bollinger(_)).drop(window - 1))
    // Need buffer to avoid deadlock. See: https://github.com/akka/akka/issues/17435
    val buffer = b.add(Flow[Quote].buffer(Math.max(window - 1, 1), OverflowStrategy.backpressure))
    val zip = b.add(Zip[Quote, Bollinger])
    val merge = b.add(Flow[(Quote, Bollinger)].map(t => t._1 ++ t._2))

    in ~> buffer                             ~> zip.in0
    in ~> extract ~> statistics ~> bollinger ~> zip.in1
                                                zip.out ~> merge

    (in.in, merge.outlet)
  }

  // Converts quotes to CSV with header
  val csv = Flow() { implicit b =>
    def format(q: Quote, header: Boolean): String = if (header) q.keys.mkString(",") else "\n" + q.values.mkString(",")
    val rows = b.add(
      Flow[Quote].prefixAndTail(1).mapConcat[Source[String, Unit]]{ pt =>
        val (prefix, tail) = pt
        immutable.Seq(
          Source(prefix).map(format(_, true)),
          Source(prefix).map(format(_, false)),
          tail.map(format(_, false)))
      }.flatten(FlattenStrategy.concat))

    (rows.inlet, rows.outlet)
  }

}
