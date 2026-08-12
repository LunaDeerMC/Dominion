package cn.lunadeer.dominion.uis.dialog.components;

/**
 * Style values for a list page.
 *
 * <p>{@link #DEFAULT} is the only intentionally shared page style. A list
 * page can derive its own style with one of the {@code with...} methods when
 * its entries or navigation need different widths.</p>
 */
public record DialogListStyle(
        DialogPageLayout pageLayout,
        DialogPageLayout searchLayout,
        int pageSize,
        int navigationButtonWidth,
        int searchButtonWidth,
        int itemWidth,
        int createItemWidth,
        int compactItemWidth,
        int specialItemWidth
) {
    public static final DialogListStyle DEFAULT = new DialogListStyle(
            DialogPageLayout.of(3), DialogPageLayout.of(2), 12, 24, 48, 164, 94, 104, 120);

    public DialogListStyle {
        if (pageLayout == null) throw new NullPointerException("pageLayout");
        if (searchLayout == null) throw new NullPointerException("searchLayout");
        if (pageSize <= 0) throw new IllegalArgumentException("pageSize must be positive");
        if (navigationButtonWidth <= 0) {
            throw new IllegalArgumentException("navigationButtonWidth must be positive");
        }
        if (searchButtonWidth <= 0) {
            throw new IllegalArgumentException("searchButtonWidth must be positive");
        }
        if (itemWidth <= 0 || createItemWidth <= 0 || compactItemWidth <= 0 || specialItemWidth <= 0) {
            throw new IllegalArgumentException("list item widths must be positive");
        }
    }

    public DialogListStyle withPageLayout(DialogPageLayout value) {
        return new DialogListStyle(value, searchLayout, pageSize, navigationButtonWidth, searchButtonWidth,
                itemWidth, createItemWidth, compactItemWidth, specialItemWidth);
    }

    public DialogListStyle withSearchLayout(DialogPageLayout value) {
        return new DialogListStyle(pageLayout, value, pageSize, navigationButtonWidth,
                searchButtonWidth, itemWidth, createItemWidth, compactItemWidth, specialItemWidth);
    }

    public DialogListStyle withPageSize(int value) {
        return new DialogListStyle(pageLayout, searchLayout, value, navigationButtonWidth, searchButtonWidth,
                itemWidth, createItemWidth, compactItemWidth, specialItemWidth);
    }

    public DialogListStyle withSearchButtonWidth(int value) {
        return new DialogListStyle(pageLayout, searchLayout, pageSize, navigationButtonWidth, value,
                itemWidth, createItemWidth, compactItemWidth, specialItemWidth);
    }

    /** Returns the page style for a list that reserves one slot for creation. */
    public DialogListStyle withCreateButton() {
        return withPageSize(Math.max(1, pageSize - 1));
    }

    public DialogListStyle withItemWidth(int value) {
        return new DialogListStyle(pageLayout, searchLayout, pageSize, navigationButtonWidth, searchButtonWidth,
                value, createItemWidth, compactItemWidth, specialItemWidth);
    }

    public DialogListStyle withCreateItemWidth(int value) {
        return new DialogListStyle(pageLayout, searchLayout, pageSize, navigationButtonWidth, searchButtonWidth,
                itemWidth, value, compactItemWidth, specialItemWidth);
    }

    public DialogListStyle withCompactItemWidth(int value) {
        return new DialogListStyle(pageLayout, searchLayout, pageSize, navigationButtonWidth, searchButtonWidth,
                itemWidth, createItemWidth, value, specialItemWidth);
    }

    public DialogListStyle withSpecialItemWidth(int value) {
        return new DialogListStyle(pageLayout, searchLayout, pageSize, navigationButtonWidth, searchButtonWidth,
                itemWidth, createItemWidth, compactItemWidth, value);
    }
}
