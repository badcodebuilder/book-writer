package io.bcb.utils.parser;

public class TXTParser implements Parser {

    @Override
    public String parse(String line) {
        // FIXME: String.strip() is supported only since Java 11
        return line.strip();
    }
}
