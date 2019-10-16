package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import com.boydti.fawe.object.number.MutableLong;
import com.boydti.fawe.util.StringMan;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import java.util.ArrayList;
import java.util.List;

// TODO FIXME
public class MappedReplacePatternFilter extends MCAFilterCounter {
    private Pattern[] map = new Pattern[Character.MAX_VALUE + 1];

    public MappedReplacePatternFilter() {
    }

    public MappedReplacePatternFilter(String from, RandomPattern to, boolean useData) throws InputParseException {
//        List<String> split = StringMan.split(from, ',');
//        Pattern[] patterns = ((RandomPattern) to).getPatterns().toArray(new Pattern[0]);
//        if (patterns.length == split.size()) {
//            for (int i = 0; i < split.size(); i++) {
//                Pattern pattern = patterns[i];
//                String arg = split.get(i);
//                ArrayList<BaseBlock> blocks = new ArrayList<BaseBlock>();
//                for (String arg2 : arg.split(",")) {
//                    BaseBlock block = FaweCache.getBlock(arg, true, !useData);
//                    blocks.add(block);
//                }
//                for (BaseBlock block : blocks) {
//                    if (block.getData() != -1) {
//                        int combined = FaweCache.getCombined(block);
//                        map[combined] = pattern;
//                    } else {
//                        for (int data = 0; data < 16; data++) {
//                            int combined = FaweCache.getCombined(block.getId(), data);
//                            map[combined] = pattern;
//                        }
//                    }
//                }
//            }
//        } else {
//            throw new InputParseException("Mask:Pattern must be a 1:1 match");
//        }
    }

    public void addReplace(BaseBlock block, Pattern pattern) {
//        map[block.getCombined()] = pattern;
    }

//    private final MutableBlockVector3 mutable = new MutableBlockVector3(0, 0, 0);

    @Override
    public void applyBlock(int x, int y, int z, BaseBlock block, MutableLong ignore) {
//        int id = block.getId();
//        int data = FaweCache.hasData(id) ? block.getData() : 0;
//        int combined = FaweCache.getCombined(id, data);
//        Pattern p = map[combined];
//        if (p != null) {
//            BaseBlock newBlock = p.apply(x, y, z);
//            int currentId = block.getId();
//            if (FaweCache.hasNBT(currentId)) {
//                block.setNbtData(null);
//            }
//            block.setId(newBlock.getId());
//            block.setData(newBlock.getData());
//        }
    }
}
