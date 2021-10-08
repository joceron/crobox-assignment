package crobox.rest

import cats.effect.Async
import cats.effect.kernel.Resource
import cats.effect.kernel.Resource.ExitCase
import cats.implicits._
import crobox.handlers.EventHandler
import crobox.util.LoggingSupport
import io.circe.{Encoder, Json, JsonObject}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpApp, HttpRoutes}

import scala.concurrent.duration.FiniteDuration

class Server[F[_]](port: Int, host: String, handler: EventHandler[F])(implicit
    F: Async[F]
) extends Http4sDsl[F] with LoggingSupport[F] {

  implicit val finiteDurationEncoder: Encoder[FiniteDuration] = (a: FiniteDuration) =>
    Json.fromJsonObject(JsonObject("length" -> Json.fromLong(a.length), "unit" -> Json.fromString(a.unit.name)))

  implicit val floatEncoder: Encoder[Float]                   = (f: Float) => Json.fromString(f.toString)

  private val endpoints: HttpApp[F] =
    HttpRoutes
      .of[F] {
        case GET -> Root / "average" / "session"  =>
          for {
            resp     <- handler.averageSession
            response <- Ok(resp)
          } yield response
        case GET -> Root / "average" / "ratio"    =>
          for {
            resp     <- handler.cartDetailRate
            response <- Ok(resp)
          } yield response
        case GET -> Root / "average" / "pageview" =>
          for {
            resp     <- handler.averageViewDuration
            response <- Ok(resp)
          } yield response
        case GET -> Root / "most-viewed"          =>
          for {
            resp     <- handler.mostViewedProduct
            response <- Ok(resp)
          } yield response
        case GET -> Root / "most-added"           =>
          for {
            resp     <- handler.mostAddedToCart
            response <- Ok(resp)
          } yield response
      }
      .orNotFound

  def serveAsResource: Resource[F, Unit] =
    BlazeServerBuilder[F]
      .bindHttp(port, host)
      .withHttpApp(endpoints)
      .resource
      .evalMap(_ => F.delay(println(s"REST service started at port $port")))
      .onFinalizeCase {
        case ExitCase.Succeeded  => logger.info("RestEndpoint finalized with Succeeded")
        case ExitCase.Errored(e) => logger.error(s"RestEndpoint finalized with Error $e")
        case ExitCase.Canceled   => logger.debug(s"RestEndpoint finalized with Canceled")
      }

  def serve(): F[Unit] = serveAsResource.use(_ => F.never)
}
