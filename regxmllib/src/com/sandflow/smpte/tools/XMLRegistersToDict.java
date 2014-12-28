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
package com.sandflow.smpte.tools;

import com.sandflow.smpte.register.ElementsRegister;
import com.sandflow.smpte.register.GroupsRegister;
import com.sandflow.smpte.register.TypesRegister;
import com.sandflow.smpte.register.exception.DuplicateEntryException;
import com.sandflow.smpte.register.exception.InvalidEntryException;
import com.sandflow.smpte.regxml.dict.MetaDictionary;
import com.sandflow.smpte.regxml.dict.MetaDictionaryGroup;
import static com.sandflow.smpte.regxml.dict.importer.XMLRegistryImporter.fromRegister;
import com.sandflow.smpte.util.ExcelCSVParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;

/**
 *
 * @author Pierre-Anthony Lemieux (pal@sandflow.com)
 */
public class XMLRegistersToDict {

    private final static String USAGE = "Converts XML Registers to RegXML metadictionary.\n"
            + "  Usage: XMLRegistersToDict -e elementsregspath\n"
            + "                            -l labelsregpath\n"
            + "                            -g groupsregpath\n"
            + "                            -t typesregpath\n"
            + "                            outputdir\n"
            + "         XMLRegistersToDict -?";

    public static void main(String[] args) throws FileNotFoundException, ExcelCSVParser.SyntaxException, JAXBException, IOException, InvalidEntryException, DuplicateEntryException, Exception {
        if (args.length != 9
                || "-?".equals(args[0])
                || (!"-e".equals(args[0]))
                || (!"-l".equals(args[2]))
                || (!"-g".equals(args[4]))
                || (!"-t".equals(args[6]))) {

            System.out.println(USAGE);

            return;
        }
        
        /* mute logging */
        /* TODO: add switch to enable warnings */
        //Logger.getLogger("").setLevel(Level.OFF);

        FileReader fe = new FileReader(args[1]);
        FileReader fg = new FileReader(args[5]);
        FileReader ft = new FileReader(args[7]);

        ElementsRegister ereg = ElementsRegister.fromXML(fe);
        GroupsRegister greg = GroupsRegister.fromXML(fg);
        TypesRegister treg = TypesRegister.fromXML(ft);

        MetaDictionaryGroup mds = fromRegister(treg, greg, ereg);
        
        for(MetaDictionary md : mds.getDictionaries()) {
            
            /* create file name from the Scheme URI */
            
            String fname = md.getSchemeURI().getAuthority() + md.getSchemeURI().getPath();
            
            File f = new File(args[8], fname.replaceAll("[^a-zA-Z0-9]", "-") + ".xml");
         
            md.toXML(new FileWriter(f));
        }
        
        

    }
}
