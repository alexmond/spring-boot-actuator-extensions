package org.alexmond.actuator.mcp;

/**
 * Caps the size of a tool result so one call cannot flood an assistant's context.
 */
public final class ResponseLimiter {

    private final int maxChars;

    public ResponseLimiter(int maxChars) {
        if (maxChars < 1) {
            throw new IllegalArgumentException("maxChars must be positive, was " + maxChars);
        }
        this.maxChars = maxChars;
    }

    public String limit(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        int dropped = text.length() - maxChars;
        return text.substring(0, maxChars) + "\n... [truncated: " + dropped + " of " + text.length()
                + " characters dropped. Call the tool with narrower arguments, or raise"
                + " management.endpoints.mcp.max-response-chars]";
    }
}
