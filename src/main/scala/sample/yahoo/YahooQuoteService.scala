package sample.yahoo

import java.io.{StringReader, IOException}
import java.net.URL
import java.time.LocalDate

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.scalaspring.akka.http.AkkaHttpClient
import com.github.tototoshi.csv.CSVReader
import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import scala.concurrent.Future

trait QuoteService {
  def historical(symbol: String, begin: LocalDate, end: LocalDate = LocalDate.now()): Future[Either[String, Iterator[Map[String, String]]]]
}

@Component
class YahooQuoteService extends AkkaHttpClient with QuoteService with StrictLogging {

  @Value("${services.yahoo.finance.url:http://real-chart.finance.yahoo.com/table.csv}")
  val url: URL = null

  protected lazy val connectionFlow: Flow[HttpRequest, HttpResponse, Any] = Http().outgoingConnection(url.getHost)

  protected def request(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(connectionFlow).runWith(Sink.head)

  override def historical(symbol: String, begin: LocalDate, end: LocalDate = LocalDate.now()): Future[Either[String, Iterator[Map[String, String]]]] = {
    val query = Map[String, String](
      "s" -> symbol, "g" -> "d",
      "a" -> (begin.getMonthValue - 1).toString, "b" -> begin.getDayOfMonth.toString, "c" -> begin.getYear.toString,
      "d" -> (end.getMonthValue - 1).toString, "e" -> end.getDayOfMonth.toString, "f" -> end.getYear.toString
    )
    val uri: Uri = Uri(url.getPath).withQuery(query)
    logger.info(s"Sending request for $uri")
    request(RequestBuilding.Get(uri)).flatMap { response =>
      response.status match {
        case OK => Unmarshal(response.entity).to[String].map { s => Right(CSVReader.open(new StringReader(s)).iteratorWithHeaders) }
        case NotFound => Future.successful(Left(s"$symbol: bad symbol"))
        case BadRequest => Future.successful(Left(s"$symbol: bad request to $uri"))
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          val error = s"Request to $uri failed with status code ${response.status}"
          logger.error(error)
          Future.failed(new IOException(error))
        }
      }
    }
  }

}
