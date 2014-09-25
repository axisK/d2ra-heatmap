package net.d2ra.heatmap;

import javax.imageio.ImageIO;
import javax.vecmath.Vector2f;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A square heatmap of a given size.
 */
public class Heatmap {

    private float stepSize;
    private Color[] colors;
    private int size;
    private float[][] data;
    private int heatmapAlpha;

    private Heatmap(final int size,
                    final Color[] colors,
                    final float stepSize,
                    final int heatmapAlpha) {

        this.size = size;
        this.colors = colors;
        this.stepSize = stepSize;
        this.heatmapAlpha = heatmapAlpha;
    }

    private void initData(Vector2f[] dataPoints, final float scoreMultiplier) {
        data = new float[size][size];
        double radius = scoreMultiplier * Math.floor(Math.sqrt(size));
        double maxScore = 0;

        for (Vector2f dataPoint : dataPoints) {
            // base cell
            int cellX = (int) Math.floor(dataPoint.x * size),
                cellY = (int) Math.floor(dataPoint.y * size);

            // scoring mechanism
            for (int radiusDiff = 0; radiusDiff < radius; radiusDiff++) {
                // process a square around the base point
                for (int currentX = Math.max(0, cellX - radiusDiff); currentX <= Math.min(size, cellX + radiusDiff); currentX++) {
                    for (int currentY = Math.max(0, cellY - radiusDiff); currentY <= Math.min(size, cellY + radiusDiff); currentY++) {
                        double hypotenuse = Math.sqrt(Math.pow(currentX - cellX, 2) + Math.pow(currentY - cellY, 2));
                        // discard points that DO NOT fall within the circle
                        if (hypotenuse <= radius) {
                            data[currentX][currentY] += (radius - hypotenuse);
                            maxScore = Math.max(maxScore, data[currentX][currentY]);
                        }
                    }
                }
            }
        }

        // normalize the data
        for (int cellX = 0; cellX < size; cellX++) {
            for (int cellY = 0; cellY < size; cellY++) {
                data[cellX][cellY] = (float) (Math.max(0, data[cellX][cellY]) / maxScore);
            }
        }
    }

    public Image toImage(final int width, final int height) {
        return toImage((float) width, (float) height);
    }

    public Image toImage(final float width, final float height) {
        BufferedImage buffer = new BufferedImage((int)width, (int)height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = (Graphics2D) buffer.getGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        try {
            BufferedImage background = ImageIO.read(getClass().getClassLoader().getResourceAsStream("dota_minimap.jpg"));
            graphics.drawImage(background, 0, 0, (int) width, (int) height, null);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not load background");
        }

        for (int cellX = 0; cellX < size; cellX++) {
            for (int cellY = 0; cellY < size; cellY++) {
                float score = data[cellX][cellY];
                int colorIndex = 0;
                for (float stepStart = 0f; stepStart < 1f; stepStart += stepSize, colorIndex++) {
                    if (stepStart <= score && score <= stepStart + stepSize) {
                        float split = (score - stepStart)/stepSize;
                        Color from = colors[colorIndex],
                                to = colors[colorIndex+1];

                        graphics.setColor(new Color(
                                (int) (from.getRed() * (1 - split) + to.getRed() * split),
                                (int) (from.getGreen() * (1 - split) + to.getGreen() * split),
                                (int) (from.getBlue() * (1 - split) + to.getBlue() * split),
                                heatmapAlpha));

                        double startX = Math.floor(((float)cellX)/size * width);
                        double endX = Math.floor(((float)cellX + 1)/size * width);
                        double startY = Math.floor(((float)cellY)/size * height);
                        double endY = Math.floor(((float)cellY + 1)/size * height);

                        graphics.fill(new Rectangle2D.Double(startX, startY, endX-startX, endY-startY));
                        continue;
                    }
                }
            }
        }

        return buffer;
    }

    public static class Builder {
        private int size = 128;
        private List<Color> colors = Arrays.asList(
                Color.BLACK,
                Color.BLUE,
                Color.GREEN,
                Color.YELLOW,
                Color.RED);
        private int alpha;
        private List<Vector2f> dataPoints = new ArrayList<>();
        private float scoreMultiplier;

        public Builder withSize(final int size) {
            this.size = size;
            return this;
        }

        public Builder withColors(final List<Color> colors) {
            this.colors = new ArrayList<>(colors);
            return this;
        }

        public Builder withColors(final Color[] colors) {
            this.colors = Arrays.asList(colors);
            return this;
        }

        public Builder addColor(final Color color) {
            return addColor(color, false);
        }

        public Builder addColor(final Color color, final boolean firstColor) {
            if (firstColor) {
                colors.retainAll(Collections.emptyList());
            }
            colors.add(color);
            return this;
        }

        public Builder withAlpha(final int alpha) {
            this.alpha = alpha;
            return this;
        }

        public Builder withScoreMultiplier(final float scoreMultiplier) {
            this.scoreMultiplier = scoreMultiplier;
            return this;
        }

        public Builder addDataPoint(Vector2f dataPoint) {
            dataPoints.add(dataPoint);
            return this;
        }

        public Heatmap build() {
            Heatmap heatmap = new Heatmap(size, colors.toArray(new Color[]{}), 1f/(colors.size()-1), alpha);
            heatmap.initData(dataPoints.toArray(new Vector2f[]{}), scoreMultiplier);
            return heatmap;
        }
    }

}
