package com.fastasyncworldedit.core.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class UpdateNotificationTest {

    @ParameterizedTest
    @CsvSource({"1.0.0,2.0.0", "1.0.0,1.1.0", "1.0.0,1.0.1", "1.0.0,1.0.0.1"})
    void hasUpdateSemver_true(String installed, String latest) {
        assertTrue(UpdateNotification.hasUpdateSemver(
                Arrays.stream(installed.split("\\.")).mapToInt(Integer::parseInt).toArray(),
                Arrays.stream(latest.split("\\.")).mapToInt(Integer::parseInt).toArray()
        ));
    }

    @ParameterizedTest
    @CsvSource({"1.0.0,1.0.0", "2.0.0,1.9.9", "1.0.0,1.0", "1.0,1.0.0"})
    void hasUpdateSemver_false(String installed, String latest) {
        assertFalse(UpdateNotification.hasUpdateSemver(
                Arrays.stream(installed.split("\\.")).mapToInt(Integer::parseInt).toArray(),
                Arrays.stream(latest.split("\\.")).mapToInt(Integer::parseInt).toArray()
        ));
    }

}
