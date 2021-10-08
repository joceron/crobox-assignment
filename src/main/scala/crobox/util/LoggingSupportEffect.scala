package crobox.util

import cats.effect.Async
import org.slf4j.{Logger, LoggerFactory}

trait LoggingSupport[F[_]] {
  private lazy val instance: Logger = LoggerFactory.getLogger(getClass.getName)

  protected def logger(implicit F: Async[F]): EffectLogger[F] = new EffectLogger[F](instance)
}

class EffectLogger[F[_]](logger: Logger)(implicit F: Async[F]) {
  def info(message: String): F[Unit]  = F.delay(logger.info(message))
  def debug(message: String): F[Unit] = F.delay(logger.debug(message))
  def error(message: String): F[Unit] = F.delay(logger.error(message))
}
