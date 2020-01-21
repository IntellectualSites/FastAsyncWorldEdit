package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.world.block.BaseBlock;
import java.util.Set;

// TODO FIXME
public abstract class FaweBlockMatcher {
    public abstract boolean apply(BaseBlock block);

    public static FaweBlockMatcher ALWAYS_TRUE = new FaweBlockMatcher() {
        @Override
        public boolean apply(BaseBlock block) {
            return true;
        }
    };

    public static FaweBlockMatcher NOT_AIR = new FaweBlockMatcher() {
        @Override
        public boolean apply(BaseBlock block) {
            return !block.getBlockType().getMaterial().isAir();
        }
    };

    public static FaweBlockMatcher setBlock(BaseBlock block) {
        return null;
//        final int id = block.getId();
//        final int data = block.getData();
//        if (data == 0) {
//            return new FaweBlockMatcher() {
//                @Override
//                public boolean apply(BaseBlock oldBlock) {
//                    int currentId = oldBlock.getId();
//                    oldBlock.setId(id);
//                    if (FaweCache.INSTANCE.hasData(currentId)) {
//                        oldBlock.setData(0);
//                    }
//                    if (FaweCache.INSTANCE.hasNBT(currentId)) {
//                        oldBlock.setNbtData(null);
//                    }
//                    return true;
//                }
//            };
//        }
//        return new FaweBlockMatcher() {
//            @Override
//            public boolean apply(BaseBlock oldBlock) {
//                int currentId = oldBlock.getId();
//                oldBlock.setId(id);
//                oldBlock.setData(data);
//                if (FaweCache.INSTANCE.hasNBT(currentId)) {
//                    oldBlock.setNbtData(null);
//                }
//                return true;
//            }
//        };
    }

    public static FaweBlockMatcher setBlocks(Set<BaseBlock> blocks) {
//        if (blocks.size() == 1) {
//            return setBlock(blocks.iterator().next());
//        }
//        final BaseBlock[] array = blocks.toArray(new BaseBlock[blocks.size()]);
//        final int size = array.length;
//        return new FaweBlockMatcher() {
//            @Override
//            public boolean apply(BaseBlock block) {
//                BaseBlock replace = array[random.random(size)];
//                int currentId = block.getId();
//                block.setId(replace.getId());
//                if (FaweCache.INSTANCE.hasNBT(currentId)) {
//                    block.setNbtData(null);
//                }
//                if (FaweCache.INSTANCE.hasData(currentId) || replace.getData() != 0) {
//                    block.setData(replace.getData());
//                }
//                return true;
//            }
//        };
        return null;
    }

    public static FaweBlockMatcher fromBlock(BaseBlock block, boolean checkData) {
//        final int id = block.getId();
//        final int data = block.getData();
//        if (checkData && FaweCache.INSTANCE.hasData(id)) {
//            return new FaweBlockMatcher() {
//                @Override
//                public boolean apply(BaseBlock block) {
//                    return (block.getId() == id && block.getData() == data);
//                }
//            };
//        } else {
//            return new FaweBlockMatcher() {
//                @Override
//                public boolean apply(BaseBlock block) {
//                    return (block.getId() == id);
//                }
//            };
//        }
        return null;
    }

    public static FaweBlockMatcher fromBlocks(Set<BaseBlock> searchBlocks, boolean checkData) {
//        if (searchBlocks.size() == 1) {
//            return fromBlock(searchBlocks.iterator().next(), checkData);
//        }
//        final boolean[] allowedId = new boolean[FaweCache.INSTANCE.getId(Character.MAX_VALUE)];
//        for (BaseBlock block : searchBlocks) {
//            allowedId[block.getId()] = true;
//        }
//        final boolean[] allowed = new boolean[Character.MAX_VALUE];
//        for (BaseBlock block : searchBlocks) {
//            allowed[FaweCache.INSTANCE.getCombined(block)] = true;
//        }
//        if (checkData) {
//            return new FaweBlockMatcher() {
//                @Override
//                public boolean apply(BaseBlock block) {
//                    int id = block.getId();
//                    if (allowedId[id]) {
//                        if (FaweCache.INSTANCE.hasData(id)) {
//                            return allowed[(id << 4) + block.getData()];
//                        }
//                        return true;
//                    }
//                    return false;
//                }
//            };
//        }
//        return new FaweBlockMatcher() {
//            @Override
//            public boolean apply(BaseBlock block) {
//                return allowedId[block.getId()];
//            }
//        };
        return null;
    }
}
