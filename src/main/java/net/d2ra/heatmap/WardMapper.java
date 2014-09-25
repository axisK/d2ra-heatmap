package net.d2ra.heatmap;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import net.d2ra.heatmap.Heatmap.Builder;
import skadistats.clarity.Clarity;
import skadistats.clarity.match.Match;
import skadistats.clarity.model.Entity;
import skadistats.clarity.parser.Profile;

import javax.imageio.ImageIO;
import javax.vecmath.Vector2f;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Utility to generate a heatmap based on all placed wards.
 */
public class WardMapper {

    public static void main(final String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Please provide a replay file AND an ouput file.");
            System.exit(1);
        }
        Builder allWardsHeatmapBuilder = new Builder()
                .withSize(512)
                .withAlpha(0x80)
                .withScoreMultiplier(1.4f);
        Set<Integer> processedEntities = new HashSet<>();

        Predicate<Entity> isNotNull = entity -> entity != null,
                notProcessedBefore = entity -> !processedEntities.contains(entity.getIndex()),
                isObserverWard = entity -> entity.getDtClass().getDtName().equals("DT_DOTA_NPC_Observer_Ward"),
                isSentryWard = entity -> entity.getDtClass().getDtName().equals("DT_DOTA_NPC_Observer_Ward_TrueSight");

        Match match = new Match();
        Clarity.tickIteratorForFile(args[0], Profile.ENTITIES)
            .forEachRemaining(tick -> {
                tick.apply(match);

                Lists.newArrayList(match.getEntities().getAllByPredicate(e -> true))
                        .parallelStream()
                        .filter(isNotNull.and(isObserverWard.or(isSentryWard)).and(notProcessedBefore))
                        .forEach(ward -> {
                            processedEntities.add(ward.getIndex());
                            allWardsHeatmapBuilder.addDataPoint(getCoordinates(ward));
                        });
            });

        ImageIO.write((RenderedImage) allWardsHeatmapBuilder.build().toImage(800, 800), "PNG", new File(args[1]));
    }

    private static Vector2f getCoordinates(final Entity entity) {
        Vector2f origin = entity.<Vector2f>getProperty("m_vecOrigin");
        int cellWidth = 1 << entity.<Integer>getProperty("m_cellbits");
        float x =((entity.<Integer>getProperty("m_cellX") * cellWidth - 16384 + (origin.x) / 128) + (16384/2))/16384;
        float y = ((entity.<Integer>getProperty("m_cellY") * cellWidth - 16384 + (origin.y) / 128) + (16384/2))/16384;
        return new Vector2f(x, 1 - y);
    }
}
