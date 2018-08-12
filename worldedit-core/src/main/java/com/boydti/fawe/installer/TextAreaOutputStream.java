package com.boydti.fawe.installer;

import com.boydti.fawe.config.BBC;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JTextArea;

public class TextAreaOutputStream extends PrintStream {

    public TextAreaOutputStream(final JTextArea textArea) {
        super(new OutputStream() {
            StringBuffer buffer = new StringBuffer();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            AtomicBoolean updated = new AtomicBoolean();
            AtomicBoolean waiting = new AtomicBoolean();
            boolean lineColor = false;

            @Override
            public void write(int b) throws IOException {
                buffer.append((char) b);
                if (b == '\n') {
                    updated.set(true);
                    if (waiting.compareAndSet(false, true)) {
                        executor.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    updated.set(false);
                                    int len = buffer.length();
                                    textArea.append(BBC.stripColor(buffer.substring(0, len)));
                                    buffer.delete(0, len);
                                    textArea.setVisible(true);
                                    textArea.repaint();
                                } finally {
                                    waiting.set(false);
                                    if (updated.get() && waiting.compareAndSet(false, true)) {
                                        executor.submit(this);
                                    }
                                }
                            }
                        });
                    }
                } else {
                    updated.lazySet(true);
                }
            }

            @Override
            protected void finalize() throws Throwable {
                executor.shutdownNow();
            }
        });
    }
}
