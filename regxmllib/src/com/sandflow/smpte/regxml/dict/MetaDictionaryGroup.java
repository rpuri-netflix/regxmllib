/*
 * Copyright (c) 2014, Pierre-Anthony Lemieux (pal@sandflow.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.sandflow.smpte.regxml.dict;

import com.sandflow.smpte.regxml.definition.ClassDefinition;
import com.sandflow.smpte.regxml.definition.Definition;
import com.sandflow.smpte.util.AUID;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 *
 * @author Pierre-Anthony Lemieux (pal@sandflow.com)
 */
public class MetaDictionaryGroup implements DefinitionResolver {

    final private HashMap<URI, MetaDictionary> dicts = new HashMap<>();

    @Override
    public Definition getDefinition(AUID auid) {
        Definition def = null;

        for (MetaDictionary md : dicts.values()) {
            if ((def = md.getDefinition(auid)) != null) {
                break;
            }
        }

        return def;
    }

    public void addDictionary(MetaDictionary metadictionary) throws IllegalDictionaryException {
        MetaDictionary md = dicts.get(metadictionary.getSchemeURI());

        if (md == null) {
            dicts.put(metadictionary.getSchemeURI(), metadictionary);
        } else {
            throw new IllegalDictionaryException("Metadictionary already present in group.");
        }

    }

    /* TODO: not necessarily cool to automatically create a metadictionary */
    public void addDefinition(Definition def) throws IllegalDefinitionException {
        MetaDictionary md = dicts.get(def.getNamespace());

        if (md == null) {
            md = new MetaDictionary(def.getNamespace());

            dicts.put(md.getSchemeURI(), md);
        }

        md.add(def);
    }

    public Collection<MetaDictionary> getDictionaries() {
        return dicts.values();
    }

    @Override
    public Collection<AUID> getSubclassesOf(ClassDefinition parent) {

        ArrayList<AUID> subclasses = new ArrayList<>();

        for (MetaDictionary md : dicts.values()) {
            
            Collection<AUID> defs = md.getSubclassesOf(parent);
            
            if (defs != null) subclasses.addAll(defs);

        }

        return subclasses;
    }

    @Override
    public Collection<AUID> getMembersOf(ClassDefinition parent) {
        ArrayList<AUID> members = new ArrayList<>();

        for (MetaDictionary md : dicts.values()) {
            
            Collection<AUID> defs = md.getMembersOf(parent);

            if (defs != null) members.addAll(defs);
        }

        return members;
    }

}
