package qupath.ext.qumap.engine;

import java.util.Arrays;

/**
 * Simple KD-tree for k-nearest-neighbor queries in marker space.
 * Used by UmapComputeService to project non-sampled cells efficiently.
 *
 * Replaces brute-force O(queries * samples) with O(queries * log(samples))
 * for typical marker dimensions (10-40).
 */
class KDTree {

    private static final int LEAF_SIZE = 32;

    private final double[][] points;
    private final int[] pointIndices;
    private final int dims;

    // Tree structure (implicit array-based)
    private final int[] splitDims;
    private final double[] splitValues;
    private final int[] leftChild;
    private final int[] rightChild;
    private final int[] leafStart;
    private final int[] leafEnd;
    private int nodeCount;

    /**
     * Build a KD-tree from the given point matrix.
     * Input must be NaN-free — NaN values corrupt distance computations silently.
     *
     * @param points row-major [numPoints][dims] matrix (no NaN values)
     */
    KDTree(double[][] points) {
        int n = points.length;
        this.points = points;
        this.dims = n > 0 ? points[0].length : 0;
        this.pointIndices = new int[n];
        for (int i = 0; i < n; i++) pointIndices[i] = i;

        // Pre-allocate tree arrays (at most 2*n/LEAF_SIZE nodes)
        int maxNodes = Math.max(4, 4 * n / LEAF_SIZE);
        splitDims = new int[maxNodes];
        splitValues = new double[maxNodes];
        leftChild = new int[maxNodes];
        rightChild = new int[maxNodes];
        leafStart = new int[maxNodes];
        leafEnd = new int[maxNodes];
        Arrays.fill(leftChild, -1);
        Arrays.fill(rightChild, -1);
        Arrays.fill(leafStart, -1);
        nodeCount = 0;

        if (n > 0) {
            buildNode(0, n);
        }
    }

    private int buildNode(int lo, int hi) {
        int nodeId = nodeCount++;
        if (hi - lo <= LEAF_SIZE) {
            // Leaf node
            leafStart[nodeId] = lo;
            leafEnd[nodeId] = hi;
            return nodeId;
        }

        // Find dimension with largest spread
        int bestDim = 0;
        double bestSpread = -1;
        for (int d = 0; d < dims; d++) {
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (int i = lo; i < hi; i++) {
                double v = points[pointIndices[i]][d];
                if (v < min) min = v;
                if (v > max) max = v;
            }
            double spread = max - min;
            if (spread > bestSpread) {
                bestSpread = spread;
                bestDim = d;
            }
        }

        // Partition around median using nth_element-style partitioning
        int mid = (lo + hi) / 2;
        nthElement(lo, hi, mid, bestDim);

        splitDims[nodeId] = bestDim;
        splitValues[nodeId] = points[pointIndices[mid]][bestDim];
        leafStart[nodeId] = -1; // not a leaf

        leftChild[nodeId] = buildNode(lo, mid);
        rightChild[nodeId] = buildNode(mid, hi);

        return nodeId;
    }

    /**
     * Partition pointIndices[lo..hi) so that element at `target` is in its sorted position
     * along dimension `dim`. QuickSelect algorithm.
     */
    private void nthElement(int lo, int hi, int target, int dim) {
        while (lo < hi - 1) {
            // Choose pivot (median of three)
            int mid = (lo + hi) / 2;
            double pivotVal = points[pointIndices[mid]][dim];

            swap(mid, hi - 1);
            int store = lo;
            for (int i = lo; i < hi - 1; i++) {
                if (points[pointIndices[i]][dim] < pivotVal) {
                    swap(i, store);
                    store++;
                }
            }
            swap(store, hi - 1);

            if (store == target) return;
            if (target < store) hi = store;
            else lo = store + 1;
        }
    }

    private void swap(int i, int j) {
        int tmp = pointIndices[i];
        pointIndices[i] = pointIndices[j];
        pointIndices[j] = tmp;
    }

    /**
     * Find the k nearest neighbors of the query point.
     *
     * @param query  the query vector [dims]
     * @param k      number of neighbors
     * @param outIndices output array for neighbor indices (into original points array)
     * @param outDists   output array for squared distances
     */
    void kNearest(double[] query, int k, int[] outIndices, double[] outDists) {
        // Max-heap of k best (distance, index) — stored as parallel arrays
        Arrays.fill(outDists, Double.MAX_VALUE);
        Arrays.fill(outIndices, -1);

        if (nodeCount > 0) {
            searchNode(0, query, k, outIndices, outDists);
        }
    }

    private void searchNode(int nodeId, double[] query, int k,
                            int[] bestIndices, double[] bestDists) {
        if (leafStart[nodeId] >= 0) {
            // Leaf node — check all points
            for (int i = leafStart[nodeId]; i < leafEnd[nodeId]; i++) {
                int pi = pointIndices[i];
                double dist = squaredDistance(query, points[pi]);
                insertIfCloser(pi, dist, k, bestIndices, bestDists);
            }
            return;
        }

        // Internal node
        int dim = splitDims[nodeId];
        double diff = query[dim] - splitValues[nodeId];

        int near = diff <= 0 ? leftChild[nodeId] : rightChild[nodeId];
        int far = diff <= 0 ? rightChild[nodeId] : leftChild[nodeId];

        // Search near side first
        if (near >= 0) searchNode(near, query, k, bestIndices, bestDists);

        // Only search far side if the splitting plane is closer than current worst
        double worstDist = 0;
        for (double d : bestDists) if (d > worstDist) worstDist = d;
        if (far >= 0 && diff * diff < worstDist) {
            searchNode(far, query, k, bestIndices, bestDists);
        }
    }

    private double squaredDistance(double[] a, double[] b) {
        double sum = 0;
        for (int d = 0; d < dims; d++) {
            double diff = a[d] - b[d];
            sum += diff * diff;
        }
        return sum;
    }

    /**
     * Insert into a max-heap of size k if the new distance is smaller than the worst.
     */
    private void insertIfCloser(int index, double dist, int k,
                                int[] bestIndices, double[] bestDists) {
        // Find the worst (maximum distance) in the heap
        int worstPos = 0;
        for (int i = 1; i < k; i++) {
            if (bestDists[i] > bestDists[worstPos]) worstPos = i;
        }
        if (dist < bestDists[worstPos]) {
            bestDists[worstPos] = dist;
            bestIndices[worstPos] = index;
        }
    }
}
