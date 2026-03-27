package qupath.ext.qumap.model;

import qupath.lib.objects.PathObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

/**
 * Immutable result of a UMAP computation.
 * Stores 2D embedding coordinates with back-references to PathObjects.
 */
public class UmapResult {

    private final double[] umapX;
    private final double[] umapY;
    private final PathObject[] objects;
    private final String[] markerNames;
    private final UmapParameters params;

    public UmapResult(double[] umapX, double[] umapY, PathObject[] objects,
                      String[] markerNames, UmapParameters params) {
        this.umapX = umapX;
        this.umapY = umapY;
        this.objects = objects;
        this.markerNames = markerNames;
        this.params = params;
    }

    public double[] getUmapX() { return umapX; }
    public double[] getUmapY() { return umapY; }
    public PathObject[] getObjects() { return objects; }
    public String[] getMarkerNames() { return markerNames; }
    public UmapParameters getParams() { return params; }
    public int size() { return umapX.length; }

    /**
     * Export UMAP coordinates and phenotype labels to CSV.
     */
    public void exportToCsv(File file) throws IOException {
        try (var writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("UMAP_X,UMAP_Y,Phenotype");
            writer.newLine();
            for (int i = 0; i < umapX.length; i++) {
                var pc = objects[i].getPathClass();
                String label = pc != null ? pc.getName() : "Unclassified";
                writer.write(String.format(Locale.US, "%.6f,%.6f,%s", umapX[i], umapY[i], label));
                writer.newLine();
            }
        }
    }
}
