package org.openprovenance.rdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.openprovenance.model.Account;
import org.openprovenance.model.AccountRef;
import org.openprovenance.model.Agent;
import org.openprovenance.model.Annotation;
import org.openprovenance.model.Artifact;
import org.openprovenance.model.Edge;
import org.openprovenance.model.EmbeddedAnnotation;
import org.openprovenance.model.Identifiable;
import org.openprovenance.model.Node;
import org.openprovenance.model.OPMDeserialiser;
import org.openprovenance.model.OPMFactory;
import org.openprovenance.model.OPMGraph;
import org.openprovenance.model.OPMUtilities;
import org.openprovenance.model.Process;
import org.openprovenance.model.Property;
import org.openprovenance.model.Role;
import org.openprovenance.model.Used;
import org.openprovenance.model.WasControlledBy;
import org.openprovenance.model.WasDerivedFrom;
import org.openprovenance.model.WasGeneratedBy;
import org.openprovenance.model.WasTriggeredBy;
import org.tupeloproject.kernel.Context;
import org.tupeloproject.kernel.OperatorException;
import org.tupeloproject.kernel.UnionContext;
import org.tupeloproject.kernel.impl.MemoryContext;
import org.tupeloproject.kernel.impl.ResourceContext;
import org.tupeloproject.provenance.ProvenanceAccount;
import org.tupeloproject.provenance.ProvenanceAgent;
import org.tupeloproject.provenance.ProvenanceArtifact;
import org.tupeloproject.provenance.ProvenanceControlledArc;
import org.tupeloproject.provenance.ProvenanceDerivedArc;
import org.tupeloproject.provenance.ProvenanceException;
import org.tupeloproject.provenance.ProvenanceGeneratedArc;
import org.tupeloproject.provenance.ProvenanceProcess;
import org.tupeloproject.provenance.ProvenanceRole;
import org.tupeloproject.provenance.ProvenanceTriggeredArc;
import org.tupeloproject.provenance.ProvenanceUsedArc;
import org.tupeloproject.provenance.impl.ProvenanceContextFacade;
import org.tupeloproject.provenance.impl.RdfProvenanceAccount;
import org.tupeloproject.provenance.impl.RdfProvenanceAgent;
import org.tupeloproject.provenance.impl.RdfProvenanceArc;
import org.tupeloproject.provenance.impl.RdfProvenanceArtifact;
import org.tupeloproject.provenance.impl.RdfProvenanceProcess;
import org.tupeloproject.provenance.impl.RdfProvenanceRole;
import org.tupeloproject.rdf.Resource;
import org.tupeloproject.rdf.Triple;
import org.tupeloproject.rdf.Vocabulary.Rdf;
import org.tupeloproject.rdf.xml.RdfXml;


public class OPMXml2Rdf {
    static Logger logger = Logger.getLogger(OPMXml2Rdf.class);

    static String NULL_ACCOUNT="_null";
    static String UNKNOWN_ROLE="_unknown";
    
    OPMUtilities u=new OPMUtilities();

    static OPMFactory oFactory=new OPMFactory();

    public void convert (OPMGraph graph, String filename) throws OperatorException, IOException, ProvenanceException {
        convert(graph,new FileOutputStream(new File(filename)));
    }
    
    HashMap<String,ProvenanceAccount>  accountTable=new HashMap();
    HashMap<String,ProvenanceProcess>  processTable=new HashMap();
    HashMap<String,ProvenanceArtifact> artifactTable=new HashMap();
    HashMap<String,ProvenanceAgent>    agentTable=new HashMap();
    HashMap<String,Resource>           idTable=new HashMap();

    static String OPM_ANNOTATION="http://www.ipaw.info/2007/opm#Annotation";
    static String OPM_HAS_ANNOTATION="http://www.ipaw.info/2007/opm#hasAnnotation";
    static String OPM_HAS_ACCOUNT="http://www.ipaw.info/2007/opm#hasAccount";

    Resource annotationType=Resource.uriRef(OPM_ANNOTATION);
    Resource hasAnnotation=Resource.uriRef(OPM_HAS_ANNOTATION);
    Resource hasAccount=Resource.uriRef(OPM_HAS_ACCOUNT);



    public void convert (OPMGraph graph, OutputStream out) throws OperatorException, IOException, ProvenanceException {
      

        //BasicLocalContext mc = new BasicLocalContext(); //MemoryContext
        //mc.setPath("target/foo.rdf");

        MemoryContext mc = new MemoryContext(); 
        
        ResourceContext rc = new ResourceContext("http://example.org/data/","/provenanceExample/");
        Context context = new UnionContext();
        context.addChild(mc);
        context.addChild(rc);



        ProvenanceContextFacade pcf = new ProvenanceContextFacade(mc);



        Resource nullRes=Resource.uriRef(urify(NULL_ACCOUNT));
        ProvenanceAccount nullRdfAccount=pcf.newAccount(NULL_ACCOUNT,nullRes);
        pcf.assertAccount(nullRdfAccount);
        accountTable.put(NULL_ACCOUNT,nullRdfAccount);

        if (graph.getAccounts()!=null) {
            for (Account acc: graph.getAccounts().getAccount()) {
                Resource res=Resource.resource(urify(acc.getId()));
                ProvenanceAccount rdfAccount=pcf.newAccount(acc.getId(),res);
                pcf.assertAccount(rdfAccount);
                accountTable.put(acc.getId(),rdfAccount);

                RdfProvenanceAccount a2=(RdfProvenanceAccount) rdfAccount;
                Resource subject=a2.getSubject();
                idTable.put(acc.getId(),subject);

                if (!(acc.getAnnotation().isEmpty())) {
                    mc.addTriples(triplifyAnnotations(subject,acc.getAnnotation()));
                }
            }
        }


        if (graph.getProcesses()!=null) {
            for (Process p: graph.getProcesses().getProcess()) {
                Resource res=Resource.uriRef(urify(p.getId()));
                ProvenanceProcess rdfProcess;
                rdfProcess = pcf.newProcess((String)oFactory.getLabel(p),res);

                pcf.assertProcess(rdfProcess);
                processTable.put(p.getId(),rdfProcess);


                RdfProvenanceProcess a2=(RdfProvenanceProcess) rdfProcess;
                Resource subject=a2.getSubject();
                idTable.put(p.getId(),subject);

                if (!(p.getAnnotation().isEmpty())) {
                    mc.addTriples(triplifyAnnotations(subject,p.getAnnotation()));
                }
            }
        }

        if (graph.getArtifacts()!=null) {
            for (Artifact a: graph.getArtifacts().getArtifact()) {
                Resource res=Resource.uriRef(urify(a.getId()));
                ProvenanceArtifact rdfArtifact;
                rdfArtifact = pcf.newArtifact((String)oFactory.getLabel(a),res);

                pcf.assertArtifact(rdfArtifact);
                artifactTable.put(a.getId(),rdfArtifact);


                RdfProvenanceArtifact a2=(RdfProvenanceArtifact) rdfArtifact;
                Resource subject=a2.getSubject();
                idTable.put(a.getId(),subject);

                if (!(a.getAnnotation().isEmpty())) {
                    mc.addTriples(triplifyAnnotations(subject,a.getAnnotation()));
                }
            }
        }


        if (graph.getAgents()!=null) {
            for (Agent a: graph.getAgents().getAgent()) {
                Resource res=Resource.uriRef(urify(a.getId()));
                ProvenanceAgent rdfAgent;
                rdfAgent = pcf.newAgent((String)oFactory.getLabel(a),res);

                pcf.assertAgent(rdfAgent);
                agentTable.put(a.getId(),rdfAgent);


                RdfProvenanceAgent a2=(RdfProvenanceAgent) rdfAgent;
                Resource subject=a2.getSubject();
                idTable.put(a.getId(),subject);

                if (!(a.getAnnotation().isEmpty())) {
                    mc.addTriples(triplifyAnnotations(subject,a.getAnnotation()));
                }
            }
        }



        List<Edge> edges=u.getEdges(graph);
        for (Edge e: edges) {
            List<AccountRef> accounts= e.getAccount();
            String causeId=((Node)e.getCause().getRef()).getId();
            String effectId=((Node)e.getEffect().getRef()).getId();

            if (e instanceof Used) {
                ProvenanceArtifact cause=artifactTable.get(causeId);
                ProvenanceProcess effect=processTable.get(effectId);
                Role thisRole=((Used)e).getRole();
                ProvenanceRole role;
                if (thisRole!=null) {
                    role=pcf.newRole(thisRole.getValue());
                } else {
                    role=pcf.newRole(UNKNOWN_ROLE);
                }
                if (accounts.isEmpty()) {
                    ProvenanceAccount account=accountTable.get(NULL_ACCOUNT);
                    pcf.assertUsed(effect, cause, role, account);
                } else {
                    for (AccountRef aid: accounts) {
                        ProvenanceAccount account=accountTable.get(((Account)aid.getRef()).getId());
                        pcf.assertUsed(effect, cause, role, account);
                    }
                }


                if (!(e.getAnnotation().isEmpty())) {
                    
                    Collection<ProvenanceUsedArc> used=pcf.getUsed(effect);
                    for (ProvenanceUsedArc a: used) {
                        if (a.getSink().equals(cause)
                            && a.getSource().equals(effect)) {
                            //System.out.println("Found " + a);
                            //System.out.println("Found " + ((RdfProvenanceArtifact)cause).getSubject());
                            //System.out.println("Found " + ((RdfProvenanceProcess)effect).getSubject());
                            RdfProvenanceArc rpa=(RdfProvenanceArc) a;
                            Resource subject=rpa.getSubject();
                            mc.addTriples(triplifyAnnotations(subject,e.getAnnotation()));
                            break;
                        }
                    }
                }

                if (!(thisRole.getAnnotation().isEmpty())) {
                    RdfProvenanceRole rdfRole=(RdfProvenanceRole) role;
                    Resource roleSubject=rdfRole.getSubject();
                    mc.addTriples(triplifyAnnotations(roleSubject,thisRole.getAnnotation()));
                }


            }
            else if (e instanceof WasGeneratedBy) {
                ProvenanceProcess cause=processTable.get(causeId);
                ProvenanceArtifact effect=artifactTable.get(effectId);
                Role thisRole=((WasGeneratedBy)e).getRole();
                ProvenanceRole role;
                if (thisRole!=null) {
                    role=pcf.newRole(thisRole.getValue());
                } else {
                    role=pcf.newRole(UNKNOWN_ROLE);
                }
                if (accounts.isEmpty()) {
                    ProvenanceAccount account=accountTable.get(NULL_ACCOUNT);
                    pcf.assertGeneratedBy(effect, cause, role, account);
                } else {
                    for (AccountRef aid: accounts) {
                        ProvenanceAccount account=accountTable.get(((Account)aid.getRef()).getId());
                        pcf.assertGeneratedBy(effect, cause, role, account);
                    }
                }


                Collection<ProvenanceGeneratedArc> generatedBy=pcf.getGeneratedBy(effect);
                for (ProvenanceGeneratedArc a: generatedBy) {
                    if (a.getSink().equals(cause)
                        && a.getSource().equals(effect)) {
                        RdfProvenanceArc rpa=(RdfProvenanceArc) a;
                        Resource subject=rpa.getSubject();
                        mc.addTriples(triplifyAnnotations(subject,e.getAnnotation()));
                        break;
                    }
                }

                if (!(thisRole.getAnnotation().isEmpty())) {
                    RdfProvenanceRole rdfRole=(RdfProvenanceRole) role;
                    Resource roleSubject=rdfRole.getSubject();
                    mc.addTriples(triplifyAnnotations(roleSubject,thisRole.getAnnotation()));
                }


            }
            else if (e instanceof WasTriggeredBy) {
                ProvenanceProcess cause=processTable.get(causeId);
                ProvenanceProcess effect=processTable.get(effectId);
                if (accounts.isEmpty()) {
                    ProvenanceAccount account=accountTable.get(NULL_ACCOUNT);
                    pcf.assertTriggeredBy(effect, cause, account);
                } else {
                    for (AccountRef aid: accounts) {
                        ProvenanceAccount account=accountTable.get(((Account)aid.getRef()).getId());
                        pcf.assertTriggeredBy(effect, cause, account);
                    }
                }


                if (!(e.getAnnotation().isEmpty())) {
                    Collection<ProvenanceTriggeredArc> triggered=pcf.getTriggeredBy(effect);
                    for (ProvenanceTriggeredArc a: triggered) {
                        if (a.getSink().equals(cause)
                            && a.getSource().equals(effect)) {
                            RdfProvenanceArc rpa=(RdfProvenanceArc) a;
                            Resource subject=rpa.getSubject();
                            mc.addTriples(triplifyAnnotations(subject,e.getAnnotation()));
                            break;
                        }
                    }
                }

            }
            else if (e instanceof WasDerivedFrom) {
                ProvenanceArtifact cause=artifactTable.get(causeId);
                ProvenanceArtifact effect=artifactTable.get(effectId);
                RdfProvenanceArc arc;
                if (accounts.isEmpty()) {
                    ProvenanceAccount account=accountTable.get(NULL_ACCOUNT);
                    pcf.assertDerivedFrom(effect, cause, account);
                } else {
                    for (AccountRef aid: accounts) {
                        ProvenanceAccount account=accountTable.get(((Account)aid.getRef()).getId());
                        pcf.assertDerivedFrom(effect, cause, account);
                    }
                }

                if (!(e.getAnnotation().isEmpty())) {
                    Collection<ProvenanceDerivedArc> derived=pcf.getDerivedFrom(effect);
                    for (ProvenanceDerivedArc a: derived) {
                        if (a.getAntecedent().equals(cause)
                            && a.getConsequent().equals(effect)) {
                            RdfProvenanceArc rpa=(RdfProvenanceArc) a;
                            Resource subject=rpa.getSubject();
                            mc.addTriples(triplifyAnnotations(subject,e.getAnnotation()));
                            break;
                        }
                    }
                }
            }

            else if (e instanceof WasControlledBy) {
                ProvenanceAgent cause=agentTable.get(causeId);
                ProvenanceProcess effect=processTable.get(effectId);
                ProvenanceRole role = pcf.newRole(((WasControlledBy)e).getRole().getValue());
                if (accounts.isEmpty()) {
                    ProvenanceAccount account=accountTable.get(NULL_ACCOUNT);
                    pcf.assertControlledBy(effect, cause, role, account);
                } else {
                    for (AccountRef aid: accounts) {
                        ProvenanceAccount account=accountTable.get(((Account)aid.getRef()).getId());
                        pcf.assertControlledBy(effect, cause, role, account);
                    }
                }

                if (!(e.getAnnotation().isEmpty())) {
                    Collection<ProvenanceControlledArc> controlled=pcf.getControlledBy(effect);
                    for (ProvenanceControlledArc a: controlled) {
                        if (a.getSink().equals(cause)
                            && a.getSource().equals(effect)) {
                            RdfProvenanceArc rpa=(RdfProvenanceArc) a;
                            Resource subject=rpa.getSubject();
                            mc.addTriples(triplifyAnnotations(subject,e.getAnnotation()));
                            break;
                        }
                    }
                }

                Role thisRole=((WasControlledBy)e).getRole();
                if (!(thisRole.getAnnotation().isEmpty())) {
                    RdfProvenanceRole rdfRole=(RdfProvenanceRole) role;
                    Resource roleSubject=rdfRole.getSubject();
                    mc.addTriples(triplifyAnnotations(roleSubject,thisRole.getAnnotation()));
                }
            }
        }


        if (graph.getAnnotations()!=null) {
            List<Annotation> annotations=graph.getAnnotations().getAnnotation();
            for (Annotation ann: annotations) {
                mc.addTriples(triplifyAnnotation(ann));
            }
        }



        Set<Triple> triplesToWrite = mc.getTriples();
        RdfXml.write(triplesToWrite, out);

    }

    public List<Triple> triplifyAnnotation(Annotation annotation) {
        String id=(((Identifiable)annotation.getLocalSubject()).getId());
        Resource subject=idTable.get(id);
        List<Triple> triples=new LinkedList();
        if (subject!=null) {
            addTriplesForAnnotation(subject,annotation,triples);
        } else {
            logger.warn("$$$$$$$$$$$$$$$ " + id + ": no rdf subject!");
        }
        return triples;
    }

    public List<Triple> triplifyAnnotations(Resource subject, List<JAXBElement<? extends EmbeddedAnnotation>> annotations) {
        List<Triple> triples=new LinkedList();
        for (JAXBElement<? extends EmbeddedAnnotation> jann: annotations) {
            EmbeddedAnnotation ann=jann.getValue();
            addTriplesForAnnotation(subject,ann,triples);
        }
        return triples;
    }


    public void addTriplesForAnnotation(Resource subject, EmbeddedAnnotation ann, List<Triple> triples) {

        oFactory.expandAnnotation(ann);

        Resource annotationInRdf;
        if (ann.getId()!=null) {
            annotationInRdf=Resource.uriRef(urify(ann.getId()));
        } else {
            annotationInRdf=Resource.uriRef();
        }
        Triple t1=Triple.create(subject,
                                hasAnnotation,
                                annotationInRdf);


        triples.add(t1);

        Triple t2=Triple.create(annotationInRdf,
                                Rdf.TYPE,
                                annotationType);

        triples.add(t2);


        for (Property prop: ann.getProperty()) {
            Resource predicate=Resource.uriRef(prop.getKey());
            Resource value=makeLiteral(prop.getValue());

            
            Triple t=Triple.create(annotationInRdf,
                                   predicate,
                                   value);

            for (AccountRef aid: ann.getAccount()) {
                ProvenanceAccount account=accountTable.get(((Account)aid.getRef()).getId());
                RdfProvenanceAccount a2=(RdfProvenanceAccount) account;
                Resource accountSubject=a2.getSubject();
                    
                Triple t3=Triple.create(annotationInRdf,
                                        hasAccount,
                                        accountSubject);
                triples.add(t3);
                    
            }

            triples.add(t);
        }

        List<Triple> moreTriples=triplifyAnnotations(annotationInRdf,
                                                     ann.getAnnotation());
        triples.addAll(moreTriples);

    }


    public Resource makeLiteral(Object value) {
        if (value instanceof String) return Resource.literal((String)value);
        if (value instanceof Double) return Resource.literal((Double)value);
        if (value instanceof Float) return Resource.literal((Float)value);
        if (value instanceof Integer) return Resource.literal((Integer)value);
        System.out.println("Foudn " + value);
        System.out.println("Foudn " + value.getClass());
        return null;
    }

    public static String URI_PREFIX="id:";

    public String urify(String id) {
        return  URI_PREFIX + id.replaceAll("#","_");
    }


    public void convert (String inFilename, String outFilename) throws OperatorException, IOException, JAXBException, ProvenanceException {
        OPMDeserialiser deserial=OPMDeserialiser.getThreadOPMDeserialiser();
        OPMGraph graph=deserial.deserialiseOPMGraph(new File(inFilename));
        convert(graph,outFilename);
    }
    

    public static void main(String [] args) throws OperatorException, IOException, JAXBException, ProvenanceException {
        if ((args==null) || (args.length!=2)) {
            System.out.println("Usage: opmxml2rdf fileIn fileOut");
            return;
        }
        OPMXml2Rdf converter=new OPMXml2Rdf();
        converter.convert(args[0],args[1]);
    }


    
}