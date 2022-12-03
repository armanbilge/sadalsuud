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

import algebra.ring.AdditiveSemigroup
import cats.collections.Heap
import cats.data.NonEmptyList
import cats.kernel.Order
import fs2.Pull
import fs2.Stream
import schrodinger.kernel.Gumbel
import schrodinger.math.Logarithmic
import schrodinger.math.syntax.*
import schrodinger.montecarlo.Weighted

object AStarSampler:
  trait Proposal[F[_], W, S, A]:
    def sample: F[Weighted[W, A]]
    def measure(subset: S): F[W]

  trait Target[F[_], W, S, A]:
    def density(a: A): F[W]
    def split(subset: S, bound: W): F[NonEmptyList[(S, W)]]

  def apply[F[_], R, W, S, A](
      proposal: Proposal[F, W, S, A],
      target: Target[F, W, S, A],
  )(using
      Logarithmic[R, W],
      Gumbel[F, R],
      TruncatedGumbel[F, R],
      AdditiveSemigroup[R],
      Order[R],
  ): Stream[F, Weighted[W, A]] =

    case class UpperBound(gumbel: R, bound: R, subset: S)
    given Order[UpperBound] = Order.reverse(Order.by(ub => ub.gumbel + ub.bound))

    case class LowerBound(bound: R, sample: Weighted[W, A])
    given Order[LowerBound] = Order.reverse(Order.by(_.bound))

    def go(
        upperBounds: Heap[UpperBound],
        lowerBounds: Heap[LowerBound],
    ): Pull[F, Weighted[W, A], Unit] =
      ???

    ???
