package cn.lunadeer.dominion.utils.chestui;

/** Pure pagination calculations shared by all list menus. */
public record Pagination(int page, int pages, int from, int to) {
    public static Pagination of(int requestedPage, int total, int pageSize) {
        int safeSize = Math.max(1, pageSize);
        int safeTotal = Math.max(0, total);
        int pages = Math.max(1, (safeTotal + safeSize - 1) / safeSize);
        int page = Math.max(1, Math.min(requestedPage, pages));
        int from = Math.min(safeTotal, (page - 1) * safeSize);
        int to = Math.min(safeTotal, from + safeSize);
        return new Pagination(page, pages, from, to);
    }
}
