package com.fastasyncworldedit;

import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchitecturalTest {

    public static Stream<Arguments> abstractDelegateExtentMethods() {
        return Arrays.stream(AbstractDelegateExtent.class.getMethods())
                .filter(m -> m.getDeclaringClass() != Object.class) // ignore methods inherited from java.lang.Object
                // TODO: figure out why enableHistory returns STQE instead of PQE when overriding
                .filter(m -> !m.getName().equals("enableHistory"))
                .map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("abstractDelegateExtentMethods")
    void testAbstractDelegateExtentOverridesAllMethods(Method method) {
        assertEquals(AbstractDelegateExtent.class, method.getDeclaringClass());
    }

}
