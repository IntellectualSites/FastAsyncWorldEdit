/***
 *
 * Copyright (c) 2007 Paul Hammant
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.sk89q.worldedit.util.command.parametric;

import com.thoughtworks.paranamer.CachingParanamer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Parameter;

/**
 * Default implementation of Paranamer reads from a post-compile added field called '__PARANAMER_DATA'
 *
 * @author Paul Hammant
 * @author Mauro Talevi
 * @author Guilherme Silveira
 */
public class FaweParanamer extends CachingParanamer {

    @Override
    public String[] lookupParameterNames(AccessibleObject methodOrConstructor, boolean throwExceptionIfMissing) {
        Parameter[] params;
        if (methodOrConstructor instanceof Constructor) {
            params = ((Constructor) methodOrConstructor).getParameters();
        } else if (methodOrConstructor instanceof Method) {
            params = ((Method) methodOrConstructor).getParameters();
        } else {
            return super.lookupParameterNames(methodOrConstructor, throwExceptionIfMissing);
        }
        String[] names = new String[params.length];
        String[] defNames = null;
        for (int i = 0; i < names.length; i++) {
            Parameter param = params[i];
            if (param.isNamePresent()) {
                names[i] = param.getName();
            } else {
                if (defNames == null) {
                    defNames = super.lookupParameterNames(methodOrConstructor, throwExceptionIfMissing);
                    if (defNames.length == 0)
                        return defNames;
                }
                names[i] = defNames[i];
            }
        }
        return names;
    }
}
