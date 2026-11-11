package org.example.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for ManifestXmlBuilder.
 */
@DisplayName("ManifestXmlBuilder Tests")
class ManifestXmlBuilderTest {

    // ── escapeXml ────────────────────────────────────────────────────────

    @ParameterizedTest(name = "escapeXml({0}) = {1}")
    @CsvSource(
            value = {
                "AT&T,                              AT&amp;T",
                "'5 < 10',                          '5 &lt; 10'",
                "'10 > 5',                          '10 &gt; 5'",
                "'Say \"Hello\"',                   'Say &quot;Hello&quot;'",
                "It's working,                      It&apos;s working",
                "Hello World 123,                   Hello World 123",
            },
            delimiterString = ",")
    @DisplayName("Escapes each special character individually, and leaves safe text unmodified")
    void escapesIndividualCharacters(String input, String expected) {
        assertThat(ManifestXmlBuilder.escapeXml(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Returns an empty string for null or empty input")
    void escapeXml_handlesNullAndEmpty(String input) {
        assertThat(ManifestXmlBuilder.escapeXml(input)).isEqualTo("");
    }

    @ParameterizedTest(name = "escapeXml order-correctness")
    @ValueSource(strings = {"<tag attr=\"value\">Text & more</tag>"})
    @DisplayName("Escapes ampersand FIRST so replacement entities (&amp;, &lt;, ...) are not themselves re-escaped")
    void escapeXml_multipleCharsInCorrectOrder(String input) {
        assertThat(ManifestXmlBuilder.escapeXml(input))
                .isEqualTo("&lt;tag attr=&quot;value&quot;&gt;Text &amp; more&lt;/tag&gt;");
    }

    // ── formatDuration ───────────────────────────────────────────────────

    @ParameterizedTest(name = "{0}s -> {1}")
    @CsvSource({
        "0,    PT0S", // zero
        "-100, PT0S", // negative -> same as zero (the <= 0 guard, not just == 0)
        "45,   PT45S", // seconds only
        "125,  PT2M5S", // minutes + seconds
        "7385, PT2H3M5S", // hours + minutes + seconds
        "3600, PT1H", // exact hour (no trailing 0M0S)
        "300,  PT5M", // exact minute
        "5400, PT1H30M", // hour + minutes, no seconds
        "3605, PT1H5S", // hour + seconds, no minutes
    })
    @DisplayName("Formats every hours/minutes/seconds presence combination to ISO 8601")
    void formatsDurationPerComponentCombination(long durationSeconds, String expected) {
        assertThat(ManifestXmlBuilder.formatDuration(durationSeconds)).isEqualTo(expected);
    }

    // ── formatDurationWithMillis ─────────────────────────────────────────

    @ParameterizedTest(name = "{0}s -> {1}")
    @CsvSource({
        "119.702, PT1M59.702S", // decimal seconds
        "120.0,   PT2M", // whole seconds -> no decimal point
        "45.123,  PT45.123S", // millisecond precision
        "1.001,   PT1.001S", // small decimal
    })
    @DisplayName("Formats decimal-second durations, omitting the decimal point when seconds are whole")
    void formatsDurationWithMillisPrecision(double durationSeconds, String expected) {
        assertThat(ManifestXmlBuilder.formatDurationWithMillis(durationSeconds)).isEqualTo(expected);
    }

    // ── normalizeLanguageCode ────────────────────────────────────────────

    @ParameterizedTest(name = "normalizeLanguageCode({0}) = {1}")
    @CsvSource({
        "en_US,      en-US", // underscore -> hyphen
        "en-US,      en-US", // hyphen passes through unchanged
        "en,         en", // simple code, no separator
        "zh_Hans_CN, zh-Hans-CN" // multiple underscores all converted
    })
    @DisplayName("Converts underscores to hyphens, leaving already-hyphenated or simple codes unchanged")
    void normalizesLanguageCodeSeparators(String input, String expected) {
        assertThat(ManifestXmlBuilder.normalizeLanguageCode(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Defaults null or empty language codes to 'und'")
    void normalizeLanguageCode_defaultsNullAndEmptyToUnd(String input) {
        assertThat(ManifestXmlBuilder.normalizeLanguageCode(input)).isEqualTo("und");
    }

    // ── getLanguageName ──────────────────────────────────────────────────

    @ParameterizedTest(name = "getLanguageName({0}) = {1}")
    @CsvSource({
        "en,    English", // known code
        "es,    Spanish",
        "fr,    French",
        "de,    German",
        "ja,    Japanese",
        "zh,    Chinese",
        "EN,    English", // case-insensitive
        "ES,    Spanish",
        "en-US, English", // region suffix stripped before lookup
        "es-MX, Spanish",
        "und,   Unknown", // explicit undefined-language code
        "xy,    XY", // unrecognised code falls back to uppercased input
    })
    @DisplayName("Maps known codes (case-insensitively, region stripped) to display names, with und/unknown fallbacks")
    void mapsLanguageCodesToDisplayNames(String input, String expected) {
        assertThat(ManifestXmlBuilder.getLanguageName(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Returns 'Unknown' for null or empty language codes")
    void getLanguageName_returnsUnknownForNullAndEmpty(String input) {
        assertThat(ManifestXmlBuilder.getLanguageName(input)).isEqualTo("Unknown");
    }

    // ── indent ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "indent({0}) has length {1}")
    @CsvSource({
        "-1, 0", // negative clamped to zero (the only real branch here)
        "0,  0", "2,  4",
    })
    @DisplayName("Returns two spaces per level, clamping negative levels to zero indentation")
    void indentsTwoSpacesPerLevelClampingNegative(int level, int expectedLength) {
        assertThat(ManifestXmlBuilder.indent(level)).hasSize(expectedLength);
    }
}
