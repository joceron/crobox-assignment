package crobox.handlers

import cats.effect.Async
import cats.implicits._
import crobox.stores.SessionStore
import crobox.stores.domain.PageType.OverView
import crobox.stores.domain.{AddCart, Event, PageView}
import crobox.util.LoggingSupport

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

trait EventHandler[F[_]] {
  def averageSession: F[FiniteDuration]
  def mostViewedProduct: F[String]
  def mostAddedToCart: F[String]
  def cartDetailRate: F[Float]
  def averageViewDuration: F[FiniteDuration]
}

object EventHandler {

  def apply[F[_]: Async](store: SessionStore[F]): EventHandler[F] =
    new EventHandler[F] with LoggingSupport[F] {

      override def averageSession: F[FiniteDuration] =
        for {
          _                <- logger.info("Got average session duration request")
          visitorIds       <- store.getAllVisitors
          sessionDurations <- visitorIds.traverse(sessionDuration)
          result            = FiniteDuration(sessionDurations.sum / sessionDurations.length, TimeUnit.SECONDS)
          _                <- logger.info(s"Average session duration is ${result.toString}")
        } yield result

      override def mostViewedProduct: F[String] =
        for {
          _             <- logger.info("Got most viewed product request")
          pageViews     <- store.getAllOverviews
          overViewEvents = pageViews.filter(_.pageType == OverView)
          allProducts    = overViewEvents.flatMap(_.pageIds)
          result         = allProducts.groupBy(identity).maxBy(_._2.size)._1
          _             <- logger.info(s"Most viewed product is $result")
        } yield result

      override def mostAddedToCart: F[String] =
        for {
          _          <- logger.info("Got most added product to cart request")
          cartEvents <- store.getAllCartEvents
          result      = productOccurrences(cartEvents).maxBy(_._2)._1
          _          <- logger.info(s"Most added product to cart is $result")
        } yield result

      override def cartDetailRate: F[Float] =
        for {
          _                <- logger.info("Got average cart-to-detail rate request")
          cartEvents       <- store.getAllCartEvents.map(_.map(_.productIds))
          detailProducts   <- getDetailProducts
          cartRepetitions   = calculateRepetitions(cartEvents)
          detailRepetitions = calculateRepetitions(detailProducts)
          ratios            = calculateRatio(cartRepetitions, detailRepetitions)
          _                <- logger.debug(s"The cart-to-detail ratios are ${ratios.mkString(",")}")
          ratioValues       = ratios.values.toSeq
          average           = ratioValues.sum / ratioValues.length
          _                <- logger.info(s"Average cart-to-detail ratio is $average")
        } yield average

      override def averageViewDuration: F[FiniteDuration] =
        for {
          _           <- logger.info("Got average pageview duration request")
          allSessions <- store.getAllSessions
          allDurations = getPageViewDurations(allSessions)
          _           <- logger.debug(s"All pageview durations are ${allDurations.mkString(",")}")
          result       = FiniteDuration(allDurations.sum / allDurations.length, TimeUnit.SECONDS)
          _           <- logger.info(s"Average pageview duration is ${result.toString}")
        } yield result

      // In seconds
      private def sessionDuration(visitorId: String): F[Int] =
        store.get(visitorId).map { events =>
          (events.headOption, events.lastOption) match {
            case (Some(firstEvent), Some(lastEvent)) =>
              ((lastEvent.eventTs - firstEvent.eventTs) / 1000).toInt // Timestamp in ms
            case _                                   => 0
          }
        }

      private def productOccurrences(cartEvents: Seq[AddCart]): Map[String, Int] =
        cartEvents.foldLeft(Map[String, Int]()) {
          case (acc, cartEvent) =>
            acc.get(cartEvent.productIds) match {
              case Some(occurrences) => acc + (cartEvent.productIds -> (occurrences + cartEvent.productQty))
              case None              => acc + (cartEvent.productIds -> cartEvent.productQty)
            }
        }

      // There's always one single product in the .json, that's why headOption
      private def getDetailProducts: F[Seq[String]] = store.getAllDetailEvents.map(_.flatMap(_.pageIds.headOption))

      private def calculateRepetitions[T](collection: Seq[T]): Map[T, Int]                 =
        collection.groupBy(identity).map { case (product, repetitions) => (product, repetitions.length) }

      private def calculateRatio[T](base: Map[T, Int], target: Map[T, Int]): Map[T, Float] =
        base.foldLeft(Map[T, Float]()) {
          case (acc, productWithRepetitions) =>
            target.get(productWithRepetitions._1) match {
              case Some(value) =>
                acc + (productWithRepetitions._1 -> (productWithRepetitions._2.toFloat / value.toFloat))
              case None        => acc + (productWithRepetitions._1 -> 0f)
            }
        }

      private def getPageViewDurations(data: Map[String, Seq[Event]]): List[Long] = {
        final case class Accumulator(total: List[Long], lastPageView: Option[Event])

        data.foldLeft(List[Long]()) {
          case (accumulator, events) =>
            val sessionDurations = events
              ._2
              .foldLeft(Accumulator(List.empty, None)) {
                case (acc, currentEvent) =>
                  currentEvent match {
                    case pageView: PageView =>
                      acc
                        .lastPageView
                        .fold(acc.copy(lastPageView = Some(pageView)))(last =>
                          acc.copy(
                              total = (currentEvent.eventTs - last.eventTs) / 1000 :: acc.total // Timestamp in ms
                            , lastPageView = Some(pageView)
                          )
                        )
                    case _                  => acc
                  }
              }
              .total
            accumulator.concat(sessionDurations)
        }
      }

    }

}
