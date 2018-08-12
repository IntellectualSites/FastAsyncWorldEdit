package com.boydti.fawe.util.gui;

import com.boydti.fawe.object.FawePlayer;
import java.net.URL;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public interface FormBuilder<T> {
    FormBuilder setTitle(String text);

    FormBuilder setIcon(URL icon);

    FormBuilder addButton(String text, @Nullable URL image);

    FormBuilder addDropdown(String text, int def, String... options);

    FormBuilder addInput(String text, String placeholder, String def);

    FormBuilder addLabel(String text);

    FormBuilder addSlider(String text, double min, double max, int step, double def);

    FormBuilder addStepSlider(String text, int def, String... options);

    FormBuilder addToggle(String text, boolean def);

    FormBuilder setResponder(Consumer<Map<Integer, Object>> handler);

    void display(FawePlayer<T> fp);
}
