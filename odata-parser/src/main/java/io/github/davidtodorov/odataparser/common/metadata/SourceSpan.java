package io.github.davidtodorov.odataparser.common.metadata;

import java.util.Objects;

public record SourceSpan(
        int start,
        int end
) {

    private static final SourceSpan UNKNOWN = new SourceSpan(0, 0);

    public SourceSpan {
        if (start < 0) {
            throw new IllegalArgumentException(
                    "Source span start cannot be negative"
            );
        }

        if (end < start) {
            throw new IllegalArgumentException(
                    "Source span end cannot be before its start"
            );
        }
    }

    public static SourceSpan unknown() {
        return UNKNOWN;
    }

    public int length() {
        return end - start;
    }

    public boolean contains(int position) {
        return position >= start && position < end;
    }

    public boolean isUnknown() {
        return equals(UNKNOWN);
    }

    public SourceSpan cover(SourceSpan other) {
        Objects.requireNonNull(
                other,
                "Other source span cannot be null"
        );

        if (isUnknown() || other.isUnknown()) {
            return unknown();
        }

        return new SourceSpan(
                Math.min(start, other.start),
                Math.max(end, other.end)
        );
    }
}
