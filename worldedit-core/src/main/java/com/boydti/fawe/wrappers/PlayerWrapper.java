package com.boydti.fawe.wrappers;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.TargetBlock;
import com.sk89q.worldedit.util.auth.AuthorizationException;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.gamemode.GameMode;

import javax.annotation.Nullable;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class PlayerWrapper extends AbstractPlayerActor {
    private final Player parent;

    public PlayerWrapper(Player parent) {
        this.parent = parent;
    }

    public static PlayerWrapper wrap(Player parent) {
        if (parent instanceof PlayerWrapper) {
            return (PlayerWrapper) parent;
        }
        return new PlayerWrapper(parent);
    }

    public Player getParent() {
        return parent;
    }

    @Override
    public BaseBlock getBlockInHand(HandSide handSide) throws WorldEditException {
        return parent.getBlockInHand(handSide);
    }

    @Override
    public UUID getUniqueId() {
        return parent.getUniqueId();
    }

    @Override
    public BaseItemStack getItemInHand(HandSide handSide) {
        return parent.getItemInHand(handSide);
    }

    @Override
    public void giveItem(BaseItemStack itemStack) {
        parent.giveItem(itemStack);
    }

    @Override
    public BlockBag getInventoryBlockBag() {
        return parent.getInventoryBlockBag();
    }

    @Override
    public String getName() {
        return parent.getName();
    }

    @Override
    public BaseEntity getState() {
        throw new UnsupportedOperationException("Can't withPropertyId() on a player");
    }

    @Override
    public Location getLocation() {
        return this.parent.getLocation();
    }

    @Override
    public void setPosition(Vector3 pos, float pitch, float yaw) {
        parent.setPosition(pos, pitch, yaw);
    }

    @Override
    public World getWorld() {
        return WorldWrapper.wrap(parent.getWorld());
    }

    @Override
    public void printRaw(String msg) {
        parent.printRaw(msg);
    }

    @Override
    public void printDebug(String msg) {
        parent.printDebug(msg);
    }

    @Override
    public void print(String msg) {
        parent.print(msg);
    }

    @Override
    public void printError(String msg) {
        parent.printError(msg);
    }

    @Override
    public String[] getGroups() {
        return parent.getGroups();
    }

    @Override
    public boolean hasPermission(String perm) {
        return parent.hasPermission(perm);
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
        parent.dispatchCUIEvent(event);
    }

    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        return parent.getFacet(cls);
    }

    @Override
    public SessionKey getSessionKey() {
        return parent.getSessionKey();
    }

    @Override
    public GameMode getGameMode() {
        return parent.getGameMode();
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        parent.setGameMode(gameMode);
    }

    /////////////////////////////////////////
    /////////////////////////////////////////
    /////////////////////////////////////////

    /////////////////////////////////////////
    /////////////////////////////////////////
    /////////////////////////////////////////

    /////////////////////////////////////////
    /////////////////////////////////////////
    /////////////////////////////////////////


    @Override
    public void findFreePosition(final Location searchPos) {
        TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                PlayerWrapper.super.findFreePosition(searchPos);
            }
        });
    }

    @Override
    public void setOnGround(final Location searchPos) {
        TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                PlayerWrapper.super.setOnGround(searchPos);
            }
        });
    }

    @Override
    public void findFreePosition() {
        TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                parent.findFreePosition();
            }
        });
    }

    @Override
    public boolean ascendLevel() {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                this.value = parent.ascendLevel();
            }
        });
    }

    @Override
    public boolean descendLevel() {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                this.value = parent.descendLevel();
            }
        });
    }

    @Override
    public boolean ascendToCeiling(int clearance) {
        return ascendToCeiling(clearance, true);
    }

    @Override
    public boolean ascendToCeiling(int clearance, boolean alwaysGlass) {
        Location pos = getBlockIn();
        int x = pos.getBlockX();
        int initialY = Math.max(0, pos.getBlockY());
        int y = Math.max(0, pos.getBlockY() + 2);
        int z = pos.getBlockZ();
        Extent world = getLocation().getExtent();

        // No free space above
        if (!world.getBlock(BlockVector3.at(x, y, z)).getBlockType().getMaterial().isAir()) {
            return false;
        }

        while (y <= world.getMaximumPoint().getY()) {
            // Found a ceiling!
            if (world.getBlock(BlockVector3.at(x, y, z)).getBlockType().getMaterial().isMovementBlocker()) {
                int platformY = Math.max(initialY, y - 3 - clearance);
                floatAt(x, platformY + 1, z, alwaysGlass);
                return true;
            }

            ++y;
        }

        return false;
    }

    @Override
    public boolean ascendUpwards(int distance) {
        return ascendUpwards(distance, true);
    }

    @Override
    public boolean ascendUpwards(int distance, boolean alwaysGlass) {
        final Location pos = getBlockIn();
        final int x = pos.getBlockX();
        final int initialY = Math.max(0, pos.getBlockY());
        int y = Math.max(0, pos.getBlockY() + 1);
        final int z = pos.getBlockZ();
        final int maxY = Math.min(getWorld().getMaxY() + 1, initialY + distance);
        final Extent world = getLocation().getExtent();

        while (y <= world.getMaximumPoint().getY() + 2) {
            if (world.getBlock(BlockVector3.at(x, y, z)).getBlockType().getMaterial().isMovementBlocker()) {
                break; // Hit something
            } else if (y > maxY + 1) {
                break;
            } else if (y == maxY + 1) {
                floatAt(x, y - 1, z, alwaysGlass);
                return true;
            }

            ++y;
        }

        return false;
    }

    @Override
    public void floatAt(final int x, final int y, final int z, final boolean alwaysGlass) {
        RuntimeException caught = null;
        try {
            EditSession edit = new EditSessionBuilder(parent.getWorld()).player(FawePlayer.wrap(this)).build();
            edit.setBlock(BlockVector3.at(x, y - 1, z), BlockTypes.GLASS);
            edit.flushQueue();
            LocalSession session = Fawe.get().getWorldEdit().getSessionManager().get(this);
            if (session != null) {
                session.remember(edit, true, FawePlayer.wrap(this).getLimit().MAX_HISTORY);
            }
        } catch (RuntimeException e) {
            caught = e;
        }
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                setPosition(Vector3.at(x + 0.5, y, z + 0.5));
            }
        });
        if (caught != null) {
            throw caught;
        }
    }

    @Override
    public Location getBlockTrace(final int range, final boolean useLastBlock) {
        return TaskManager.IMP.sync(new RunnableVal<Location>() {
            @Override
            public void run(Location value) {
                TargetBlock tb = new TargetBlock(PlayerWrapper.this, range, 0.2D);
                this.value = useLastBlock ? tb.getAnyTargetBlock() : tb.getTargetBlock();
            }
        });
    }

    @Override
    public Location getBlockTraceFace(final int range, final boolean useLastBlock) {
        return TaskManager.IMP.sync(new RunnableVal<Location>() {
            @Override
            public void run(Location value) {
                TargetBlock tb = new TargetBlock(PlayerWrapper.this, range, 0.2D);
                this.value = useLastBlock ? tb.getAnyTargetBlockFace() : tb.getTargetBlockFace();
            }
        });
    }

    @Override
    public Location getSolidBlockTrace(final int range) {
        return TaskManager.IMP.sync(new RunnableVal<Location>() {
            @Override
            public void run(Location value) {
                TargetBlock tb = new TargetBlock(PlayerWrapper.this, range, 0.2D);
                this.value = tb.getSolidTargetBlock();
            }
        });
    }

    @Override
    public Direction getCardinalDirection() {
        return parent.getCardinalDirection();
    }

    @Override
    public boolean passThroughForwardWall(final int range) {
        return TaskManager.IMP.sync(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                int searchDist = 0;
                TargetBlock hitBlox = new TargetBlock(PlayerWrapper.this, range, 0.2);
                Extent world = getLocation().getExtent();
                Location block;
                boolean firstBlock = true;
                int freeToFind = 2;
                boolean inFree = false;

                while ((block = hitBlox.getNextBlock()) != null) {
                    boolean free = !world.getBlock(BlockVector3.at(block.getBlockX(), block.getBlockY(), block.getBlockZ())).getBlockType().getMaterial().isMovementBlocker();

                    if (firstBlock) {
                        firstBlock = false;

                        if (!free) {
                            --freeToFind;
                            continue;
                        }
                    }

                    ++searchDist;
                    if (searchDist > 20) {
                        return false;
                    }

                    if (inFree != free) {
                        if (free) {
                            --freeToFind;
                        }
                    }

                    if (freeToFind == 0) {
                        setOnGround(block);
                        return true;
                    }

                    inFree = free;
                }

                return false;
            }
        });
    }

    @Override
    public boolean remove() {
        return parent.remove();
    }

    @Override
    public boolean canDestroyBedrock() {
        return parent.canDestroyBedrock();
    }

    @Override
    public boolean isPlayer() {
        return parent.isPlayer();
    }

    @Override
    public File openFileOpenDialog(String[] extensions) {
        return parent.openFileOpenDialog(extensions);
    }

    @Override
    public File openFileSaveDialog(String[] extensions) {
        return parent.openFileSaveDialog(extensions);
    }

	@Override
	public boolean setLocation(Location location) {
		return parent.setLocation(location);
	}
}
