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

import algebra.ring.AdditiveMonoid
import cats.Monad
import cats.collections.Heap
import cats.data.NonEmptyList
import cats.kernel.Order
import cats.syntax.all.*
import fs2.Pull
import fs2.Stream
import schrodinger.kernel.Categorical
import schrodinger.kernel.Gumbel
import schrodinger.math.Logarithmic
import schrodinger.math.syntax.*

object AStarSampler:
  trait Proposal[F[_], P, S, A]:
    def sample(subset: S): F[A]
    def measure(subset: S): F[P]
    def support: S

  trait Perturbation[F[_], P, S, A]:
    def perturb(a: A): F[P]
    def split(subset: S, bound: P): F[NonEmptyList[(S, P)]]

  def apply[F[_], G, P, S, A](
      proposal: Proposal[F, P, S, A],
      perturbation: Perturbation[F, P, S, A],
  )(using
      Monad[F],
      Logarithmic[G, P],
      AdditiveMonoid[P],
      Categorical[F, NonEmptyList[P], Long],
      Gumbel[F, G],
      TruncatedGumbel[F, G],
      AdditiveMonoid[G],
      Order[G],
  ): Stream[F, (A, G)] =

    import proposal.*
    import perturbation.*

    final case class UpperBound(gumbel: G, bound: P, subset: S, volume: P) {
      val value: G = gumbel + bound.toLinear
    }
    given Order[UpperBound] = Order.reverse(Order.by(_.value))

    final case class LowerBound(value: G, sample: A)
    given Order[LowerBound] = Order.reverse(Order.by(_.value))

    def go(
        upperBounds0: Heap[UpperBound],
        lowerBounds0: Heap[LowerBound],
    ): Pull[F, (A, G), Unit] =

      val (UpperBound(gumbel, bound, subset, volume), upperBounds) = upperBounds0.pop.get

      val updateBounds = for
        X <- sample(subset)
        oX <- perturb(X)
        lowerBounds = lowerBounds0.add(LowerBound(gumbel + oX.toLinear, X))

        gumbel <- TruncatedGumbel(volume.toLinear, gumbel)
        shouldSplit = lowerBounds.getMin.get.value <
          Order[G].max(gumbel + bound.toLinear, upperBounds.getMin.get.value)

        upperBounds <-
          if shouldSplit then
            for
              subsets <- split(subset, ???)
              volumes <- subsets.traverse((s, _) => measure(s))
              heir <- Categorical(volumes)
              upperBounds <-
                def go(
                    upperbounds: Heap[UpperBound],
                    subsets: List[(S, P)],
                    volumes: List[P],
                    index: Long,
                ): F[Heap[UpperBound]] =
                  (subsets, volumes) match
                    case ((subset, bound) :: subsets, volume :: volumes) =>
                      (if index == heir then gumbel.pure
                       else TruncatedGumbel(volume.toLinear, gumbel)).flatMap { gumbel =>
                        val upperBound = UpperBound(gumbel, bound, subset, volume)
                        go(
                          upperBounds.add(upperBound),
                          subsets,
                          volumes,
                          index + 1,
                        )
                      }

                    case _ => upperBounds.pure

                go(upperBounds, subsets.toList, volumes.toList, 0)
            yield upperBounds
          else upperBounds.add(UpperBound(gumbel, bound, subset, volume)).pure
      yield (upperBounds, lowerBounds)

      Pull.eval(updateBounds).flatMap { (upperBounds, lowerBounds0) =>
        val (LowerBound(value, sample), lowerBounds) = lowerBounds0.pop.get

        if value >= upperBounds.getMin.get.value then
          Pull.output1(sample -> value) >> go(upperBounds, lowerBounds)
        else go(upperBounds, lowerBounds0)
      }

    ???
