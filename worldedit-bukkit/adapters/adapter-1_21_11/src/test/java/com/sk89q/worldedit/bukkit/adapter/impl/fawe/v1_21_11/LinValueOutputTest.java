package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_21_11;

import net.minecraft.core.HolderLookup;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.ValueOutput;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LinValueOutputTest {

    @Test
    public void testNestedComponentOutput() {
        final LinValueOutput output = LinValueOutput.createWithContext(
                ProblemReporter.DISCARDING, HolderLookup.Provider.create(Stream.empty())
        );
        output.child("first_child")
                .child("second_child")
                .child("third_child")
                .putString("hello", "world");
        assertEquals(LinCompoundTag.builder()
                .put("first_child", LinCompoundTag.builder()
                        .put("second_child", LinCompoundTag.builder()
                                .put("third_child", LinCompoundTag.builder()
                                        .putString("hello", "world")
                                        .build())
                                .build())
                        .build())
                .build(), output.buildResult());
    }

    @Test
    public void testMerge() {
        final LinValueOutput output = LinValueOutput.createWithContext(
                ProblemReporter.DISCARDING, HolderLookup.Provider.create(Stream.empty())
        );
        output.putString("hello", "world");
        ValueOutput child = output.child("compound_holder");
        child.putBoolean("success", false);
        child.putInt("my_int", 1234);
        output.merge(LinCompoundTag.builder()
                .put("compound_holder", LinCompoundTag.builder()
                        .putByte("success", (byte) 1)
                        .build())
                .putInt("dummy", 1337)
                .put("another_compound", LinCompoundTag.builder().build())
                .build());
        assertEquals(LinCompoundTag.builder()
                .putString("hello", "world")
                .put("compound_holder", LinCompoundTag.builder()
                        .putInt("my_int", 1234)
                        .putByte("success", (byte) 1)
                        .build())
                .putInt("dummy", 1337)
                .put("another_compound", LinCompoundTag.builder().build())
                .build(), output.buildResult());
    }

}
