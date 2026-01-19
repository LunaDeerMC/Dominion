package cn.lunadeer.dominion.utils.highlighter;

/**
 * Utility class for highlighter implementations.
 * Contains shared constants and methods used by both ParticleHighlighter and BlockDisplayHighlighter.
 */
public final class HighlighterUtil {

    /**
     * Maximum render radius for highlighting.
     * Blocks/particles beyond this distance from the player won't be rendered.
     */
    public static final int RENDER_MAX_RADIUS = 48;

    private HighlighterUtil() {}

    /**
     * Adjusts boundary coordinates based on player's view range.
     * This method clips the boundary to the player's visible range to avoid
     * rendering blocks/particles that are too far away.
     *
     * @param playerMin   The minimum coordinate of the player's view range
     * @param playerMax   The maximum coordinate of the player's view range
     * @param boundaryMin The minimum coordinate of the boundary to adjust
     * @param boundaryMax The maximum coordinate of the boundary to adjust
     * @return An array of two integers: [adjustedMin, adjustedMax]
     */
    public static int[] adjustBoundary(int playerMin, int playerMax, int boundaryMin, int boundaryMax) {
        if (playerMax <= boundaryMin) {
            boundaryMin = boundaryMax;
        } else if (playerMax <= boundaryMax) {
            boundaryMax = playerMax;
            if (playerMin >= boundaryMin) {
                boundaryMin = playerMin;
            }
        } else {
            if (playerMin > boundaryMin) {
                boundaryMin = playerMin;
            } else if (playerMin > boundaryMax) {
                boundaryMin = boundaryMax;
            }
        }
        return new int[]{boundaryMin, boundaryMax};
    }
}