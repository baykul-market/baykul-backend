package by.baykulbackend.services.product;

import java.io.IOException;
import java.io.Reader;

/** Bounds memory even if an uploaded file contains a single enormous malformed line. */
public final class BoundedCsvReader implements AutoCloseable {
    public record Line(String text, boolean oversized) { }
    private static final int MAX_LINE = 8192;
    private final Reader reader;
    private final char[] buffer = new char[8192];
    private int offset;
    private int length;
    private boolean skipLf;
    private boolean eof;

    public BoundedCsvReader(Reader reader) {
        this.reader = reader;
    }

    public Line next() throws IOException {
        if (eof) {
            return null;
        }
        StringBuilder line = new StringBuilder();
        boolean oversized = false;
        boolean readAny = false;
        while (true) {
            if (offset == length) {
                length = reader.read(buffer);
                offset = 0;
                if (length < 0) {
                    eof = true;
                    return readAny ? new Line(line.toString(), oversized) : null;
                }
            }
            char ch = buffer[offset++];
            if (skipLf) {
                skipLf = false;
                if (ch == '\n') {
                    continue;
                }
            }
            readAny = true;
            if (ch == '\r' || ch == '\n') {
                skipLf = ch == '\r';
                return new Line(line.toString(), oversized);
            }
            if (line.length() < MAX_LINE) {
                line.append(ch);
            } else {
                oversized = true;
            }
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
