package cn.lunadeer.dominion.uis.dialog.components;

/**
 * Layout values owned by one native dialog page.
 *
 * <p>Pages deliberately keep their layout instance next to their rendering
 * code. This keeps a change to one page from silently changing another page.
 * Lists use {@link DialogListStyle}, which can derive its page layout from a
 * shared default and override only the values that need to differ.</p>
 */
public record DialogPageLayout(int contentWidth, int columns,
                               int buttonWidth, int wideButtonWidth) {
    public DialogPageLayout {
        if (contentWidth <= 0) throw new IllegalArgumentException("contentWidth must be positive");
        if (columns <= 0) throw new IllegalArgumentException("columns must be positive");
        if (buttonWidth <= 0) throw new IllegalArgumentException("buttonWidth must be positive");
        if (wideButtonWidth <= 0) {
            throw new IllegalArgumentException("wideButtonWidth must be positive");
        }
    }

    public static DialogPageLayout of(int columns) {
        return switch (columns) {
            case 2 -> new DialogPageLayout(336, 2, 164, 336);
            case 3 -> new DialogPageLayout(336, 3, 108, 336);
            default -> new DialogPageLayout(336, columns, 108, 336);
        };
    }
}
