package com.fastasyncworldedit.core.history.changeset;

import com.sk89q.worldedit.history.change.Change;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.Exchanger;
import java.util.function.BiConsumer;

/**
 * @since TODO
 */
@ApiStatus.Internal
public class ChangeExchangeCoordinator implements AutoCloseable {

    private static final Thread.Builder.OfVirtual UNDO_VIRTUAL_THREAD_BUILDER = Thread.ofVirtual()
            .name("FAWE undo", 0);
    private final Exchanger<Change[]> exchanger;
    private final BiConsumer<Exchanger<Change[]>, Change[]> runnerTask;
    private boolean started = false;
    private Thread runner;

    public ChangeExchangeCoordinator(BiConsumer<Exchanger<Change[]>, Change[]> runner) {
        this.runnerTask = runner;
        this.exchanger = new Exchanger<>();
    }

    public Change[] take(Change[] consumed) {
        if (!this.started) {
            this.started = true;
            final int length = consumed.length;
            this.runner = UNDO_VIRTUAL_THREAD_BUILDER
                    .start(() -> this.runnerTask.accept(this.exchanger, new Change[length]));
        }
        try {
            return exchanger.exchange(consumed);
        } catch (InterruptedException e) {
            this.runner.interrupt();
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public void close() {
        if (this.runner != null) {
            this.runner.interrupt();
        }
    }

}
