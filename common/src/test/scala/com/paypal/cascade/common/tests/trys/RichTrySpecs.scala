/**
 * Copyright 2013-2014 PayPal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.paypal.cascade.common.tests.trys

import org.specs2._
import org.scalacheck.Prop.{Exception => PropException, _}
import org.scalacheck.Arbitrary._
import scala.util.Try
import com.paypal.cascade.common.trys._
import com.paypal.cascade.common.tests.scalacheck._

/**
 * RichTry is a convenience wrapper for converting Try to Either
 */
class RichTrySpecs
  extends Specification
  with ScalaCheck { override def is = s2"""

  RichTry is an implicit wrapper for Try objects

  toEither should
    on a Try[A] Success, return an Either[Throwable, A] Right with the Success value      ${ToEither.SuccessCase().ok}
    on a Try[A] Failure, return an Either[Throwable, A] Left with the Failure exception   ${ToEither.FailureCase().fails}

  toEither[LeftT] should
    on a Try[A] Success, return an Either[LeftT, A] Right with the Success value          ${ToEitherWithConversion.SuccessCase().ok}
    on a Try[A] Failure, return an Either[LeftT, A] Left with the converted Failure value ${ToEitherWithConversion.FailureCase().fails}

  toFuture should
    on a Try[A] Success, return a Future.successful(value)                                ${ToFuture.SuccessCase().ok}
    on a Try[A] Failure, return a Future.failed(value)                                    ${ToFuture.FailureCase().fails}

  """

  object ToEither {

    case class SuccessCase() {
      def ok = forAll(arbitrary[String]) { s =>
        Try { s }.toEither must beRight.like { case v: String =>
          v must beEqualTo(s)
        }
      }
    }
    case class FailureCase() {
      def fails = forAll(arbitrary[Exception]) { e =>
        Try[String] { throw e }.toEither must beLeft.like { case v: Exception =>
          v must beEqualTo(e)
        }
      }
    }
  }

  object ToEitherWithConversion {

    case class SuccessCase() {
      def ok = forAll(arbitrary[String]) { s =>
        Try { s }.toEither[Int](_.getMessage.length) must beRight.like { case v: String =>
          v must beEqualTo(s)
        }
      }
    }
    case class FailureCase() {
      def fails = forAll(arbitrary[Exception], arbitrary[Int]) { (e, i) =>
        Try[String] { throw e }.toEither[Int](_ => i) must beLeft.like { case v: Int =>
          v must beEqualTo(i)
        }
      }
    }
  }

  object ToFuture {

    case class SuccessCase() {
      def ok = forAll(arbitrary[String]) { s =>
        val res = Try { s }.toFuture
        res.value.get must beASuccessfulTry
      }
    }
    case class FailureCase() {
      def fails = forAll(arbitrary[Exception]) { e =>
        val res = Try { throw e }.toFuture
        res.value.get must beAFailedTry
      }
    }
  }

}
