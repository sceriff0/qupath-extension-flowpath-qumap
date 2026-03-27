package qupath.ext.qumap.model;

public record UmapParameters(int k, double minDist, double spread, int epochs) {

    public static UmapParameters defaults() {
        return new UmapParameters(15, 0.1, 1.0, 200);
    }
}
