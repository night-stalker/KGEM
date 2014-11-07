package edu.gsu.cs.kgem.model

import edu.gsu.cs.kgem.exec.log
import scala.collection.mutable
import Math.min

/**
 * Created with IntelliJ IDEA.
 * User: aartyomenko
 * Date: 4/13/13
 * Time: 1:43 PM
 * Class to wrap derandomized KGEM, i. e. initialization with maximizing distance between
 * seeds and detection of size of the population via distance threshold.
 */
object MaxDistanceSeedFinder {

  /**
   * Find seeds according to maximization Hamming distance between all pairs
   * of reads with pre-specified threshold. This is a 2-approximation to the
   * metric k-center problem.
   *
   * @param reads
   * Collection of reads
   * @param k
   * Maximum size of sample
   * @param threshold
   * Min hamming distance between seeds
   * @return
   */
  def findSeeds(reads: Iterable[Read], k: Int, threshold: Int, count_threshold: Int): Iterable[Genotype] = {
    val readArr = reads.toArray
    val first = getFirstSeed(readArr)
    var seeds = new mutable.MutableList[Read]()
    seeds += first
    var distanceMap = readArr.view.filter(r => !r.equals(first)).map(r => r -> hammingDistance(first, r) * r.freq).toMap
    var maxHD = Double.MaxValue
    var count = 0.0
    log("Count threshold: %d".format(threshold))
    while (seeds.size < k && (maxHD >= threshold || count > count_threshold)) {
      if (distanceMap.isEmpty) return seeds.map(r => new Genotype(r.seq))
      val cur = distanceMap.maxBy(r => r._2 * r._1.freq)
      maxHD = distanceMap.view.map(r => r._2 / r._1.freq).max

      if (maxHD < threshold) count = readArr.view.filter(r => hammingDistance(r, cur._1) < distanceMap(r) / r.freq).map(_.freq).sum
      seeds += cur._1
      distanceMap = distanceMap.map(e => e._1 -> min(e._2, hammingDistance(cur._1, e._1) * e._1.freq)).toMap
    }
    log("Estimated k: %d".format(seeds.size))
    log("Final max distance: %.0f count: %.0f".format(maxHD, count))
    seeds.map(r => new Genotype(r.seq))
  }

  /**
   * Select first read randomly
   * @param readArr
   * Array with all reads
   * @return
   * one read
   */
  private def getFirstSeed(readArr: Array[Read]) = {
    val mc = readArr.map(r => r.seq.count(_ != 'N')).max
    val candidates = readArr.filter(r => r.seq.count(_ != 'N') == mc)
    candidates.maxBy(_.freq)
    //val s = candidates.size
    //val rnd = new Random()
    //candidates(rnd.nextInt(s))
  }

  /**
   * Wrapper for hamming distance between reads
   * @param r1
   * Read 1
   * @param r2
   * Read 2
   * @return
   * Hamming Distance between reads
   */
  @inline
  private def hammingDistance(r1: Read, r2: Read): Int = {
    if (r1.equals(r2)) return 0
    hammingDistance(r1.seq, r2.seq)
  }

  /**
   * Compute hamming distance between two strings
   * of the same length
   * @param s
   * String 1
   * @param t
   * String 2
   * @return
   * Hamming distance between s and t if
   * their length is the same and -1
   * otherwise
   */
  @inline
  def hammingDistance(s: String, t: String): Int = {
    val l = s.length
    if (l != t.length) {
      throw new IllegalArgumentException("Hamming Distance: Strings have different lengths")
    }
    var r = 0
    for (i <- 0 until l) {
      if (s(i) != t(i) && s(i) != 'N' && t(i) != 'N' && t(i) != ' ' && s(i) != ' ') {
        r += 1
      }
    }
    r
  }
}
