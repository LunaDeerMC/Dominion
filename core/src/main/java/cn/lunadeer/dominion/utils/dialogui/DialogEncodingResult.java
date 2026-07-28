package cn.lunadeer.dominion.utils.dialogui;

/**
 * Result returned by a version-specific dialog backend.
 */
public record DialogEncodingResult(boolean successful, String message) {
    public DialogEncodingResult {
        message = message == null ? "" : message;
    }

    public static DialogEncodingResult success() {
        return new DialogEncodingResult(true, "");
    }

    public static DialogEncodingResult unsupported(String message) {
        return new DialogEncodingResult(false, message);
    }
}
