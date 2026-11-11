package org.example.api.utils;

/**
 * Utility class for building DASH manifest XML strings.
 * Provides methods for XML escaping, formatting, and construction.
 */
public class ManifestXmlBuilder {

    /**
     * Escapes special XML characters in text content.
     *
     * @param text The text to escape
     * @return XML-safe escaped text
     */
    public static String escapeXml(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Formats duration in seconds to ISO 8601 duration format (PT#H#M#S).
     *
     * @param durationSeconds Duration in seconds
     * @return ISO 8601 formatted duration string (e.g., "PT1M59.702S")
     */
    public static String formatDuration(long durationSeconds) {
        if (durationSeconds <= 0) {
            return "PT0S";
        }

        long hours = durationSeconds / 3600;
        long minutes = (durationSeconds % 3600) / 60;
        long seconds = durationSeconds % 60;

        StringBuilder duration = new StringBuilder("PT");

        if (hours > 0) {
            duration.append(hours).append("H");
        }
        if (minutes > 0) {
            duration.append(minutes).append("M");
        }
        if (seconds > 0 || (hours == 0 && minutes == 0)) {
            duration.append(seconds).append("S");
        }

        return duration.toString();
    }

    /**
     * Formats duration with millisecond precision.
     *
     * @param durationSeconds Duration in seconds (can be decimal)
     * @return ISO 8601 formatted duration string with milliseconds if needed
     */
    public static String formatDurationWithMillis(double durationSeconds) {
        if (durationSeconds <= 0) {
            return "PT0S";
        }

        long hours = (long) (durationSeconds / 3600);
        long minutes = (long) ((durationSeconds % 3600) / 60);
        double seconds = durationSeconds % 60;

        StringBuilder duration = new StringBuilder("PT");

        if (hours > 0) {
            duration.append(hours).append("H");
        }
        if (minutes > 0) {
            duration.append(minutes).append("M");
        }
        if (seconds > 0 || (hours == 0 && minutes == 0)) {
            if (seconds == (long) seconds) {
                duration.append((long) seconds).append("S");
            } else {
                duration.append(String.format("%.3f", seconds)).append("S");
            }
        }

        return duration.toString();
    }

    /**
     * Normalizes language code by converting underscores to hyphens.
     *
     * @param languageCode The language code to normalize (e.g., "en_US")
     * @return Normalized language code (e.g., "en-US")
     */
    public static String normalizeLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return "und";
        }
        return languageCode.replace("_", "-");
    }

    /**
     * Gets a human-readable language name for a language code.
     *
     * @param languageCode ISO language code (e.g., "en", "es")
     * @return Display name for the language
     */
    public static String getLanguageName(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return "Unknown";
        }

        // Extract base language code (remove region if present)
        String baseCode = languageCode.split("[-_]")[0].toLowerCase();

        return switch (baseCode) {
            case "en" -> "English";
            case "es" -> "Spanish";
            case "fr" -> "French";
            case "de" -> "German";
            case "it" -> "Italian";
            case "pt" -> "Portuguese";
            case "ru" -> "Russian";
            case "ja" -> "Japanese";
            case "ko" -> "Korean";
            case "zh" -> "Chinese";
            case "ar" -> "Arabic";
            case "hi" -> "Hindi";
            case "nl" -> "Dutch";
            case "pl" -> "Polish";
            case "tr" -> "Turkish";
            case "sv" -> "Swedish";
            case "no" -> "Norwegian";
            case "da" -> "Danish";
            case "fi" -> "Finnish";
            case "und" -> "Unknown";
            default -> baseCode.toUpperCase();
        };
    }

    /**
     * Adds proper indentation to XML content.
     *
     * @param level Indentation level (0 = no indent)
     * @return Indentation string
     */
    public static String indent(int level) {
        return "  ".repeat(Math.max(0, level));
    }
}
