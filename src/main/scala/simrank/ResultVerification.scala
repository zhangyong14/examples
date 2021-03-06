/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simrank

import no.uib.cipr.matrix.DenseMatrix

import scala.collection.mutable

/**
 * Result verification trait for small matrix multiplication
 */
trait ResultVerification extends AbstractSimRankImpl {

  def adjMatrix: DenseMatrix = {
    val matrix = new DenseMatrix(graphSize, graphSize)
    val graphMap = new mutable.HashMap[Int, mutable.ArrayBuffer[(Int, Double)]]()
    val data = initializeGraphDataLocally(graphPath)
    (data.map(r => ((r._1._2, r._1._1), r._2)) ++ data).foreach { r =>
      val buf = graphMap.getOrElseUpdate(r._1._2, mutable.ArrayBuffer())
      buf += ((r._1._1, r._2))
    }
    graphMap.flatMap { kv => kv._2.map(e => ((e._1, kv._1), e._2 / kv._2.length))}
      .foreach(r => matrix.set(r._1._1, r._1._2, r._2))
    matrix
  }

  def simrankMatrix: DenseMatrix = {
    val matrix = new DenseMatrix(graphSize, graphSize)
    (0 until graphSize).foreach(i => matrix.set(i, i, 1.0))
    matrix
  }

  def verify(result: Array[((Int, Int), Double)]): Boolean = {
    var simMat = simrankMatrix
    val transAdjMat = adjMatrix.transpose()
    for (iter <- 1 to iterations) {
      val mat = new DenseMatrix(graphSize, graphSize)
      val ret = new DenseMatrix(graphSize, graphSize)
      transAdjMat.mult(0.8, simMat, mat)
      transAdjMat.mult(mat.transpose(), ret)

      (0 until graphSize).foreach(i => ret.set(i, i, 1.0))
      simMat = ret
    }

    val compA = new DenseMatrix(graphSize, graphSize)
    result.foreach(r => compA.set(r._1._1, r._1._2, r._2))
    val compB = simMat

    var identity = true
    import scala.collection.JavaConversions._
    compA.iterator zip compB.iterator foreach { key =>
      val (i1, i2) = key
      //println("i1: " + i1.row() + "," + i1.column() + ":" + i1.get())
      //println("i2: " + i2.row() + "," + i2.column() + ":" + i2.get())
      if (scala.math.abs(i1.get - i2.get) > 1e-8) identity = false
    }

    identity
  }
}

