package com.fastasyncworldedit.core.regions.selector;

import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.regions.RegionSelector;

import javax.annotation.Nullable;

public interface ActorRegionSelector extends RegionSelector {

    void setActor(@Nullable Actor actor);

}
