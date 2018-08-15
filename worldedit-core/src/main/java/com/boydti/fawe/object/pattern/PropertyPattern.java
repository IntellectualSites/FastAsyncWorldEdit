package com.boydti.fawe.object.pattern;

import com.boydti.fawe.object.string.MutableCharSequence;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.StringMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.registry.state.AbstractProperty;
import com.sk89q.worldedit.registry.state.IntegerProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.List;

public class PropertyPattern extends AbstractExtentPattern {
    public PropertyPattern(Extent extent, String[] properties) {
        super(extent);
        addRegex(".*[" + StringMan.join(properties, ",") + "]");
    }

    public PropertyPattern(Extent extent) {
        super(extent);
    }

    private int[][] intSets = new int[BlockTypes.size()][];

    private static final Operator EQUAL = (length, value, index) -> value;
    private static final Operator PLUS = (length, value, index) -> index + value;
    private static final Operator MINUS = (length, value, index) -> index - value;
    private static final Operator MODULO = (length, value, index) -> index % value;
    private static final Operator AND = (length, value, index) -> index & value;
    private static final Operator OR = (length, value, index) -> index | value;
    private static final Operator XOR = (length, value, index) -> index ^ value;

    private interface Operator {
        int apply(int length, int value, int index);
    }

    private Operator getOp(char c) {
        switch (c) {
            case '=': return EQUAL;
            case '+': return PLUS;
            case '-': return MINUS;
            case '%': return MODULO;
            case '&': return AND;
            case '|': return OR;
            case '^': return XOR;
            default: return null;
        }
    }

    private void add(BlockTypes type, PropertyKey key, Operator operator, MutableCharSequence value, boolean wrap) {
        if (!type.hasProperty(key)) return;
        AbstractProperty property = (AbstractProperty) type.getProperty(key);
        BlockState defaultState = type.getDefaultState();
        int valueInt;
        if (value.length() == 0) {
            valueInt = property.getIndex(defaultState.getInternalId());
        } else if (!(property instanceof IntegerProperty) && MathMan.isInteger(value)) {
            valueInt = StringMan.parseInt(value);
        } else {
            valueInt = property.getIndexFor(value);
        }
        List values = property.getValues();
        int length = values.size();

        int[] states = null;
        for (int i = 0; i < values.size(); i++) {
            int result = operator.apply(length, valueInt, i);
            if (wrap) result = MathMan.wrap(result, 0, length - 1);
            else result = Math.max(Math.min(result, length - 1), 0);
            if (result == i) continue;

            if (states == null) {
                states = intSets[type.getInternalId()];
                if (states == null) {
                    intSets[type.getInternalId()] = states = new int[type.getMaxStateId() + 1];
                    for (int j = 0; j < states.length; j++) states[j] = j;
                }
            }
            int state = property.modifyIndex(0, i);
            if (type.getProperties().size() > 1) {
                ArrayList<Property> properties = new ArrayList<>(type.getProperties().size() - 1);
                for (Property current : type.getProperties()) {
                    if (current == property) continue;
                    properties.add(current);
                }
                applyRecursive(property, properties, 0, state, result, states);
            } else {
                states[i] = result;
            }
        }
    }

    private void applyRecursive(AbstractProperty property, List<Property> properties, int propertiesIndex, int state, int index, int[] states) {
        AbstractProperty current = (AbstractProperty) properties.get(propertiesIndex);
        List values = current.getValues();
        if (propertiesIndex + 1 < properties.size()) {
            for (int i = 0; i < values.size(); i++) {
                int newState = current.modifyIndex(state, i);
                applyRecursive(property, properties, propertiesIndex + 1, newState, index, states);
            }
        } else {
            //set chest[waterlogged=north]
            for (int i = 0; i < values.size(); i++) {
                int statesIndex = current.modifyIndex(state, i) >> BlockTypes.BIT_OFFSET;
                int existing = states[statesIndex] << BlockTypes.BIT_OFFSET;
                states[statesIndex] = property.modifyIndex(existing, index) >> BlockTypes.BIT_OFFSET;
            }
        }
    }

    public PropertyPattern addRegex(String input) {
        if (input.charAt(input.length() - 1) == ']') {
            int propStart = StringMan.findMatchingBracket(input, input.length() - 1);
            if (propStart == -1) return this;

            MutableCharSequence charSequence = MutableCharSequence.getTemporal();
            charSequence.setString(input);
            charSequence.setSubstring(0, propStart);

            BlockTypes type = null;
            List<BlockTypes> blockTypeList = null;
            if (StringMan.isAlphanumericUnd(charSequence)) {
                type = BlockTypes.get(charSequence);
            } else {
                String regex = charSequence.toString();
                blockTypeList = new ArrayList<>();
                for (BlockTypes myType : BlockTypes.values) {
                    if (myType.getId().matches(regex)) {
                        blockTypeList.add(myType);
                    }
                }
                if (blockTypeList.size() == 1) type = blockTypeList.get(0);
            }

            PropertyKey key = null;
            int length = input.length();
            int last = propStart + 1;
            Operator operator = null;
            boolean wrap = false;
            for (int i = last; i < length; i++) {
                char c = input.charAt(i);
                switch (c) {
                    case '[':
                    case '{':
                    case '(':
                        int next = StringMan.findMatchingBracket(input, i);
                        if (next != -1) i = next;
                        break;
                    case ']':
                    case ',': {
                        charSequence.setSubstring(last, i);
                        char firstChar = input.charAt(last + 1);
                        if (type != null) add(type, key, operator, charSequence, wrap);
                        else {
                            for (BlockTypes myType : blockTypeList) {
                                add(myType, key, operator, charSequence, wrap);
                            }
                        }
                        last = i + 1;
                        break;
                    }
                    default: {
                        Operator tmp = getOp(c);
                        if (tmp != null) {
                            operator = tmp;
                            charSequence.setSubstring(last, i);
                            char cp = input.charAt(i + 1);
                            boolean extra = cp == '=';
                            wrap = cp == '~';
                            if (extra || wrap) i++;
                            if (charSequence.length() > 0) key = PropertyKey.get(charSequence);
                            last = i + 1;
                        }
                        break;
                    }
                }
            }
        }
        return this;
    }

    @Override
    public BlockStateHolder apply(Vector position) {
        BlockState block = getExtent().getBlock(position);
        return apply(block, block);
    }

    public BlockState apply(BlockState block, BlockState orDefault) {
        int typeId = block.getInternalBlockTypeId();
        int[] states = intSets[typeId];
        if (states == null) return orDefault;
        int propertyId = block.getInternalPropertiesId();
        int newPropertyId = states[propertyId];
        if (newPropertyId == propertyId) return orDefault;
        BlockState newState = block.withPropertyId(newPropertyId);
        CompoundTag nbt = block.getNbtData();
        if (nbt == null) return newState;
        return new BaseBlock(newState, nbt);
    }

    @Override
    public boolean apply(Extent extent, Vector set, Vector get) throws WorldEditException {
        BlockState block = getExtent().getBlock(get);
        block = apply(block, null);
        if (block != null) {
            return extent.setBlock(set, block);
        }
        return false;
    }
}
