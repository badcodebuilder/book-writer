package io.bcb.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.bcb.Constant;
import io.bcb.utils.parser.Parser;

public class BookGenerator {
    private BufferedReader in;
    private Parser parser;
    private String line;
    private int index = 0;
    private boolean isBookEnd = false;
    private byte[] sizes;

    public BookGenerator(File file, Parser parser, byte[] sizes) {
        try {
            this.in = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            this.in = null;
            e.printStackTrace();
        }
        this.parser = parser;
        this.line = "";
        this.sizes = sizes;
    }

    /**
     * Generate ONE book content from given text file, pixel-accurate
     * level
     * @return
     */
    public List<String> generate() {
        if (this.in == null) {
            return null;
        }

        boolean isNewLine = false;
        List<String> book = new ArrayList<>();

        while (book.size() < 90) {
            StringBuffer page = new StringBuffer();
            int[] cursorPos = {0,0};

            while (true) {
                if (this.index >= this.line.length()) {
                    // Read new line
                    try {
                        this.line = in.readLine();
                        this.index = 0;
                        isNewLine = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                    // If reach EOF
                    if (this.line == null) {
                        book.add(page.toString());
                        this.isBookEnd = true;
                        break;
                    }
                    // Pares line
                    this.line = this.parser.parse(this.line);
                }

                char c = isNewLine ? '\n' : this.line.charAt(this.index);
                if (updataCursor(cursorPos, c) < 0) {
                    book.add(page.toString());
                    break;
                }
                page.append(c);
                if (!isNewLine) {
                    ++index;
                }
                isNewLine = false;
            }

            if (isBookEnd) {
                break;
            }
        }

        return book;
    }

    public boolean getIsBookEnd() {
        return this.isBookEnd;
    }

    private int updataCursor(int[] cursorPos, char c) {
        if (cursorPos.length < 2) {
            return -2;
        }

        // XXX: Maybe there are some chars with no width, whether use them or not
        int charAdvance = getCharAdvance(c);
        if (c == '\n' || cursorPos[1] + charAdvance > Constant.pageWidthThreshold) {
            if (cursorPos[0] + 1 >= Constant.pageMaxRowCount) {
                return -1;
            } else {
                ++cursorPos[0];
                cursorPos[1] = charAdvance;
            }
        } else {
            cursorPos[1] += charAdvance;
        }
        
        return 0;
    }

    private int getCharAdvance(char c) {
        // TODO: get sizes.bin and load to an array of byte
        switch (c) {
            case '\n':
                return 0;
        }
        byte binData = this.sizes[c];
        int width = ((binData & 0x0f) + 1) - ((binData >> 4) & 0x0f);
        return (width >> 1) + 1;
    }
}
