package by.baykulbackend.services.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.StringReader;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PartCsvRowTest {
    @Test
    void preservesIdentityAndAllowsEmptyNameAndNullableFields() {
        var row = PartCsvRow.parse(" a-01 ;;;;;;12,34;Brand");
        assertEquals(" a-01 ", row.article());
        assertEquals("", row.name());
        assertNull(row.weight());
        assertNull(row.storageCount());
        assertEquals(1, row.minCount());
        assertEquals(new BigDecimal("12.34"), row.price());
        assertEquals(new BigDecimal("0.00"), row.returnPart());
    }

    @ParameterizedTest
    @ValueSource(strings = {"A;Name;NaN;1;2;0;1;BMW", "A;Name;Infinity;1;2;0;1;BMW",
            "A;Name;-1;1;2;0;1;BMW", "A;Name;1;0;2;0;1;BMW", "A;Name;1;1;-2;0;1;BMW",
            "A;Name;1;1;2;0;1.234;BMW", "A;Name;1;1;2;0;-1;BMW", "A;Name;1;1;2;0;1;",
            ";Name;1;1;2;0;1;BMW", "A;Name;1;1;2;0;1;BMW;extra", "broken"})
    void rejectsInvalidRows(String line) {
        assertThrows(IllegalArgumentException.class, () -> PartCsvRow.parse(line));
    }

    @Test
    void boundsOversizedLinesAndRecoversWithAccurateLineBoundaries() throws Exception {
        try (var reader = new BoundedCsvReader(new StringReader("x".repeat(100000) + "\r\nnext\nlast"))) {
            var oversized = reader.next();
            assertTrue(oversized.oversized());
            assertEquals(8192, oversized.text().length());
            assertEquals("next", reader.next().text());
            assertEquals("last", reader.next().text());
            assertNull(reader.next());
            assertNull(reader.next());
        }
    }
}
