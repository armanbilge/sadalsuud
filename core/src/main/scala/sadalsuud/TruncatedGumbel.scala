/*
 * Copyright 2022 Arman Bilge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sadalsuud

import cats.Functor
import cats.syntax.all.*
import schrodinger.kernel.Exponential

trait TruncatedGumbel[F[_], A]:
  def truncatedGumbel(location: A, truncation: A): F[A]

object TruncatedGumbel:
  inline def apply[F[_], A](location: A, truncation: A)(using tc: TruncatedGumbel[F, A]): F[A] =
    tc.truncatedGumbel(location, truncation)

  given [F[_]: Functor](using Exponential[F, Double]): TruncatedGumbel[F, Double] with
    def truncatedGumbel(location: Double, truncation: Double): F[Double] =
      Exponential.standard.map { expo =>
        location - Math.log(Math.exp(location - truncation) + expo)
      }
