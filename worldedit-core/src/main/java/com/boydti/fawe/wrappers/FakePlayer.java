package com.boydti.fawe.wrappers;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.google.common.base.Charsets;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.*;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.NullWorld;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.item.ItemTypes;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Only really useful for executing commands from console<br>
 * - The API itself doesn't any fake player anywhere
 */
public class FakePlayer extends AbstractPlayerActor {
    private static FakePlayer CONSOLE;

    public static FakePlayer getConsole() {
        if (CONSOLE == null) {
            CONSOLE = new FakePlayer("#CONSOLE", null, null) {
                @Override
                public boolean hasPermission(String permission) {
                    return true;
                }
            };
        }
        return CONSOLE;
    }

    private final Actor parent;
    private final String name;
    private final UUID uuid;
    private World world;
    private Location pos;

    public static FakePlayer wrap(String name, UUID uuid, Actor parent) {
        if (parent != null && parent.getUniqueId().toString().equals("a233eb4b-4cab-42cd-9fd9-7e7b9a3f74be")) {
            return getConsole();
        }
        return new FakePlayer(name, uuid, parent);
    }

    public FakePlayer(String name, UUID uuid, Actor parent) {
        this.name = name;
        this.uuid = uuid == null ? UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8)) : uuid;
        try {
            this.world = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).getWorlds().get(0);
        } catch (NoCapablePlatformException e) {
            this.world = NullWorld.getInstance();
        }
        this.pos = new Location(world, 0, 0, 0);
        this.parent = parent;
    }

    private FawePlayer fp = null;

    public FawePlayer toFawePlayer() {
        if (fp != null) {
            Fawe.get().register(fp);
            return fp;
        }
        FawePlayer existing = Fawe.get().getCachedPlayer(getName());
        if (existing != null) {
            return fp = existing;
        }
        final Actor actor = this;
        return fp = new FawePlayer(this) {
            @Override
            public void sendTitle(String head, String sub) {
            }

            @Override
            public void resetTitle() {
            }

            @Override
            public String getName() {
                return actor.getName();
            }

            @Override
            public UUID getUUID() {
                return actor.getUniqueId();
            }

            @Override
            public boolean hasPermission(String perm) {
                return actor.hasPermission(perm) || (Boolean) getMeta("perm." + perm, false);
            }

            @Override
            public void setPermission(String perm, boolean flag) {
                setMeta("perm." + perm, true);
            }

            @Override
            public void sendMessage(String message) {
                actor.print(message);
            }

            @Override
            public void executeCommand(String substring) {
                CommandManager.getInstance().handleCommand(new CommandEvent(actor, substring));
            }

            @Override
            public FaweLocation getLocation() {
                Location loc = FakePlayer.this.getLocation();
                String world;
                if (loc.getExtent() instanceof World) {
                    world = ((World) loc.getExtent()).getName();
                } else {
                    world = loc.getExtent().toString();
                }
                return new FaweLocation(world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            }

            @Override
            public Player toWorldEditPlayer() {
                return FakePlayer.this;
            }
        };
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public BaseItemStack getItemInHand(HandSide handSide) {
        return new BaseItemStack(ItemTypes.AIR, 0);
    }

    @Override
    public BaseBlock getBlockInHand(HandSide ignore) {
        return new BaseBlock(BlockTypes.AIR.getDefaultState());
    }

    @Override
    public void giveItem(BaseItemStack itemStack) {

    }

    @Override
    public BlockBag getInventoryBlockBag() {
        return null;
    }

    @Override
    public void setPosition(Vector3 pos, float pitch, float yaw) {
    	//TODO: find replacement for following code
//        if (pos instanceof Location) {
//            Extent extent = ((Location) pos).getExtent();
//            if (extent instanceof World) this.world = (World) extent;
//        }
        this.pos = new Location(world, pos, yaw, pitch);
    }

    @Nullable
    @Override
    public BaseEntity getState() {
        return null;
    }

    @Override
    public Location getLocation() {
        return pos;
    }

    @Override
    public String getName() {
        if (parent != null) {
            return parent.getName();
        }
        return name;
    }

    @Override
    public void printRaw(String msg) {
        if (parent != null) {
            parent.printRaw(msg);
            return;
        }
        Fawe.get().debugPlain(msg);
    }

    @Override
    public void printDebug(String msg) {
        if (parent != null) {
            parent.printDebug(msg);
            return;
        }
        Fawe.get().debugPlain(msg);
    }

    @Override
    public void print(String msg) {
        if (parent != null) {
            parent.print(msg);
            return;
        }
        Fawe.get().debugPlain(msg);
    }

    @Override
    public void printError(String msg) {
        if (parent != null) {
            parent.printError(msg);
            return;
        }
        Fawe.get().debugPlain(msg);
    }

    private FakeSessionKey key;

    @Override
    public SessionKey getSessionKey() {
        if (parent != null) {
            return parent.getSessionKey();
        }
        if (key == null) {
            key = new FakeSessionKey(uuid, name);
        }
        return key;
    }

    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        return null;
    }

    @Override
    public UUID getUniqueId() {
        if (parent != null) {
            return parent.getUniqueId();
        }
        return uuid;
    }

    @Override
    public String[] getGroups() {
        if (parent != null) {
            return parent.getGroups();
        }
        return new String[0];
    }

    @Override
    public boolean hasPermission(String permission) {
        if (parent != null) {
            return parent.hasPermission(permission);
        }
        return true;
    }

    private static class FakeSessionKey implements SessionKey {
        private final UUID uuid;
        private final String name;

        private FakeSessionKey(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        @Override
        public UUID getUniqueId() {
            return uuid;
        }

        @Nullable
        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public boolean isPersistent() {
            return true;
        }
    }

	@Override
	public boolean setLocation(Location location) {
		this.pos = location;
		return true;
	}
}
