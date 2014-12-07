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
package com.sandflow.smpte.register.importer;

import com.sandflow.smpte.register.exception.DuplicateEntryException;
import com.sandflow.smpte.register.exception.InvalidEntryException;
import com.sandflow.smpte.register.TypeEntry;
import com.sandflow.smpte.register.TypesRegister;
import com.sandflow.smpte.util.ExcelCSVParser;
import com.sandflow.smpte.util.UL;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

/**
 *
 * @author Pierre-Anthony Lemieux (pal@sandflow.com)
 */
public class ExcelTypesRegister {

    public final static String SMPTE_NAMESPACE = "http://www.smpte-ra.org/reg/2003/2012";

    private final static Logger LOGGER = Logger.getLogger(ExcelElementsRegister.class.getName());

    static final UL AUID_TYPE_UL = UL.fromURN("urn:smpte:ul:060E2B34.01040101.01030100.00000000");
    static final UL EIDRIdentifierType_TYPE_UL = UL.fromURN("urn:smpte:ul:060e2b34.01040101.01200800.00000000");
    static final UL CanonicalDOINameType_TYPE_UL = UL.fromURN("urn:smpte:ul:060e2b34.01040101.01200700.00000000");
    static final UL BAD_UUID_TYPE_UL = UL.fromURN("urn:smpte:ul:060E2B34.01040101.04011100.00000000");

    static public TypesRegister fromXLS(InputStream xlsfile) throws ExcelCSVParser.SyntaxException, IOException, InvalidEntryException, DuplicateEntryException, URISyntaxException {
        InputStreamReader isr;

        try {

            /* this should really never happen */
            isr = new InputStreamReader(xlsfile, "US-ASCII");

        } catch (UnsupportedEncodingException ex) {

            throw new RuntimeException(ex);

        }

        BufferedReader br = new BufferedReader(isr);

        ExcelCSVParser p = new ExcelCSVParser(br);

        HashMap<String, Integer> c = new HashMap<>();

        TypesRegister reg = new TypesRegister();

        TypeEntry lasttype = null;

        /* BUG: there is no StrongReferenceNameValue type */
        TypeEntry entry = new TypeEntry();
        entry.setKind(TypeEntry.Kind.LEAF);
        entry.setDeprecated(false);
        entry.setSymbol("StrongReferenceNameValue");
        entry.setUL(UL.fromURN("urn:smpte:ul:060E2B34.01040101.05022900.00000000"));
        entry.setName("StrongReferenceNameValue");
        entry.setTypeKind(TypeEntry.STRONGREF_TYPEKIND);
        entry.setBaseType(UL.fromDotValue("06.0E.2B.34.02.7F.01.01.0D.01.04.01.01.1F.01.00"));
        entry.setNamespaceName(new URI(SMPTE_NAMESPACE));
        reg.addEntry(entry);


        for (AbstractList<String> fields; (fields = p.getLine()) != null;) {

            if ("_rxi".equalsIgnoreCase(fields.get(0))) {

                /* read headers */
                for (int i = 0; i < fields.size(); i++) {
                    c.put(fields.get(i), i);
                }

            } else if (fields.get(0) == null
                    || (!fields.get(0).startsWith("_"))) {

                if (lasttype != null && "Link".equalsIgnoreCase(fields.get(c.get("n:node")))) {

                    /* CHILD */
                    TypeEntry.Facet f = new TypeEntry.Facet();

                    f.setApplications(fields.get(c.get("i:app")));

                    f.setNotes(fields.get(c.get("i:notes")));

                    f.setDeprecated(!("No".equalsIgnoreCase(fields.get(c.get("n:deprecated")))));

                    f.setDefinition(fields.get(c.get("n:detail")));

                    if (TypeEntry.RECORD_TYPEKIND.equals(lasttype.getTypeKind())) {

                        f.setSymbol(fields.get(c.get("n:sym")));
                        f.setName(fields.get(c.get("n:name")));

                        if (fields.get(c.get("n:type_urn")) != null) {
                            f.setType(UL.fromURN(fields.get(c.get("n:type_urn"))));
                        } else {
                            throw new InvalidEntryException(
                                    String.format(
                                            "Missing n:type_urn from Record face %s",
                                            fields.get(c.get("a:urn"))
                                    )
                            );
                        }

                    } else if (TypeEntry.ENUMERATION_TYPEKIND.equals(lasttype.getTypeKind())) {

                        if (lasttype.getBaseType().equals(AUID_TYPE_UL)) {
                            f.setValue(UL.fromDotValue(fields.get(c.get("n:urn"))).toString());
                        } else {
                            f.setSymbol(fields.get(c.get("n:sym")));
                            f.setName(fields.get(c.get("n:name")));
                            f.setValue(fields.get(c.get("n:value")));
                        }

                    } else if (TypeEntry.WEAKREF_TYPEKIND.equals(lasttype.getTypeKind())) {

                        /* BUG: skip Weak Reference children since there are apparently many
                        bugs with them.
                        */
                        
                        continue;

                    }


                    /* Cannot use "n:parent_urn" since it is wrong for Weak Reference entries  */
                    lasttype.getFacets().add(f);

                } else {

                    /* NODE and LEAF */
                    TypeEntry type = new TypeEntry();

                    if (fields.get(c.get("n:urn")) == null) {

                        throw new InvalidEntryException("Invalid Type UL:"
                                + fields.get(c.get("n:urn"))
                                + " / "
                                + fields.get(c.get("n:sym"))
                        );
                    }

                    type.setUL(UL.fromDotValue(fields.get(c.get("n:urn"))));

                    /* BUG: Bad UUID type */
                    if (type.getUL().isClass14() || type.getUL().isClass13() || type.getUL().isClass15() || type.getUL().equals(BAD_UUID_TYPE_UL)) {
                        lasttype = null;
                        continue;
                    }

                    type.setName(fields.get(c.get("n:name")));

                    type.setDefinition(fields.get(c.get("n:detail")));

                    type.setApplications(fields.get(c.get("i:app")));

                    type.setNotes(fields.get(c.get("i:notes")));

                    type.setDeprecated(!("No".equalsIgnoreCase(fields.get(c.get("n:deprecated")))));

                    type.setSymbol(fields.get(c.get("n:sym")));

                    type.setDefiningDocument(fields.get(c.get("n:docs")));

                    try {
                        if (fields.get(c.get("n:ns_uri")) != null) {

                            type.setNamespaceName(new URI(fields.get(c.get("n:ns_uri"))));

                        } else {
                            if (type.getUL().getValueOctet(8) <= 12) {
                                type.setNamespaceName(new URI(SMPTE_NAMESPACE));
                            } else {
                                type.setNamespaceName(new URI(SMPTE_NAMESPACE + "/" + type.getUL().getValueOctet(8) + "/" + type.getUL().getValueOctet(9)));
                            }

                        }
                    } catch (URISyntaxException ex) {
                        LOGGER.warning(
                                String.format(
                                        "Invalid URI %s at Type %s",
                                        fields.get(c.get("n:ns_uri")),
                                        type.getUL()
                                ));
                    }

                    if ("Leaf".equalsIgnoreCase(fields.get(c.get("n:node")))) {

                        /* LEAF */
                        type.setKind(TypeEntry.Kind.LEAF);

                        String kind = fields.get(c.get("n:kind"));

                        String qualif = fields.get(c.get("n:qualif"));

                        UL target_urn = null;

                        if (fields.get(c.get("n:target_urn")) != null) {
                            target_urn = UL.fromURN(fields.get(c.get("n:target_urn")));

                            if (target_urn == null) {
                                target_urn = UL.fromDotValue(fields.get(c.get("n:target_urn")));
                            }
                        }

                        if ("integer".equalsIgnoreCase(kind)) {

                            /* INTEGER */
                            type.setTypeKind(TypeEntry.INTEGER_TYPEKIND);
                            type.setTypeSize(Long.parseLong(fields.get(c.get("n:qualif"))));

                            if ("True".equals(fields.get(c.get("n:value")))) {
                                type.getTypeQualifiers().add(TypeEntry.TypeQualifiers.isSigned);
                            }
                            type.getTypeQualifiers().add(TypeEntry.TypeQualifiers.isNumeric);

                            if (target_urn != null) {

                                throw new InvalidEntryException(
                                        String.format(
                                                "Integer type %s has n:target_urn defined.",
                                                type.getUL()
                                        )
                                );

                            }

                        } else if ("rename".equalsIgnoreCase(kind)) {

                            type.setTypeKind(TypeEntry.RENAME_TYPEKIND);

                            if (target_urn != null) {

                                type.setBaseType(target_urn);

                            } else if (type.getUL().equals(EIDRIdentifierType_TYPE_UL)) {

                                /* BUG: EIDRIdentifierType is missing target_urn */
                                type.setBaseType(CanonicalDOINameType_TYPE_UL);
                            } else {
                                throw new InvalidEntryException(
                                        String.format(
                                                "Rename type %s is missing n:target_urn.",
                                                type.getUL()
                                        )
                                );
                            }

                        } else if ("record".equalsIgnoreCase(kind)) {

                            type.setTypeKind(TypeEntry.RECORD_TYPEKIND);

                        } else if ("array".equalsIgnoreCase(kind)) {

                            

                            if (target_urn != null) {

                                type.setBaseType(target_urn);

                            } else {
                                throw new InvalidEntryException(
                                        String.format(
                                                "Type %s is missing n:target_urn.",
                                                type.getUL()
                                        )
                                );
                            }

                            if ("fixed".equals(qualif)) {
                                
                                type.setTypeKind(TypeEntry.FIXEDARRAY_TYPEKIND);

                                type.setTypeSize(Long.parseLong(fields.get(c.get("n:minOccurs"))));

                                type.getTypeQualifiers().add(TypeEntry.TypeQualifiers.isSizeImplicit);
                                type.getTypeQualifiers().add(TypeEntry.TypeQualifiers.isOrdered);
                                type.getTypeQualifiers().add(TypeEntry.TypeQualifiers.isCountImplicit);

                            } else if ("varying".equals(qualif)) {

                                type.setTypeKind(TypeEntry.ARRAY_TYPEKIND);
                                
                                type.setTypeSize(0L);
                                type.getTypeQualifiers().add(TypeEntry.TypeQualifiers.isOrdered);

                            } else if ("strong".equals(qualif)) {
                                
                                type.setTypeKind(TypeEntry.ARRAY_TYPEKIND);

                                if (type.getSymbol().startsWith("StrongReferenceVector")) {

                                    String symbol = "StrongReference"
                                            + type.getSymbol().substring("StrongReferenceVector".length());

                                    TypeEntry realtype = reg.getEntryBySymbol(symbol, type.getNamespaceName());

                                    type.setBaseType(realtype.getUL());
                                }

                                type.setTypeSize(0L);
                                type.getTypeQualifiers().add(TypeEntry.TypeQualifiers.isOrdered);

                            } else if ("weak".equals(qualif)) {
                                
                                type.setTypeKind(TypeEntry.ARRAY_TYPEKIND);
                                
                                if (type.getSymbol().startsWith("WeakReferenceVector")) {

                                    String symbol = "WeakReference"
                                            + type.getSymbol().substring("WeakReferenceVector".length());

                                    TypeEntry realtype = reg.getEntryBySymbol(symbol, type.getNamespaceName());

                                    type.setBaseType(realtype.getUL());
                                }

                                type.setTypeSize(0L);
                                type.getTypeQualifiers().add(TypeEntry.TypeQualifiers.isOrdered);

                            } else {

                                throw new InvalidEntryException(
                                        String.format(
                                                "Array type %s has unknown n:qualif.",
                                                type.getUL()
                                        )
                                );

                            }

                        } else if ("character".equalsIgnoreCase(kind)) {

                            type.setTypeKind(TypeEntry.CHARACTER_TYPEKIND);
                            type.setTypeSize(Long.parseLong(fields.get(c.get("n:qualif"))));

                        } else if ("string".equalsIgnoreCase(kind)) {

                            type.setTypeKind(TypeEntry.STRING_TYPEKIND);

                            if (target_urn != null) {

                                type.setBaseType(target_urn);

                            } else {
                                throw new InvalidEntryException(
                                        String.format(
                                                "Type %s is missing n:target_urn.",
                                                type.getUL()
                                        )
                                );
                            }

                            type.getTypeQualifiers().add(TypeEntry.TypeQualifiers.isCountImplicit);
                            type.getTypeQualifiers().add(TypeEntry.TypeQualifiers.isOrdered);
                            type.getTypeQualifiers().add(TypeEntry.TypeQualifiers.isSizeImplicit);

                        } else if ("enumeration".equalsIgnoreCase(kind)) {

                            type.setTypeKind(TypeEntry.ENUMERATION_TYPEKIND);

                            if (target_urn != null) {

                                type.setBaseType(target_urn);

                            } else {
                                throw new InvalidEntryException(
                                        String.format(
                                                "Type %s is missing n:target_urn.",
                                                type.getUL()
                                        )
                                );
                            }

                        } else if ("extendible".equalsIgnoreCase(kind)) {

                            type.setTypeKind(TypeEntry.ENUMERATION_TYPEKIND);

                            type.setBaseType(UL.fromURN("urn:smpte:ul:060E2B34.01040101.01030100.00000000"));

                        } else if ("set".equalsIgnoreCase(kind)) {

                            if ("global".equals(qualif)) {
                                /* BUG: missing Global reference types */
                            }

                            type.setTypeKind(TypeEntry.SET_TYPEKIND);
                            type.setTypeSize(0L);

                            type.getTypeQualifiers().add(TypeEntry.TypeQualifiers.isSizeImplicit);
                            type.getTypeQualifiers().add(TypeEntry.TypeQualifiers.isIdentified);

                            /* BUG: StrongReferenceSets do no have entries of type Strong Reference */
                            if (type.getSymbol().startsWith("StrongReferenceSet")) {

                                String symbol = "StrongReference"
                                        + type.getSymbol().substring("StrongReferenceSet".length());

                                TypeEntry realtype = reg.getEntryBySymbol(symbol, type.getNamespaceName());
                                
                                if (realtype == null) {
                                    throw new InvalidEntryException(
                                        String.format(
                                                "Could not find %s.",
                                                symbol
                                        )
                                ); 
                                }

                                type.setBaseType(realtype.getUL());
                            } else if (type.getSymbol().startsWith("WeakReferenceSet")) {

                                String symbol = "WeakReference"
                                        + type.getSymbol().substring("WeakReferenceSet".length());

                                TypeEntry realtype = reg.getEntryBySymbol(symbol, type.getNamespaceName());

                                type.setBaseType(realtype.getUL());
                            } else if (target_urn != null) {

                                type.setBaseType(target_urn);

                            } else {
                                throw new InvalidEntryException(
                                        String.format(
                                                "Type %s is missing n:target_urn.",
                                                type.getUL()
                                        )
                                );
                            }

                        } else if ("stream".equalsIgnoreCase(kind)) {

                            type.setTypeKind(TypeEntry.STREAM_TYPEKIND);

                        } else if ("indirect".equalsIgnoreCase(kind)) {

                            type.setTypeKind(TypeEntry.INDIRECT_TYPEKIND);

                        } else if ("opaque".equalsIgnoreCase(kind)) {

                            type.setTypeKind(TypeEntry.OPAQUE_TYPEKIND);

                        } else if ("formal".equalsIgnoreCase(kind)) {

                            /* BUG: what is 'formal' */
                            continue;

                        } else if ("reference".equalsIgnoreCase(kind)) {

                            type.setBaseType(target_urn);

                            if ("strong".equalsIgnoreCase(qualif)) {
                                type.setTypeKind(TypeEntry.STRONGREF_TYPEKIND);

                            } else if ("weak".equalsIgnoreCase(qualif)) {
                                type.setTypeKind(TypeEntry.WEAKREF_TYPEKIND);
                            } else {
                                throw new InvalidEntryException(
                                        String.format(
                                                "Array type %s has unknown n:qualif.",
                                                type.getUL()
                                        )
                                );
                            }

                        } else {
                            throw new InvalidEntryException(
                                    String.format(
                                            "Type %s is missing n:kind.",
                                            type.getUL()
                                    )
                            );
                        }

                        lasttype = type;

                    } else {

                        /* NODE */
                        type.setKind(TypeEntry.Kind.NODE);

                        lasttype = null;

                    }

                    reg.addEntry(type);

                }

            }

        }

        return reg;

    }

    public static void main(String args[]) throws FileNotFoundException, ExcelCSVParser.SyntaxException, IOException, InvalidEntryException, JAXBException, DuplicateEntryException, URISyntaxException {
        FileInputStream f = new FileInputStream("\\\\SERVER\\Business\\sandflow-consulting\\projects\\imf\\regxml\\register-format\\input\\types-smpte-ra-frozen-20140304.2118.csv");

        TypesRegister reg = ExcelTypesRegister.fromXLS(f);

        BufferedWriter writer = null;
        writer = new BufferedWriter(new FileWriter("\\\\SERVER\\Business\\sandflow-consulting\\projects\\imf\\regxml\\register-format\\output\\types-register.xml"));

        JAXBContext ctx = JAXBContext.newInstance(TypesRegister.class);

        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(reg, writer);
        writer.close();

        File baseDir = new File("C:\\Users\\pal\\Documents\\");

        ctx.generateSchema(new SchemaOutputResolver() {

            @Override
            public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
                return new StreamResult(new File(baseDir, suggestedFileName));
            }
        });

    }

    HashMap<UL, TypeEntry> types = new HashMap<>();

}