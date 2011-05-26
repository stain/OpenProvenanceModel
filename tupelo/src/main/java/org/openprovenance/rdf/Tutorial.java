package org.openprovenance.rdf;

import java.util.Set;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;

import org.apache.log4j.chainsaw.Main;
import org.tupeloproject.provenance.ProvenanceAccount;
import org.tupeloproject.provenance.ProvenanceException;
import org.tupeloproject.provenance.ProvenanceRole;
import org.tupeloproject.provenance.ProvenanceProcess;
import org.tupeloproject.provenance.ProvenanceArtifact;
import org.tupeloproject.provenance.impl.ProvenanceContextFacade;
import org.tupeloproject.rdf.Resource;
import org.tupeloproject.rdf.Triple;
import org.tupeloproject.kernel.Context;
import org.tupeloproject.kernel.UnionContext;
import org.tupeloproject.kernel.impl.ResourceContext;
import org.tupeloproject.kernel.impl.MemoryContext;
import org.tupeloproject.kernel.impl.FileContext;
import org.tupeloproject.kernel.impl.BasicLocalContext;
import org.tupeloproject.util.Xml;
import org.tupeloproject.rdf.xml.RdfXml;
import org.tupeloproject.kernel.OperatorException; 


public class Tutorial {

    public void example (String [] args) throws OperatorException, IOException, ProvenanceException {
        MemoryContext mc = new MemoryContext(); //
        
        ResourceContext rc = new ResourceContext("http://example.org/data/","/provenanceExample/");
        Context context = new UnionContext();
        context.addChild(mc);
        context.addChild(rc);
        ProvenanceContextFacade pcf = new ProvenanceContextFacade(mc);
        ProvenanceAccount account = pcf.newAccount("example account");
 
        Resource sheet = Resource.uriRef("http://example.org/data/style.xsl");
        Resource doc = Resource.uriRef("http://example.org/data/doc.xml");
 
        ProvenanceArtifact docArtifact = pcf.newArtifact("source doc", doc);
        ProvenanceArtifact sheetArtifact = pcf.newArtifact("stylesheet", sheet);
 
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        //Xml.transform(context.read(doc), context.read(sheet), outBuffer);
        ByteArrayInputStream inBuffer = new ByteArrayInputStream(outBuffer.toByteArray());

        Resource result = Resource.uriRef("http://example.org/data/result.xml");
        context.write(result, inBuffer);
 
        // the process has completed, now let's record the provenance
        ProvenanceArtifact resultArtifact = pcf.newArtifact("transform result");
        ProvenanceProcess transformProcess = pcf.newProcess("xslt transform");
 
        pcf.assertAccount(account);
        pcf.assertArtifact(sheetArtifact);
        pcf.assertArtifact(docArtifact);
        pcf.assertProcess(transformProcess);
        pcf.assertArtifact(resultArtifact);
 
        // the input document and stylesheet are two different kinds of inputs for the transform
        // process, so each has its own role
        ProvenanceRole inputDocumentRole = pcf.newRole("input document");
        ProvenanceRole stylesheetRole = pcf.newRole("stylesheet");
        ProvenanceRole outputRole = pcf.newRole("output");
 
        pcf.assertUsed(transformProcess, docArtifact, inputDocumentRole, account);
        pcf.assertUsed(transformProcess, sheetArtifact, stylesheetRole, account);
        pcf.assertGeneratedBy(resultArtifact, transformProcess, outputRole, account);


        Set<Triple> triplesToWrite = mc.getTriples();
        RdfXml.write(triplesToWrite, new FileOutputStream(new File("target/foo.xml")));
    }
}