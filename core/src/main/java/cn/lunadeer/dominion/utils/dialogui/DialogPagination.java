package cn.lunadeer.dominion.utils.dialogui;

public record DialogPagination(int page, int pages, int from, int to) {
    public static DialogPagination of(int requested, int total, int pageSize) {
        int size = Math.max(1, pageSize);
        int pages = Math.max(1, (int) Math.ceil(total / (double) size));
        int page = Math.max(1, Math.min(requested, pages));
        int from = Math.min(total, (page - 1) * size);
        return new DialogPagination(page, pages, from, Math.min(total, from + size));
    }
}
