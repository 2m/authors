package lt.dvim.scala.compat.javaslang

import io.vavr.control.{Option => VavrOption}
import io.vavr.{Function1 => VavrFunction1, Function2 => VavrFunction2, Function3 => VavrFunction3}

object FunctionConverters {
  implicit def scalaFunction1ToJavaslangFunction2[T1, R](f: (T1) => R): VavrFunction1[T1, R] =
    new VavrFunction1[T1, R] {
      def apply(t1: T1): R = f(t1)
    }

  implicit def scalaFunction2ToJavaslangFunction2[T1, T2, R](f: (T1, T2) => R): VavrFunction2[T1, T2, R] =
    new VavrFunction2[T1, T2, R] {
      def apply(t1: T1, t2: T2): R = f(t1, t2)
    }

  implicit def scalaFunction3ToJavaslangFunction3[T1, T2, T3, R](
      f: (T1, T2, T3) => R
  ): VavrFunction3[T1, T2, T3, R] =
    new VavrFunction3[T1, T2, T3, R] {
      def apply(t1: T1, t2: T2, t3: T3): R = f(t1, t2, t3)
    }
}

object OptionConverters {
  implicit def javaslangOptionToScalaOption[T](o: VavrOption[T]): Option[T] =
    if (o.isDefined) {
      Some(o.get)
    } else {
      None
    }
}
