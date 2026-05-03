package by.baykulbackend.database.dto.user;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Custom deserializer that gives three distinct states for a nullable {@code BigDecimal} field:
 * <ul>
 *   <li>Field <b>absent</b> from JSON → Java field stays {@code null} (Jackson never calls the setter).</li>
 *   <li>Field present as JSON {@code null} → {@code Optional.empty()} (explicit clear intent).</li>
 *   <li>Field present with a numeric value → {@code Optional.of(value)}.</li>
 * </ul>
 * This allows callers to distinguish "do not touch" from "clear to null" without
 * polluting the entity with tracking flags.
 */
public class NullableDecimalDeserializer extends JsonDeserializer<Optional<BigDecimal>> {

    @Override
    public Optional<BigDecimal> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return Optional.empty();
        }
        return Optional.of(p.getDecimalValue());
    }

    /**
     * Called by Jackson when the token IS the null literal.
     * Returning {@code Optional.empty()} signals an explicit null was sent.
     */
    @Override
    public Optional<BigDecimal> getNullValue(DeserializationContext ctxt) {
        return Optional.empty();
    }
}
