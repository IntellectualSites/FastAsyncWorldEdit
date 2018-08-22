/**
 This file is part of VoxelSniper, licensed under the MIT License (MIT).

 Copyright (c) The VoxelBox <http://thevoxelbox.com>
 Copyright (c) contributors

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package com.thevoxelbox.voxelsniper.brush.perform;

import com.sk89q.worldedit.function.pattern.Pattern;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.Brush;
import com.thevoxelbox.voxelsniper.event.SniperBrushChangedEvent;
import java.util.Arrays;
import org.bukkit.Bukkit;

public abstract class PerformBrush extends Brush implements Performer {
    protected vPerformer current = new pMaterial();

    public PerformBrush() {
    }

    public vPerformer getCurrentPerformer() {
        return this.current;
    }

    public void parse(String[] args, SnipeData v) {
        String handle = args[0];
        if(PerformerE.has(handle)) {
            vPerformer p = PerformerE.getPerformer(handle);
            if(p != null) {
                this.current = p;
                SniperBrushChangedEvent event = new SniperBrushChangedEvent(v.owner(), v.owner().getCurrentToolId(), this, this);
                Bukkit.getPluginManager().callEvent(event);
                this.info(v.getVoxelMessage());
                this.current.info(v.getVoxelMessage());
                if(args.length > 1) {
                    String[] additionalArguments = (String[])Arrays.copyOfRange(args, 1, args.length);
                    this.parameters(this.hackTheArray(additionalArguments), v);
                }
            } else {
                this.parameters(this.hackTheArray(args), v);
            }
        } else {
            this.parameters(this.hackTheArray(args), v);
        }

    }

    private String[] hackTheArray(String[] args) {
        String[] returnValue = new String[args.length + 1];
        int i = 0;

        for(int argsLength = args.length; i < argsLength; ++i) {
            String arg = args[i];
            returnValue[i + 1] = arg;
        }

        return returnValue;
    }

    public void initP(SnipeData v) {
        Pattern pattern = v.getPattern();
        if (pattern != null) {
            if (!(current instanceof PatternPerformer)) {
                current = new PatternPerformer();
            }
        } else if (current instanceof PatternPerformer) {
            current = new pMaterial();
        }
        this.current.init(v);
        this.current.setUndo();
    }

    public void showInfo(Message vm) {
        this.current.info(vm);
    }

    public static Class<?> inject() {
        return PerformBrush.class;
    }
}
