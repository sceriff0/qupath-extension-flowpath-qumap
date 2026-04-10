package qupath.ext.qumap.ui;

import javafx.event.EventHandler;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interactive polygon drawing controller for UmapCanvas.
 * Click to add vertices, double-click to close and complete the polygon.
 * After completion, drag handles to reposition vertices.
 */
public class PolygonSelector {

    private static final double HANDLE_HIT_RADIUS = 8.0;

    private final UmapCanvas canvas;
    private final List<double[]> vertices = new ArrayList<>();
    private boolean active = false;
    private boolean completed = false;
    private int dragHandleIndex = -1;
    private Consumer<List<double[]>> onPolygonComplete;

    private final EventHandler<MouseEvent> pressHandler = this::handlePressed;
    private final EventHandler<MouseEvent> dragHandler = this::handleDragged;
    private final EventHandler<MouseEvent> releaseHandler = this::handleReleased;

    public PolygonSelector(UmapCanvas canvas) {
        this.canvas = canvas;
    }

    public void setOnPolygonComplete(Consumer<List<double[]>> cb) {
        this.onPolygonComplete = cb;
    }

    public void activate() {
        // Remove first to prevent double-registration if activate() is called while already active
        canvas.removeEventHandler(MouseEvent.MOUSE_PRESSED, pressHandler);
        canvas.removeEventHandler(MouseEvent.MOUSE_DRAGGED, dragHandler);
        canvas.removeEventHandler(MouseEvent.MOUSE_RELEASED, releaseHandler);

        active = true;
        completed = false;
        dragHandleIndex = -1;
        vertices.clear();
        canvas.setPolygonOverlay(null);
        canvas.setPolygonCompleted(false);

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, pressHandler);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, dragHandler);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, releaseHandler);
    }

    public void deactivate() {
        active = false;
        completed = false;
        dragHandleIndex = -1;

        canvas.removeEventHandler(MouseEvent.MOUSE_PRESSED, pressHandler);
        canvas.removeEventHandler(MouseEvent.MOUSE_DRAGGED, dragHandler);
        canvas.removeEventHandler(MouseEvent.MOUSE_RELEASED, releaseHandler);
    }

    public boolean isActive() { return active; }
    public boolean isCompleted() { return completed; }

    public void clear() {
        vertices.clear();
        completed = false;
        dragHandleIndex = -1;
        canvas.clearPolygonOverlay();
    }

    private void handlePressed(MouseEvent e) {
        if (!active || e.getButton() != MouseButton.PRIMARY) return;

        double sx = e.getX();
        double sy = e.getY();

        if (completed) {
            // Try to start handle drag
            int handle = findHandle(sx, sy);
            if (handle >= 0) {
                dragHandleIndex = handle;
                e.consume();
            }
            return;
        }

        // Drawing mode
        if (e.getClickCount() == 2) {
            // Remove the spurious vertex added by the preceding single-click event
            if (!vertices.isEmpty()) vertices.remove(vertices.size() - 1);
            // Complete polygon
            if (vertices.size() >= 3 && onPolygonComplete != null) {
                completed = true;
                canvas.setPolygonCompleted(true);
                canvas.setPolygonOverlay(new ArrayList<>(vertices));
                onPolygonComplete.accept(new ArrayList<>(vertices));
            }
            e.consume();
        } else if (e.getClickCount() == 1) {
            double dx = canvas.screenXToDataX(sx);
            double dy = canvas.screenYToDataY(sy);
            vertices.add(new double[]{dx, dy});
            canvas.setPolygonOverlay(new ArrayList<>(vertices));
            e.consume();
        }
    }

    private void handleDragged(MouseEvent e) {
        if (!active || !completed || dragHandleIndex < 0) return;
        if (e.getButton() != MouseButton.PRIMARY) return;

        double dx = canvas.screenXToDataX(e.getX());
        double dy = canvas.screenYToDataY(e.getY());
        vertices.get(dragHandleIndex)[0] = dx;
        vertices.get(dragHandleIndex)[1] = dy;
        canvas.setPolygonOverlay(new ArrayList<>(vertices));
        e.consume();
    }

    private void handleReleased(MouseEvent e) {
        if (!active || !completed || dragHandleIndex < 0) return;

        dragHandleIndex = -1;
        // Fire callback to recompute inside/outside mask
        if (onPolygonComplete != null && vertices.size() >= 3) {
            onPolygonComplete.accept(new ArrayList<>(vertices));
        }
        e.consume();
    }

    private int findHandle(double screenX, double screenY) {
        for (int i = 0; i < vertices.size(); i++) {
            double hx = canvas.dataXToScreenX(vertices.get(i)[0]);
            double hy = canvas.dataYToScreenY(vertices.get(i)[1]);
            if (Math.hypot(screenX - hx, screenY - hy) <= HANDLE_HIT_RADIUS) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Compute a boolean mask: true = inside the polygon.
     */
    public boolean[] computeInsideMask(double[] umapX, double[] umapY) {
        int n = umapX.length;
        boolean[] mask = new boolean[n];
        if (vertices.size() < 3) return mask;

        for (int i = 0; i < n; i++) {
            mask[i] = UmapCanvas.pointInPolygon(umapX[i], umapY[i], vertices);
        }
        return mask;
    }

    public List<double[]> getVertices() { return vertices; }
}
