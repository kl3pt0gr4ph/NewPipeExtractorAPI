package org.example.api.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.example.api.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for ValidationUtils.
 */
@DisplayName("ValidationUtils Tests")
class ValidationUtilsTest {

    // ── requireNonEmpty ──────────────────────────────────────────────────

    @ParameterizedTest(name = "Accepts \"{0}\"")
    @ValueSource(strings = {"valid value", "  content  ", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    @DisplayName("Passes for non-blank values, including ones with surrounding whitespace or very long content")
    void requireNonEmpty_acceptsNonBlankValues(String value) {
        assertThatCode(() -> ValidationUtils.requireNonEmpty(value, "testField"))
                .doesNotThrowAnyException();
    }

    static Stream<Arguments> blankValues() {
        return Stream.of(
                Arguments.of("null", (String) null),
                Arguments.of("empty string", ""),
                Arguments.of("whitespace only", "   "),
                Arguments.of("mixed whitespace (tabs/newlines)", " \t\n\r "));
    }

    @ParameterizedTest(name = "Rejects {0}")
    @MethodSource("blankValues")
    @DisplayName("Rejects null/blank values with a 400 ValidationException naming the field")
    void requireNonEmpty_rejectsBlankValues(String name, String value) {
        assertThatThrownBy(() -> ValidationUtils.requireNonEmpty(value, "testField"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(ValidationException.class))
                .satisfies(ex -> {
                    assertThat(ex.getMessage()).contains("testField", "must not be empty");
                    assertThat(ex.getHttpStatus()).isEqualTo(400);
                    assertThat(ex.getErrorCode()).isEqualTo("VALIDATION_ERROR");
                });
    }

    @Test
    @DisplayName("Includes the exact field name passed in, unsanitized, in the error message")
    void requireNonEmpty_includesFieldNameVerbatim() {
        assertThatThrownBy(() -> ValidationUtils.requireNonEmpty(null, "field_with-special.chars"))
                .hasMessageContaining("field_with-special.chars");
    }

    // ── requireValidUrl ──────────────────────────────────────────────────

    @ParameterizedTest(name = "Accepts {0}")
    @ValueSource(
            strings = {
                "http://example.com",
                "https://example.com",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "https://example.com/path?param1=value1&param2=value2",
                "https://example.com/path#section",
                "https://example.com:8080/path",
                "https://example.com/path?query=hello%20world"
            })
    @DisplayName("Passes for http(s) URLs with query params, fragments, ports, or encoded characters")
    void requireValidUrl_acceptsWellFormedHttpUrls(String url) {
        assertThatCode(() -> ValidationUtils.requireValidUrl(url)).doesNotThrowAnyException();
    }

    static Stream<Arguments> invalidUrls() {
        return Stream.of(
                Arguments.of("null", null, "must not be empty"),
                Arguments.of("empty", "", "must not be empty"),
                Arguments.of("no protocol", "example.com", "http"),
                Arguments.of("wrong protocol (ftp)", "ftp://example.com", "http"));
    }

    @ParameterizedTest(name = "Rejects {0}")
    @MethodSource("invalidUrls")
    @DisplayName("Rejects null/blank URLs and non-http(s) protocols, each with a message naming the actual problem")
    void requireValidUrl_rejectsInvalidUrls(String name, String url, String expectedMessageFragment) {
        assertThatThrownBy(() -> ValidationUtils.requireValidUrl(url))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(expectedMessageFragment);
    }

    // ── requireValidServiceId ────────────────────────────────────────────

    @ParameterizedTest(name = "Accepts serviceId={0}")
    @ValueSource(ints = {0, 1})
    @DisplayName("Passes for zero and positive service IDs")
    void requireValidServiceId_acceptsNonNegative(int serviceId) {
        assertThatCode(() -> ValidationUtils.requireValidServiceId(serviceId)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "Rejects serviceId={0}")
    @ValueSource(ints = {-1, -100})
    @DisplayName("Rejects negative service IDs, naming the field and requirement in the message")
    void requireValidServiceId_rejectsNegative(int serviceId) {
        assertThatThrownBy(() -> ValidationUtils.requireValidServiceId(serviceId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("serviceId")
                .hasMessageContaining("non-negative");
    }
}
