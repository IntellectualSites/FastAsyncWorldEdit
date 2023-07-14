package com.fastasyncworldedit.core.extent.processor;

/**
 * The scope of a processor.
 * Order in which processors are executed:
 * - ADDING_BLOCKS (processors that may ADD blocks to an edit ONLY)
 * - CHANGING_BLOCKS (processors that may ADD or CHANGE blocks being set)
 * - REMOVING_BLOCKS (processors that may ADD, CHANGE or REMOVE blocks being set)
 * - CUSTOM (processors that do not specify a SCOPE)
 * - READING_BLOCKS (processors that do not alter blocks at all, and read the blocks that are actually going to set, e.g.
 * history processors). There is no guarantee that changes made here will be stored in history.
 */
public enum ProcessorScope {
    ADDING_BLOCKS(0),
    CHANGING_BLOCKS(1),
    REMOVING_BLOCKS(2),
    CUSTOM(3),
    READING_BLOCKS(5),
    /**
     * @deprecated use {@link ProcessorScope#READING_BLOCKS}
     */
    @Deprecated(forRemoval = true, since = "TODO")
    READING_SET_BLOCKS(4);

    private final int value;

    ProcessorScope(int value) {
        this.value = value;
    }

    public int intValue() {
        return this.value;
    }

    public static ProcessorScope valueOf(int value) {
        return switch (value) {
            case 0 -> ProcessorScope.ADDING_BLOCKS;
            case 1 -> ProcessorScope.CHANGING_BLOCKS;
            case 2 -> ProcessorScope.REMOVING_BLOCKS;
            case 4 -> ProcessorScope.READING_SET_BLOCKS;
            case 5 -> ProcessorScope.READING_BLOCKS;
            default -> ProcessorScope.CUSTOM;
        };
    }
}
