/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.scripting;

import com.boydti.fawe.Fawe;
import com.sk89q.worldedit.WorldEditException;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.Map;

public class NashornCraftScriptEngine implements CraftScriptEngine {
    private static NashornScriptEngineFactory FACTORY;
    private int timeLimit;

    @Override
    public void setTimeLimit(int milliseconds) {
        timeLimit = milliseconds;
    }

    @Override
    public int getTimeLimit() {
        return timeLimit;
    }

    @Override
    public Object evaluate(String script, String filename, Map<String, Object> args) throws Throwable {
        ClassLoader cl = Fawe.get().getClass().getClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        synchronized (NashornCraftScriptEngine.class) {
            if (FACTORY == null) FACTORY = new NashornScriptEngineFactory();
        }
        ;
        ScriptEngine engine = FACTORY.getScriptEngine("--language=es6");
        SimpleBindings bindings = new SimpleBindings();

        for (Map.Entry<String, Object> entry : args.entrySet()) {
            bindings.put(entry.getKey(), entry.getValue());
        }

       try {
           return engine.eval(script, bindings);
        } catch (Error e) {
            e.printStackTrace();
            throw new ScriptException(e.getMessage());
        } catch (Throwable e) {
            e.printStackTrace();
            while (e.getCause() != null) {
                e = e.getCause();
            }
            if (e instanceof WorldEditException) {
                throw e;
            }
            throw e;
        }
    }

}
