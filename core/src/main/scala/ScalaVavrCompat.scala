package lt.dvim.scala.compat.vavr

import io.vavr.control.{Option => VavrOption}

object OptionConverters {
  implicit def javaslangOptionToScalaOption[T](o: VavrOption[T]): Option[T] =
    if (o.isDefined) {
      Some(o.get)
    } else {
      None
    }
}
