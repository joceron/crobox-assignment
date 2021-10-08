package crobox.stores

import cats.effect.Async
import cats.implicits._
import crobox.stores.SessionStore._
import crobox.stores.domain.PageType.{Detail, OverView}
import crobox.stores.domain.{AddCart, Event, PageView}

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

trait SessionStore[F[_]] {
  def put(event: Event): F[Unit]
  def get(visitorId: VisitorId): F[Seq[Event]]
  def getAllVisitors: F[Seq[VisitorId]]
  def getAllOverviews: F[Seq[PageView]]
  def getAllCartEvents: F[Seq[AddCart]]
  def getAllDetailEvents: F[Seq[PageView]]
  def getAllSessions: F[Map[VisitorId, Seq[Event]]]
}

// The data looks like a big collection of events from users on an e-commerce website, so normally storing them in
// memory would be a good idea because it will grow too much. On a real situation, I would probably store this on a
// database for reporting purposes (like ClickHouse), or add some sort of expiration policy. But for the sake of this
// exercise, I didn't do neither of those things
object SessionStore {

  type VisitorId = String
  type EventId   = String

  def inMemory[F[_]](implicit F: Async[F]): SessionStore[F] =
    new SessionStore[F] {

      // Don't really need these many Concurrent Hash Maps, but I like them because they are relatively safe for
      // concurrent access, and although this is just an exercise, on a production-like environment, these kind of
      // things usually have a lot of traffic, so it's safer

      // Assuming that the events on the .json are ordered (they seem to be), I used a Seq[Event] so they are kept in
      // chronological order. If they weren't, then I would need to add some small check on the put function to keep
      // them ordered
      private val store = new ConcurrentHashMap[VisitorId, Seq[Event]]()

      // These caches are only used to speed things up on EventHandler
      private val overviewCache = new ConcurrentHashMap[EventId, PageView]()
      private val cartCache     = new ConcurrentHashMap[EventId, AddCart]()
      private val detailCache   = new ConcurrentHashMap[EventId, PageView]()

      override def put(event: Event): F[Unit] =
        for {
          eventsOpt <- F.delay(Option(store.get(event.visitorId)))
          _         <- appendOrInsert(event, eventsOpt)
          _         <- updateCaches(event)
        } yield ()

      override def get(visitorId: VisitorId): F[Seq[Event]]      = F.delay(store.getOrDefault(visitorId, Seq.empty))
      override def getAllVisitors: F[Seq[VisitorId]]             = F.delay(store.keys().asScala.toSeq)
      override def getAllOverviews: F[Seq[PageView]]             = F.delay(overviewCache.values().asScala.toSeq)
      override def getAllCartEvents: F[Seq[AddCart]]             = F.delay(cartCache.values().asScala.toSeq)
      override def getAllDetailEvents: F[Seq[PageView]]          = F.delay(detailCache.values().asScala.toSeq)
      override def getAllSessions: F[Map[VisitorId, Seq[Event]]] = F.delay(store.asScala.toMap)

      private def appendOrInsert(eventToInsert: Event, events: Option[Seq[Event]]): F[Unit] =
        events match {
          case Some(existingEvents) => F.delay(store.put(eventToInsert.visitorId, existingEvents :+ eventToInsert))
          case None                 => F.delay(store.put(eventToInsert.visitorId, Seq(eventToInsert)))
        }

      private def updateCaches(event: Event): F[Unit] =
        event match {
          case pageView: PageView =>
            pageView.pageType match {
              case OverView => F.delay(overviewCache.put(event.eventId, pageView))
              case Detail   => F.delay(detailCache.put(event.eventId, pageView))
              case _        => F.unit
            }
          case addCart: AddCart   => F.delay(cartCache.put(event.eventId, addCart))
          case _                  => F.unit
        }

    }

}
