package lt.dvim.scala.compat.javaslang

import javaslang.control.{Option => JsOption}

object FunctionConverters {
  implicit def scalaFunction1ToJavaslangFunction2[T1, R](f: (T1) => R): javaslang.Function1[T1, R] =
    new javaslang.Function1[T1, R] {
      def apply(t1: T1): R = f(t1)
    }

  implicit def scalaFunction2ToJavaslangFunction2[T1, T2, R](f: (T1, T2) => R): javaslang.Function2[T1, T2, R] =
    new javaslang.Function2[T1, T2, R] {
      def apply(t1: T1, t2: T2): R = f(t1, t2)
    }

  implicit def scalaFunction3ToJavaslangFunction3[T1, T2, T3, R](
      f: (T1, T2, T3) => R): javaslang.Function3[T1, T2, T3, R] =
    new javaslang.Function3[T1, T2, T3, R] {
      def apply(t1: T1, t2: T2, t3: T3): R = f(t1, t2, t3)
    }
}

object OptionConverters {
  implicit def javaslangOptionToScalaOption[T](o: JsOption[T]): Option[T] =
    if (o.isDefined) {
      Some(o.get)
    } else {
      None
    }
}
