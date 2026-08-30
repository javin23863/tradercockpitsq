/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.debugging;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import org.apache.commons.io.output.TeeOutputStream;

public class MessageConsole {
    private JTextComponent textComponent;
    private Document document;
    private boolean isAppend;

    public MessageConsole(JTextComponent jTextComponent) {
        this(jTextComponent, true);
    }

    public MessageConsole(JTextComponent jTextComponent, boolean bl) {
        this.textComponent = jTextComponent;
        this.document = jTextComponent.getDocument();
        this.isAppend = bl;
        jTextComponent.setEditable(false);
    }

    public void redirectOut() {
        this.redirectOut(null, null);
    }

    public void redirectOut(Color color, PrintStream printStream) {
        ConsoleOutputStream consoleOutputStream = new ConsoleOutputStream(color, printStream);
        TeeOutputStream teeOutputStream = new TeeOutputStream((OutputStream)System.out, (OutputStream)consoleOutputStream);
        System.setOut(new PrintStream((OutputStream)teeOutputStream, true));
    }

    public void redirectErr() {
        this.redirectErr(null, null);
    }

    public void redirectErr(Color color, PrintStream printStream) {
        ConsoleOutputStream consoleOutputStream = new ConsoleOutputStream(color, printStream);
        TeeOutputStream teeOutputStream = new TeeOutputStream((OutputStream)System.err, (OutputStream)consoleOutputStream);
        System.setErr(new PrintStream((OutputStream)teeOutputStream, true));
    }

    class ConsoleOutputStream
    extends ByteArrayOutputStream {
        private final String EOL = System.getProperty("line.separator");
        private SimpleAttributeSet attributes;
        private PrintStream printStream;
        private StringBuffer buffer = new StringBuffer(80);
        private boolean isFirstLine;

        public ConsoleOutputStream(Color color, PrintStream printStream) {
            if (color != null) {
                this.attributes = new SimpleAttributeSet();
                StyleConstants.setForeground(this.attributes, color);
            }
            this.printStream = printStream;
            if (MessageConsole.this.isAppend) {
                this.isFirstLine = true;
            }
        }

        @Override
        public void flush() {
            String string = this.toString();
            if (string.length() == 0) {
                return;
            }
            if (MessageConsole.this.isAppend) {
                this.handleAppend(string);
            } else {
                this.handleInsert(string);
            }
            this.reset();
        }

        private void handleAppend(String string) {
            if (MessageConsole.this.document.getLength() == 0) {
                this.buffer.setLength(0);
            }
            if (this.EOL.equals(string)) {
                this.buffer.append(string);
            } else {
                this.buffer.append(string);
                this.clearBuffer();
            }
        }

        private void handleInsert(String string) {
            this.buffer.append(string);
            if (this.EOL.equals(string)) {
                this.clearBuffer();
            }
        }

        private void clearBuffer() {
            if (this.isFirstLine && MessageConsole.this.document.getLength() != 0) {
                this.buffer.insert(0, "\n");
            }
            this.isFirstLine = false;
            String string = this.buffer.toString();
            try {
                if (MessageConsole.this.isAppend) {
                    int n = MessageConsole.this.document.getLength();
                    MessageConsole.this.document.insertString(n, string, this.attributes);
                    MessageConsole.this.textComponent.setCaretPosition(MessageConsole.this.document.getLength());
                } else {
                    MessageConsole.this.document.insertString(0, string, this.attributes);
                    MessageConsole.this.textComponent.setCaretPosition(0);
                }
            }
            catch (BadLocationException badLocationException) {
                // empty catch block
            }
            if (this.printStream != null) {
                this.printStream.print(string);
            }
            this.buffer.setLength(0);
        }
    }
}

