/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.ml.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.lang.IgniteUuid;
import org.apache.ignite.ml.math.*;
import org.apache.ignite.ml.math.exceptions.ConvergenceException;
import org.apache.ignite.ml.math.exceptions.MathIllegalArgumentException;
import org.apache.ignite.ml.math.functions.Functions;
import org.apache.ignite.ml.math.functions.IgniteBiFunction;
import org.apache.ignite.ml.math.impls.CacheUtils;
import org.apache.ignite.ml.math.impls.matrix.DenseLocalOnHeapMatrix;
import org.apache.ignite.ml.math.impls.matrix.SparseDistributedMatrix;
import org.apache.ignite.ml.math.impls.storage.matrix.SparseDistributedMatrixStorage;
import org.apache.ignite.ml.math.util.MapUtil;
import org.apache.ignite.ml.math.util.MatrixUtil;

import static org.apache.ignite.ml.math.StorageConstants.RANDOM_ACCESS_MODE;
import static org.apache.ignite.ml.math.StorageConstants.ROW_STORAGE_MODE;
import static org.apache.ignite.ml.math.impls.CacheUtils.*;
import static org.apache.ignite.ml.math.util.MatrixUtil.localCopyOf;

/**
 * Clustering algorithm based on Bahmani et al. paper and Apache Spark class with corresponding functionality.
 *
 * @see <a href="http://theory.stanford.edu/~sergei/papers/vldb12-kmpar.pdf">Scalable K-Means++(wikipedia)</a>
 */
public class KMeansDistributedClusterer extends BaseKMeansClusterer<SparseDistributedMatrix> {
    /** */
    private final int maxIterations;

    /** */
    private Random rnd;

    /** */
    private int initSteps;

    /** */
    private long seed;

    /** */
    private double epsilon = 1e-4;

    /** */
    protected KMeansDistributedClusterer(DistanceMeasure measure,
        int initSteps,
        int maxIterations,
        Long seed) {
        super(measure);
        this.initSteps = initSteps;

        // L'Ecuyer, "Tables of Linear Congruential Generators of
        // Different Sizes and Good Lattice Structure", 1999
        AtomicLong seedUniquifier
            = new AtomicLong(8682522807148012L);

        if (seed == null) {
            for (; ; ) {
                long curr = seedUniquifier.get();
                long next = curr * 181783497276652981L;
                if (seedUniquifier.compareAndSet(curr, next)) {
                    this.seed = next;
                    break;
                }
            }
        }

        this.maxIterations = maxIterations;
        rnd = new Random(this.seed);
    }

    /** Service class used for statistics, */
    public static class SumsAndCounts {
        /** */
        public double totalCost;

        /** */
        public ConcurrentHashMap<Integer, Vector> sums = new ConcurrentHashMap<>();

        /** Count of points closest to the center with a given index. */
        public ConcurrentHashMap<Integer, Integer> counts = new ConcurrentHashMap<>();

        /** Merge current */
        public SumsAndCounts merge(SumsAndCounts other) {
            this.totalCost += totalCost;
            MapUtil.mergeMaps(sums, other.sums, Vector::plus, ConcurrentHashMap::new);
            MapUtil.mergeMaps(counts, other.counts, (i1, i2) -> i1 + i2, ConcurrentHashMap::new);
            return this;
        }
    }

    /** */
    @Override public KMeansModel cluster(SparseDistributedMatrix points,
        int k) throws MathIllegalArgumentException, ConvergenceException {

        SparseDistributedMatrix pointsCp = new SparseDistributedMatrix(points.rowSize(),
            points.columnSize(),
            ROW_STORAGE_MODE,
            RANDOM_ACCESS_MODE);

        // TODO: this copy is very ineffective, just for POC. Immutability of data should be guaranteed by other methods
        // such as logical locks for example.
        pointsCp.assign(points);

        Vector[] centers = initClusterCenters(pointsCp, k);

        boolean converged = false;
        int iteration = 0;
        int dim = pointsCp.viewRow(0).size();
        IgniteUuid uid = pointsCp.getUUID();

        // Execute iterations of Lloyd's algorithm until converged
        while (iteration < maxIterations && !converged) {
            SumsAndCounts stats = CacheUtils.distributedFold(SparseDistributedMatrixStorage.ML_CACHE_NAME,
                (IgniteBiFunction<javax.cache.Cache.Entry<IgniteBiTuple<Integer, IgniteUuid>, Map<Integer, Double>>, SumsAndCounts, SumsAndCounts>)(entry, counts) -> {
                    Map<Integer, Double> vec = entry.getValue();

                    IgniteBiTuple<Integer, Double> closest = findClosest(centers, VectorUtils.fromMap(vec, false));
                    int bestCenterIdx = closest.get1();

                    counts.totalCost += closest.get2();
                    counts.sums.putIfAbsent(bestCenterIdx, VectorUtils.zeroes(dim));

                    counts.sums.compute(bestCenterIdx,
                        (IgniteBiFunction<Integer, Vector, Vector>)(ind, v) -> v.plus(VectorUtils.fromMap(vec, false)));

                    counts.counts.merge(bestCenterIdx,
                        1,
                        (IgniteBiFunction<Integer, Integer, Integer>)(i1, i2) -> i1 + i2);

                    return counts;
                },
                key -> key.get2().equals(uid),
                SumsAndCounts::merge, new SumsAndCounts()
            );

            converged = true;

            for (Integer ind : stats.sums.keySet()) {
                Vector massCenter = stats.sums.get(ind).times(1.0 / stats.counts.get(ind));

                if (converged && distance(massCenter, centers[ind]) > epsilon * epsilon)
                    converged = false;

                centers[ind] = massCenter;
            }

            iteration++;
        }

        pointsCp.destroy();

        return new KMeansModel(centers, getDistanceMeasure());
    }

    /** Initialize cluster centers. */
    private Vector[] initClusterCenters(SparseDistributedMatrix points, int k) {
        // Initialize empty centers and point costs.
        int ptsCount = points.rowSize();

        // Initialize the first center to a random point.
        Vector sample = localCopyOf(points.viewRow(rnd.nextInt(ptsCount)));

        List<Vector> centers = new ArrayList<>();
        final List<Vector> newCenters = new ArrayList<>();
        newCenters.add(sample);
        centers.add(sample);

        final ConcurrentHashMap<Integer, Double> costs = new ConcurrentHashMap<>();

        // On each step, sample 2 * k points on average with probability proportional
        // to their squared distance from the centers. Note that only distances between points
        // and new centers are computed in each iteration.
        int step = 0;
        IgniteUuid uid = points.getUUID();

        while (step < initSteps) {
            // We assume here that costs can fit into memory of one node.
            ConcurrentHashMap<Integer, Double> newCosts = distributedFold(SparseDistributedMatrixStorage.ML_CACHE_NAME,
                (IgniteBiFunction<javax.cache.Cache.Entry<IgniteBiTuple<Integer, IgniteUuid>, ConcurrentHashMap<Integer, Double>>,
                    ConcurrentHashMap<Integer, Double>,
                    ConcurrentHashMap<Integer, Double>>)(vectorWithIndex, map) -> {
                    for (Vector center : newCenters)
                        map.merge(vectorWithIndex.getKey().get1(), distance(vectorWithIndex.getValue(), center), Functions.MIN);

                    return map;
                },
                key -> key.get2().equals(points.getUUID()),
                (map1, map2) -> {
                    map1.putAll(map2);
                    return map1;
                }, new ConcurrentHashMap<>());

            // Merge costs with new costs.
            for (Integer ind : newCosts.keySet())
                costs.merge(ind, newCosts.get(ind), Math::min);

            double sumCosts = costs.values().stream().mapToDouble(Double::valueOf).sum();

            newCenters.clear();
            newCenters.addAll(distributedFold(SparseDistributedMatrixStorage.ML_CACHE_NAME,
                (IgniteBiFunction<javax.cache.Cache.Entry<IgniteBiTuple<Integer, IgniteUuid>, Map<Integer, Double>>,
                    List<Vector>,
                    List<Vector>>)(vectorWithIndex, list) -> {
                    Integer ind = vectorWithIndex.getKey().get1();

                    double prob = costs.get(ind) * 2.0 * k / sumCosts;

                    if (new Random(seed ^ ind).nextDouble() < prob)
                        list.add(VectorUtils.fromMap(vectorWithIndex.getValue(), false));

                    return list;
                },
                key -> key.get2().equals(uid),
                (list1, list2) -> {
                    list1.addAll(list2);
                    return list1;
                },
                new ArrayList<>()
            ));

            centers.addAll(newCenters);
            step++;
        }

        List<Vector> distinctCenters = centers.stream().distinct().collect(Collectors.toList());

        if (distinctCenters.size() <= k)
            return distinctCenters.toArray(new Vector[] {});
        else {
            // Finally, we might have a set of more than k distinct candidate centers; weight each
            // candidate by the number of points in the dataset mapping to it and run a local k-means++
            // on the weighted centers to pick k of them
            ConcurrentHashMap<Integer, Integer> centerInd2Wight = distributedFold(SparseDistributedMatrixStorage.ML_CACHE_NAME,
                (IgniteBiFunction<javax.cache.Cache.Entry<IgniteBiTuple<Integer, IgniteUuid>, Map<Integer, Double>>,
                    ConcurrentHashMap<Integer, Integer>,
                    ConcurrentHashMap<Integer, Integer>>)(vectorWithIndex, countMap) -> {
                    Integer resInd = -1;
                    Double resDist = Double.POSITIVE_INFINITY;

                    int i = 0;
                    for (Vector cent : distinctCenters) {
                        double curDist = distance(vectorWithIndex.getValue(), cent);

                        if (resDist > curDist) {
                            resDist = curDist;
                            resInd = i;
                        }

                        i++;
                    }

                    countMap.compute(resInd, (ind, v) -> v != null ? v + 1 : 1);
                    return countMap;
                },
                key -> key.get2().equals(uid),
                (map1, map2) -> MapUtil.mergeMaps(map1, map2, (integer, integer2) -> integer2 + integer,
                    ConcurrentHashMap::new),
                new ConcurrentHashMap<>());

            List<Double> weights = new ArrayList<>(centerInd2Wight.size());

            for (int i = 0; i < distinctCenters.size(); i++)
                weights.add(i, Double.valueOf(centerInd2Wight.getOrDefault(i, 0)));

            DenseLocalOnHeapMatrix dCenters = MatrixUtil.fromList(distinctCenters, true);

            return new KMeansLocalClusterer(getDistanceMeasure(), 30, seed).
                cluster(dCenters, k, weights).centers();
        }
    }

    /** */
    private double distance(Map<Integer, Double> vecMap, Vector vector) {
        return distance(VectorUtils.fromMap(vecMap, false), vector);
    }
}
