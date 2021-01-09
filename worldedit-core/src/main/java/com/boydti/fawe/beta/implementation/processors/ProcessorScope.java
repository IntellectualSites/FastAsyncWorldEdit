package com.boydti.fawe.beta.implementation.processors;

/**
 * The scope of a processor.
 * Order in which processors are executed:
 *  - ADDING_BLOCKS (processors that strictly ADD blocks to an edit ONLY)
 *  - CHANGING_BLOCKS (processors that strictly ADD or CHANGE blocks being set)
 *  - REMOVING_BLOCKS (processors that string ADD, CHANGE or REMOVE blockks being set)
 *  - CUSTOM (processors that do not specify a SCOPE)
 *  - SAVING_BLOCKS (processors that do NONE of the above, and save the blocks being set/changed/removed)
 */
public enum ProcessorScope {
    ADDING_BLOCKS(0), CHANGING_BLOCKS(1), REMOVING_BLOCKS(2), CUSTOM(3), SAVING_BLOCKS(4);

    private final int value;

    ProcessorScope(int value) {
        this.value = value;
    }

    public int intValue() {
        return this.value;
    }

    public static ProcessorScope valueOf(int value) {
        switch (value) {
            case 0:
                return ProcessorScope.ADDING_BLOCKS;
            case 1:
                return ProcessorScope.CHANGING_BLOCKS;
            case 2:
                return ProcessorScope.REMOVING_BLOCKS;
            case 4:
                return ProcessorScope.SAVING_BLOCKS;
            case 3:
            default:
                return ProcessorScope.CUSTOM;
        }
    }
}
