/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.blocks;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.platform.PlatformsRegisteredEvent;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extension.platform.PlatformManager;
import com.sk89q.worldedit.extension.platform.Preference;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.util.test.ResourceLockKeys;
import com.sk89q.worldedit.world.block.BlockState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ResourceLock(ResourceLockKeys.WORLDEDIT_PLATFORM)
@SuppressWarnings("deprecation")
class SignBlockTest {

    private static final String EMPTY_JSON = "{\"text\":\"\"}";
    private static final String HELLO_JSON = "{\"text\":\"Hello\"}";

    private final AtomicInteger dataVersion = new AtomicInteger();
    private final Platform platform = mock(Platform.class);

    @BeforeEach
    void setUpPlatform() {
        when(platform.getCapabilities()).thenReturn(Map.of(Capability.WORLD_EDITING, Preference.PREFERRED));
        when(platform.getDataVersion()).thenAnswer(__ -> dataVersion.get());
        when(platform.getVersion()).thenReturn("test");

        PlatformManager platformManager = WorldEdit.getInstance().getPlatformManager();
        platformManager.register(platform);
        WorldEdit.getInstance().getEventBus().post(new PlatformsRegisteredEvent());
    }

    @AfterEach
    void tearDownPlatform() {
        WorldEdit.getInstance().getPlatformManager().unregister(platform);
    }

    @Test
    void usesJsonStringsBeforeMinecraft1215() {
        dataVersion.set(Constants.DATA_VERSION_MC_1_21_4);

        assertArrayEquals(
                new String[]{EMPTY_JSON, HELLO_JSON, EMPTY_JSON, EMPTY_JSON},
                messages(new SignBlock(mock(BlockState.class), new String[]{"", "Hello", "", ""}))
        );
        assertArrayEquals(
                new String[]{EMPTY_JSON, EMPTY_JSON, EMPTY_JSON, EMPTY_JSON},
                messages(new SignBlock(mock(BlockState.class), null))
        );

        SignBlock loadedSign = new SignBlock(mock(BlockState.class), null);
        loadedSign.setNbtData(signNbt(HELLO_JSON));
        assertArrayEquals(
                new String[]{HELLO_JSON, EMPTY_JSON, EMPTY_JSON, EMPTY_JSON},
                messages(loadedSign)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {
            Constants.DATA_VERSION_MC_1_21_5,
            Constants.DATA_VERSION_MC_1_21_10,
            Constants.DATA_VERSION_MC_1_21_11,
            Constants.DATA_VERSION_MC_26_1_2
    })
    void usesLiteralStringsFromMinecraft1215(int dataVersion) {
        this.dataVersion.set(dataVersion);

        assertArrayEquals(
                new String[]{"", "Hello", "", ""},
                messages(new SignBlock(mock(BlockState.class), new String[]{"", "Hello", "", ""}))
        );
        assertArrayEquals(
                new String[]{"", "", "", ""},
                messages(new SignBlock(mock(BlockState.class), null))
        );

        SignBlock loadedSign = new SignBlock(mock(BlockState.class), null);
        loadedSign.setNbtData(signNbt("Hello"));
        assertArrayEquals(
                new String[]{"Hello", "", "", ""},
                messages(loadedSign)
        );
    }

    private static String[] messages(SignBlock sign) {
        CompoundTag frontText = (CompoundTag) sign.getNbtData().getValue().get("front_text");
        ListTag<?, ?> messages = frontText.getListTag("messages");
        return messages.getValue().stream()
                .map(StringTag.class::cast)
                .map(StringTag::getValue)
                .toArray(String[]::new);
    }

    private static CompoundTag signNbt(String... messages) {
        return new CompoundTag(Map.of(
                "id", new StringTag("minecraft:sign"),
                "front_text", new CompoundTag(Map.of(
                        "messages", new ListTag<>(
                                StringTag.class,
                                Arrays.stream(messages).map(StringTag::new).toList()
                        )
                ))
        ));
    }

}
