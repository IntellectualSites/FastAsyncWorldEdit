package com.boydti.fawe.configuration;

/**
 * Various settings for controlling the input and output of a {@link
 * com.boydti.fawe.configuration.MemoryConfiguration}
 */
public class MemoryConfigurationOptions extends ConfigurationOptions {
    protected MemoryConfigurationOptions(final MemoryConfiguration configuration) {
        super(configuration);
    }

    @Override
    public MemoryConfiguration configuration() {
        return (MemoryConfiguration) super.configuration();
    }

    @Override
    public com.boydti.fawe.configuration.MemoryConfigurationOptions copyDefaults(final boolean value) {
        super.copyDefaults(value);
        return this;
    }

    @Override
    public com.boydti.fawe.configuration.MemoryConfigurationOptions pathSeparator(final char value) {
        super.pathSeparator(value);
        return this;
    }
}
