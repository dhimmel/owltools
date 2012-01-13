package owltools.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Obo2OWLConstants.Obo2OWLVocabulary;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.graph.OWLQuantifiedProperty.Quantifier;
import owltools.profile.Profiler;

/**
 * Wraps one or more OWLOntology objects providing convenient OBO-like operations 
 * 
 * <h3>Capabilities</h3>
 * <ul>
 * <li>convenience methods for OBO-like properties such as synonyms, textual definitions, obsoletion, replaced_by
 * <li>simple graph-like operations over ontologies, including reachability/closure queries that respect the OWL semantics
 * </ul>
 *
 * <h3>Data model</h3>
 * 
 * An instance of an OWLGraphWrapper wraps one or more {@link org.semanticweb.owlapi.model.OWLOntology} objects. One of these is designated
 * the <i>sourceOntology</i>, the others are designated <i>support ontologies</i>
 * (see {@link #getSourceOntology()} and {@link #getSupportOntologySet()}).
 * The source ontology may import the support
 * ontologies, but this is optional. Most OWLGraphWrapper methods operate over the union of the source ontology
 * and support ontologies. This is particularly useful for working with OBO Library ontologies, where axioms
 * connecting ontologies may be available as separate ontologies. 
 * 
 *  <h3>Graph operations</h3>
 *
 * See {@link owltools.graph}
 * 
 * <h3>Fetching objects</h3>
 * 
 * This wrapper provides convenience methods for fetching objects by OBO-Style IDs, IRIs or by labels.
 * Note that unlike the get* calls on {@link OWLDataFactory} objects, these only return an object if it
 * has been declared in either the source ontology or a support ontology.
 * 
 * See for example
 * 
 * <ul>
 *  <li>{@link #getOWLClass(String id)}
 *  <li>{@link #getOWLClassByIdentifier(String id)}
 *  <li>{@link #getOWLObjectByLabel(String label)}
 * </ul>
 * <h3>OBO Metadata</h3>
 * 
 * <h4>OBO-style identifiers</h4>
 * 
 * This class accepts the use of OBO-Format style identifiers in some contexts, e.g. GO:0008150
 * 
 * See methods such as
 * <ul>
 *  <li>{@link #getOWLClassByIdentifier(String id)}
 * </ul>
 * 
 * <h4>Textual metadata</h4>
 * 
 *  Documentation to follow....
 *
 * @see OWLGraphUtil
 * @author cjm
 *
 */
public class OWLGraphWrapper {

	private static Logger LOG = Logger.getLogger(OWLGraphWrapper.class);

	public static final String DEFAULT_IRI_PREFIX = "http://purl.obolibrary.org/obo/";

	@Deprecated
	OWLOntology ontology; // this is the ontology used for querying. may be the merge of sourceOntology+closure

	OWLOntology sourceOntology; // graph is seeded from this ontology.

	Set<OWLOntology> supportOntologySet = new HashSet<OWLOntology>();

	OWLDataFactory dataFactory;
	OWLOntologyManager manager;
	OWLReasoner reasoner = null;
	Config config = new Config();

	private Map<OWLObject,Set<OWLGraphEdge>> edgeBySource;
	private Map<OWLObject,Set<OWLGraphEdge>> edgeByTarget;
	public Map<OWLObject,Set<OWLGraphEdge>> inferredEdgeBySource = null; // public to serialize
	private Map<OWLObject,Set<OWLGraphEdge>> inferredEdgeByTarget = null;

	// used to store mappings child->parent, where
	// parent = UnionOf( ..., child, ...)
	private Map<OWLObject,Set<OWLObject>> extraSubClassOfEdges = null;

	private Profiler profiler = new Profiler();


	/**
	 * Configuration options. These are typically specific to a
	 * OWLGraphWrapper instance.
	 *
	 */
	public class Config {
		// by default the graph closure includes only named entities
		public boolean isIncludeClassExpressionsInClosure = true;

		// by default we do not follow complement of - TODO
		public boolean isFollowComplementOfInClosure = false;

		public boolean isCacheClosure = true;
		public boolean isMonitorMemory = false;

		// if set to non-null, this constrains graph traversal. TODO
		public Set<OWLQuantifiedProperty> graphEdgeIncludeSet = null;
		public Set<OWLQuantifiedProperty> graphEdgeExcludeSet = null;
		public OWLClass excludeMetaClass = null;

		@Deprecated
		public void excludeProperty(OWLObjectProperty p) {
			if (graphEdgeExcludeSet == null)
				graphEdgeExcludeSet = new HashSet<OWLQuantifiedProperty>();
			graphEdgeExcludeSet.add(new OWLQuantifiedProperty(p, null));
		}
	}

	/**
	 * Create a new wrapper for an OWLOntology
	 * 
	 * @param ontology 
	 * 
	 * @throws OWLOntologyCreationException 
	 * @throws UnknownOWLOntologyException 
	 */
	public OWLGraphWrapper(OWLOntology ontology) throws UnknownOWLOntologyException, OWLOntologyCreationException {
		super();
		manager = OWLManager.createOWLOntologyManager();
		dataFactory = manager.getOWLDataFactory();
		sourceOntology = ontology;
		manager.getOntologyFormat(ontology);
	}

	/**
	 * Create a new wrapper for an OWLOntology
	 * 
	 * @param ontology
	 * @param isMergeImportClosure
	 * @throws UnknownOWLOntologyException
	 * @throws OWLOntologyCreationException
	 * @deprecated
	 */
	@Deprecated
	public OWLGraphWrapper(OWLOntology ontology, boolean isMergeImportClosure) throws UnknownOWLOntologyException, OWLOntologyCreationException {
		super();
		manager = OWLManager.createOWLOntologyManager();
		dataFactory = manager.getOWLDataFactory();
		if (isMergeImportClosure) {
			System.out.println("setting source ontology:"+ontology);
			this.sourceOntology = ontology;
			// the query ontology is the source ontology plus the imports closure
			useImportClosureForQueries();

		}
		else {
			this.sourceOntology = ontology;
		}
		manager.getOntologyFormat(ontology);
	}

	/**
	 * creates a new {@link OWLOntology} as the source ontology
	 * 
	 * @param iri
	 * @throws OWLOntologyCreationException
	 */
	public OWLGraphWrapper(String iri) throws OWLOntologyCreationException {
		super();
		manager = OWLManager.createOWLOntologyManager();
		dataFactory = manager.getOWLDataFactory();
		sourceOntology = manager.createOntology(IRI.create(iri));
	}

	/**
	 * adds an imports declaration between the source ontology and extOnt
	 * 
	 * @param extOnt
	 */
	public void addImport(OWLOntology extOnt) {
		AddImport ai = new AddImport(getSourceOntology(), dataFactory.getOWLImportsDeclaration(extOnt.getOntologyID().getOntologyIRI()));
		manager.applyChange(ai);
	}

	/**
	 * if called, copies all axioms from import closure into query ontology.
	 * 
	 * @throws UnknownOWLOntologyException
	 * @throws OWLOntologyCreationException
	 */
	@Deprecated
	public void useImportClosureForQueries() throws UnknownOWLOntologyException, OWLOntologyCreationException {
		this.ontology = 
			manager.createOntology(sourceOntology.getOntologyID().getOntologyIRI(), sourceOntology.getImportsClosure());
	}

	@Deprecated
	public void addQueryOntology(OWLOntology extOnt) throws OWLOntologyCreationException {
		Set<OWLAxiom> axioms = ontology.getAxioms();
		axioms.addAll(extOnt.getAxioms());
		this.ontology = 
			manager.createOntology(axioms, sourceOntology.getOntologyID().getOntologyIRI());	
	}

	/**
	 * Adds all axioms from extOnt into source ontology
	 * 
	 * @param extOnt
	 * @throws OWLOntologyCreationException
	 */
	public void mergeOntology(OWLOntology extOnt) throws OWLOntologyCreationException {
		for (OWLAxiom axiom : extOnt.getAxioms()) {
			manager.applyChange(new AddAxiom(sourceOntology, axiom));
		}
		for (OWLImportsDeclaration oid: extOnt.getImportsDeclarations()) {
			manager.applyChange(new AddImport(sourceOntology, oid));
		}
	}
	
	public void mergeOntology(OWLOntology extOnt, boolean isRemoveFromSupportList) throws OWLOntologyCreationException {
		mergeOntology(extOnt);
		if (isRemoveFromSupportList) {
			this.supportOntologySet.remove(extOnt);
		}
	}
	
	public void mergeSupportOntology(String ontologyIRI, boolean isRemoveFromSupportList) throws OWLOntologyCreationException {
		OWLOntology extOnt = null;
		for (OWLOntology ont : this.supportOntologySet) {
			if (ont.getOntologyID().getOntologyIRI().toString().equals(ontologyIRI)) {
				extOnt = ont;
				break;
			}
		}
		
		mergeOntology(extOnt);
		if (isRemoveFromSupportList) {
			this.supportOntologySet.remove(extOnt);
		}
	}

	public void mergeSupportOntology(String ontologyIRI) throws OWLOntologyCreationException {
		mergeSupportOntology(ontologyIRI, true);
	}

	@Deprecated
	public OWLOntology getOntology() {
		return ontology;
	}


	@Deprecated
	public void setOntology(OWLOntology ontology) {
		this.ontology = ontology;
	}

	/**
	 * Every OWLGraphWrapper objects wraps zero or one source ontologies.
	 * 
	 * @return ontology
	 */
	public OWLOntology getSourceOntology() {
		return sourceOntology;
	}

	public void setSourceOntology(OWLOntology sourceOntology) {
		this.sourceOntology = sourceOntology;
	}



	public Profiler getProfiler() {
		return profiler;
	}

	public void setProfiler(Profiler profiler) {
		this.profiler = profiler;
	}



	public OWLReasoner getReasoner() {
		return reasoner;
	}

	/**
	 * @param reasoner
	 */
	public void setReasoner(OWLReasoner reasoner) {
		this.reasoner = reasoner;
	}

	/**
	 * all operations are over a set of ontologies - the source ontology plus
	 * any number of supporting ontologies. The supporting ontologies may be drawn
	 * from the imports closure of the source ontology, although this need not be the case.
	 * 
	 * @return set of support ontologies
	 */
	public Set<OWLOntology> getSupportOntologySet() {
		return supportOntologySet;
	}

	public void setSupportOntologySet(Set<OWLOntology> supportOntologySet) {
		this.supportOntologySet = supportOntologySet;
	}

	public void addSupportOntology(OWLOntology o) {
		this.supportOntologySet.add(o);
	}
	public void removeSupportOntology(OWLOntology o) {
		this.supportOntologySet.remove(o);
	}

	/**
	 * Each ontology in the import closure of the source ontology is added to
	 * the list of support ontologies
	 * 
	 */
	public void addSupportOntologiesFromImportsClosure() {
		for (OWLOntology o : sourceOntology.getImportsClosure()) {
			if (o.equals(sourceOntology))
				continue;
			addSupportOntology(o);
		}
	}
	
	public void remakeOntologiesFromImportsClosure() throws OWLOntologyCreationException {
		remakeOntologiesFromImportsClosure((new OWLOntologyID()).getOntologyIRI());
	}
	
	public void remakeOntologiesFromImportsClosure(IRI ontologyIRI) throws OWLOntologyCreationException {
		addSupportOntologiesFromImportsClosure();
		sourceOntology = manager.createOntology(sourceOntology.getAxioms(), ontologyIRI);
	}
	
	/**
	 * note: may move to mooncat
	 * @throws OWLOntologyCreationException
	 */
	public void mergeImportClosure() throws OWLOntologyCreationException {
		OWLOntologyID oid = sourceOntology.getOntologyID();
		Set<OWLOntology> imports = sourceOntology.getImportsClosure();
		Set<OWLAxiom> axioms = sourceOntology.getAxioms();
		manager.removeOntology(sourceOntology);
		sourceOntology = manager.createOntology(oid);
		for (OWLOntology o : imports) {
			if (o.equals(sourceOntology))
				continue;
			manager.addAxioms(sourceOntology, o.getAxioms());
		}
	}


	/**
	 * in general application code need not call this - it is mostly used internally
	 * 
	 * @return union of source ontology plus all supporting ontologies
	 */
	public Set<OWLOntology> getAllOntologies() {
		Set<OWLOntology> all = new HashSet<OWLOntology>(getSupportOntologySet());
		all.add(getSourceOntology());
		return all;
	}

	public OWLDataFactory getDataFactory() {
		return dataFactory;
	}

	public void setDataFactory(OWLDataFactory dataFactory) {
		this.dataFactory = dataFactory;
	}



	public OWLOntologyManager getManager() {
		return manager;
	}

	public void setManager(OWLOntologyManager manager) {
		this.manager = manager;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}


	// ----------------------------------------
	// BASIC GRAPH EDGE TRAVERSAL
	// ----------------------------------------



	/**
	 * retrieves direct edges from a source
	 * to the direct **named** target
	 * 
	 * e.g. if (A SubClassOf B) then outgoing(A) = { <A,sub,B>}
	 * e.g. if (A SubClassOf R some B) then outgoing(A) = { <A, R-some, B> }
	 * e.g. if (A SubClassOf R some (R2 some B)) then outgoing(A) = { <A, [R-some,R2-same], B> }
	 * 
	 * @param cls source
	 * @return all edges that originate from source to nearest named object target
	 */
	public Set<OWLGraphEdge> getOutgoingEdges(OWLObject cls) {
		Set<OWLGraphEdge> pEdges = getPrimitiveOutgoingEdges(cls);
		Set<OWLGraphEdge> edges = new HashSet<OWLGraphEdge>();
		for (OWLGraphEdge e : pEdges) {
			edges.addAll(primitiveEdgeToFullEdges(e));
		}
		return edges;
	}
	
	public Set<OWLGraphEdge> getOutgoingEdges(OWLObject obj, boolean isClosure,boolean isReflexive) {
		if (isClosure) {
			if (isReflexive)
				return getOutgoingEdgesClosureReflexive(obj);
			else
				return getOutgoingEdgesClosure(obj);
		}
		else
			return getOutgoingEdgesClosure(obj);
	}

	private Set<OWLObject> getOutgoingEdgesViaReverseUnion(OWLObject child) {
		if (extraSubClassOfEdges == null)
			cacheReverseUnionMap();
		if (extraSubClassOfEdges.containsKey(child)) 
			return new HashSet<OWLObject>(extraSubClassOfEdges.get(child));
		else
			return new HashSet<OWLObject>();
	}


	private void cacheReverseUnionMap() {
		extraSubClassOfEdges = new HashMap<OWLObject, Set<OWLObject>>();
		for (OWLOntology o : getAllOntologies()) {
			for (OWLClass cls : o.getClassesInSignature()) {
				for (OWLEquivalentClassesAxiom eca : o.getEquivalentClassesAxioms(cls)) {
					for (OWLClassExpression ce : eca.getClassExpressions()) {
						if (ce instanceof OWLObjectUnionOf) {
							for (OWLObject child : ((OWLObjectUnionOf)ce).getOperands()) {
								if (extraSubClassOfEdges.containsKey(child)) {
									extraSubClassOfEdges.get(child).add(cls);
								}
								else {
									extraSubClassOfEdges.put(child, new HashSet<OWLObject>());
									extraSubClassOfEdges.get(child).add(cls);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * primitive edges connect any combination of named objects and expressions
	 * 
	 * e.g. (A SubClassOf R some B) => <A,sub,R-some-B>, <R-some-B,R-some,B>
	 * @param s source
	 * @return set of {@link OWLGraphEdge}
	 */
	public Set<OWLGraphEdge> getPrimitiveOutgoingEdges(OWLObject s) {
		profiler.startTaskNotify("getPrimitiveOutgoingEdges");
		Set<OWLGraphEdge> edges = new HashSet<OWLGraphEdge>();
		for (OWLOntology o : getAllOntologies()) {
			if (s instanceof OWLClass) {

				for (OWLSubClassOfAxiom sca : o.getSubClassAxiomsForSubClass((OWLClass) s)) {
					edges.add(createSubClassOfEdge(sca.getSubClass(), sca.getSuperClass()));
				}
				for (OWLEquivalentClassesAxiom eqa : o.getEquivalentClassesAxioms((OWLClass) s)) {
					for (OWLClassExpression ce : eqa.getClassExpressions()) {
						if (!ce.equals(s))
							edges.add(createSubClassOfEdge(s, ce));
					}
				}
				for (OWLObject pbu : getOutgoingEdgesViaReverseUnion(s)) {
					if (pbu instanceof OWLClass)
						edges.add(createSubClassOfEdge(s,(OWLClass)pbu));
				}

			}
			else if (s instanceof OWLIndividual) {
				// TODO - do we care about punning?
				// need to define semantics here
				//System.err.println("getting individual axioms");
				for (OWLClassAssertionAxiom a : o.getClassAssertionAxioms((OWLIndividual) s)) {
					edges.add(new OWLGraphEdge(s,a.getClassExpression(),null,Quantifier.INSTANCE_OF,getSourceOntology()));
				}
				for (OWLObjectPropertyAssertionAxiom a : o.getObjectPropertyAssertionAxioms((OWLIndividual) s)) {
					edges.add(new OWLGraphEdge(s,a.getObject(),a.getProperty(),Quantifier.PROPERTY_ASSERTION,getSourceOntology()));
				}
			}
			else if (s instanceof OWLRestriction<?, ?, ?>) {
				edges.add(restrictionToPrimitiveEdge((OWLRestriction<?, ?, ?>) s));
			}
			else if (s instanceof OWLObjectIntersectionOf) {
				for (OWLClassExpression ce : ((OWLObjectIntersectionOf)s).getOperands()) {
					edges.add(createSubClassOfEdge(s,ce));
				}
			}
			else if (s instanceof OWLObjectUnionOf) {
				// do nothing in this direction
			}
		}

		if (reasoner != null) {
			//			if (s instanceof OWLClassExpression) {
			// JCel can't do class expressions. TODO: make this more flexible
			if (s instanceof OWLClass) {
				for (Node<OWLClass> pn : reasoner.getSuperClasses( (OWLClassExpression)s, true)) {
					for (OWLClass p : pn.getEntities()) {
						OWLGraphEdge e = createSubClassOfEdge(s,p);
						e.getSingleQuantifiedProperty().setInferred(true);
						edges.add(e);
					}
				}
			}
		}

		filterEdges(edges);
		profiler.endTaskNotify("getPrimitiveOutgoingEdges");

		return edges;
	}

	/**
	 * only include those edges that match user constraints
	 * @param edges
	 */
	private void filterEdges(Set<OWLGraphEdge> edges) {
		Set<OWLGraphEdge> rmEdges = new HashSet<OWLGraphEdge>();
		if (config.graphEdgeIncludeSet != null) {
			for (OWLGraphEdge e : edges) {
				if (!edgeSatisfiesOneOf(e, config.graphEdgeIncludeSet)) {
					rmEdges.add(e);
				}
			}
		}
		if (config.graphEdgeExcludeSet != null) {
			for (OWLGraphEdge e : edges) {
				if (edgeSatisfiesOneOf(e, config.graphEdgeExcludeSet)) {
					rmEdges.add(e);
				}
			}
		}
		//TODO
		//if (config.excludeMetaClass != null) {
		for (OWLGraphEdge e : edges) {
			OWLObject t = e.getTarget();
			if (t instanceof OWLNamedObject) {
				OWLNamedObject nt = (OWLNamedObject) t;
				// TODO
				if (nt.getIRI().toString().startsWith("http://www.ifomis.org/bfo"))
					rmEdges.add(e);
			}
		}

		//}

		for (OWLGraphEdge e : edges) {
			OWLObject t = e.getTarget();
			// TODO - use this: dataFactory.getOWLThing();
			if (t instanceof OWLNamedObject &&
					((OWLNamedObject) t).getIRI().equals(OWLRDFVocabulary.OWL_THING.getIRI())) {
				rmEdges.add(e);
			}
		}
		edges.removeAll(rmEdges);
	}

	private boolean edgeSatisfiesOneOf(OWLGraphEdge e, Set<OWLQuantifiedProperty> qps) {
		for (OWLQuantifiedProperty c : qps) {
			if (edgeSatisfies(e, c))
				return true;
		}
		return false;
	}

	private boolean edgeSatisfies(OWLGraphEdge e, OWLQuantifiedProperty c) {
		return c.subsumes(e.getSingleQuantifiedProperty());
	}

	// e.g. R-some-B ==> <R-some-B,R,B>
	private OWLGraphEdge restrictionToPrimitiveEdge(OWLRestriction s) {
		OWLObjectPropertyExpression p = null;
		OWLObject t = null;
		OWLQuantifiedProperty.Quantifier q = null;
		if (s instanceof OWLObjectSomeValuesFrom) {
			t  = ((OWLObjectSomeValuesFrom)s).getFiller();
			p = (OWLObjectPropertyExpression) s.getProperty();
			q = OWLQuantifiedProperty.Quantifier.SOME;
		}
		else if (s instanceof OWLObjectAllValuesFrom) {
			t  = ((OWLObjectAllValuesFrom)s).getFiller();
			p = (OWLObjectPropertyExpression) s.getProperty();
			q = OWLQuantifiedProperty.Quantifier.ONLY;
		}
		else if (s instanceof OWLObjectHasValue) {
			t  = ((OWLObjectHasValue)s).getValue();
			p = (OWLObjectPropertyExpression) s.getProperty();
			q = OWLQuantifiedProperty.Quantifier.VALUE;
		}
		else {
			System.err.println("cannot handle:"+s);
		}
		return new OWLGraphEdge(s,t,p,q,getSourceOntology());
	}

	private OWLGraphEdge createSubClassOfEdge(OWLObject s, OWLClassExpression t) {
		return new OWLGraphEdge(s,t,null,Quantifier.SUBCLASS_OF,getSourceOntology());
	}


	// extend an edge target until we hit a named object.
	// this could involve multiple extensions and "forks", e.g.
	// <A sub B^C> ==> <A sub B>, <A sub C>
	private Set<OWLGraphEdge> primitiveEdgeToFullEdges(OWLGraphEdge e) {
		Set<OWLGraphEdge> edges = new HashSet<OWLGraphEdge>();
		if (e.isTargetNamedObject()) {
			edges.add(e); // do nothing
		}
		else {
			// extend
			OWLObject s = e.getSource();
			Set<OWLGraphEdge> nextEdges = getOutgoingEdges(e.getTarget());
			for (OWLGraphEdge e2 : nextEdges) {
				edges.add(this.combineEdgePair(s, e, e2, 1));
			}
		}
		return edges;
	}



	/**
	 * caches full outgoing and incoming edges
	 * 
	 * in general you should not need to call this directly;
	 * used internally by this class.
	 */
	public void cacheEdges() {
		edgeBySource = new HashMap<OWLObject,Set<OWLGraphEdge>>();
		edgeByTarget = new HashMap<OWLObject,Set<OWLGraphEdge>>();

		// initialize with all named objects in ontology
		Stack<OWLObject> allObjs = new Stack<OWLObject>();
		allObjs.addAll(getAllOWLObjects());

		Set<OWLObject> visisted = new HashSet<OWLObject>();

		while (allObjs.size() > 0) {
			OWLObject s = allObjs.pop();
			if (visisted.contains(s))
				continue;
			visisted.add(s);
			if (!edgeBySource.containsKey(s))
				edgeBySource.put(s, new HashSet<OWLGraphEdge>());
			for (OWLGraphEdge edge : getPrimitiveOutgoingEdges(s)) {
				edgeBySource.get(s).add(edge);
				OWLObject t = edge.getTarget();
				if (!edgeByTarget.containsKey(t))
					edgeByTarget.put(t, new HashSet<OWLGraphEdge>());
				edgeByTarget.get(t).add(edge);

				// we also want to get all edges from class expressions;
				// class expressions aren't in the initial signature, but
				// we add them here when we encounter them
				if (t instanceof OWLClassExpression) {
					allObjs.add(t);
				}
			}
		}
	}

	/**
	 * @param t
	 * @return all edges that have t as a direct target
	 */
	public Set<OWLGraphEdge> getIncomingEdges(OWLObject t) {
		ensureEdgesCached();
		if (edgeByTarget.containsKey(t)) {
			return new HashSet<OWLGraphEdge>(edgeByTarget.get(t));
		}
		return new HashSet<OWLGraphEdge>();
	}


	private void ensureEdgesCached() {
		if (edgeByTarget == null)
			cacheEdges();

	}


	/**
	 * pack/translate an edge (either asserted or a graph closure edge) into
	 * an OWL class expression according to the OWLGraph to OWLOntology
	 * translation rules.
	 * 
	 * (this is the reverse translation of the one from an OWLOntology to an
	 * OWLGraph)
	 * 
	 * e.g. after calling for the graph closure of an OWLClass a,
	 * we may get back an edge <a [part_of-some, adjacent_to-some, has_part-some] b>.
	 * after feeding this edge into this method we obtain the expression
	 *   part_of some (adjacent_to some (has_part some b))
	 * 
	 * @param e edge
	 * @return class expression equivalent to edge
	 */
	public OWLObject edgeToTargetExpression(OWLGraphEdge e) {
		return edgeToTargetExpression(e.getQuantifiedPropertyList().iterator(),e.getTarget());
	}

	private OWLObject edgeToTargetExpression(
			Iterator<OWLQuantifiedProperty> qpi, OWLObject t) {
		if (qpi.hasNext()) {
			OWLQuantifiedProperty qp = qpi.next();
			if (qp == null) {
				System.err.println("");
			}
			OWLObject x = edgeToTargetExpression(qpi,t);
			OWLClassExpression t2;
			if (!(x instanceof OWLClassExpression)) {
				//System.err.println("Not a CE: "+x);
				HashSet<OWLNamedIndividual> ins = new HashSet<OWLNamedIndividual>();
				ins.add((OWLNamedIndividual) x);
				t2 = dataFactory.getOWLObjectOneOf(ins);
			}
			else {
				t2 = (OWLClassExpression) x;
			}

			if (qp.isSubClassOf()) {
				return t2;
			}
			else if (qp.isInstanceOf()) {
				return t2;
			}
			else if (qp.isIdentity()) {
				return t2;
			}
			else if (qp.isPropertyAssertion()) {
				return dataFactory.getOWLObjectSomeValuesFrom(qp.getProperty(), 
						(OWLClassExpression) t2);
			}
			else if (qp.isSomeValuesFrom()) {
				return dataFactory.getOWLObjectSomeValuesFrom(qp.getProperty(), 
						(OWLClassExpression) t2);
			}
			else if (qp.isAllValuesFrom()) {
				return dataFactory.getOWLObjectAllValuesFrom(qp.getProperty(), 
						(OWLClassExpression) t2);
			}
			else if (qp.isHasValue()) {
				if (x instanceof OWLNamedObject)
					return dataFactory.getOWLObjectHasValue(qp.getProperty(), 
							dataFactory.getOWLNamedIndividual(((OWLNamedObject) x).getIRI()));
				else {
					System.err.println("warning: treating "+x+" as allvaluesfrom");
					return dataFactory.getOWLObjectAllValuesFrom(qp.getProperty(), 
							(OWLClassExpression) x);
				}
			}
			else {
				System.err.println("cannot handle:"+qp);
				// TODO
				return null;
			}
		}
		else {
			return t;
		}
	}


	// ----------------------------------------
	// GRAPH CLOSURE METHODS
	// ----------------------------------------


	/**
	 * Retrieves the graph closure originating from source.
	 * E.g. if A SubClassOf R some B & B SubClassOf S some C, then
	 * closure(A) = { <A R-some B>, <A [R-some,S-some] C>.
	 * 
	 * Composition rules are used to compact the list of connecting edge labels
	 * (e.g. transitivity).
	 * 
	 * The resulting edges can be translated into class expressions using 
	 * method edgeToTargetExpression(e). E.g. in the above the expression would be
	 *   R some (S some C)
	 * 
	 * @param s source
	 * @return closure of edges originating from source
	 */
	public Set<OWLGraphEdge> getOutgoingEdgesClosure(OWLObject s) {

		if (config.isCacheClosure) {
			//System.out.println("@@checking cache for:"+s+" cache:"+inferredEdgeBySource == null ? "NULL" : "SET");
			if (inferredEdgeBySource == null)
				inferredEdgeBySource = new HashMap<OWLObject,Set<OWLGraphEdge>>();
			if (inferredEdgeBySource.containsKey(s)) {
				//System.out.println("@@cache:"+s+" -->"+inferredEdgeBySource.get(s).size());
				return new HashSet<OWLGraphEdge>(inferredEdgeBySource.get(s));
			}
		}
		profiler.startTaskNotify("getOutgoingEdgesClosure");

		Stack<OWLGraphEdge> edgeStack = new Stack<OWLGraphEdge>();
		Set<OWLGraphEdge> closureSet = new HashSet<OWLGraphEdge>();
		//Set<OWLGraphEdge> visitedSet = new HashSet<OWLGraphEdge>();
		Set<OWLObject> visitedObjs = new HashSet<OWLObject>();
		Map<OWLObject,Set<OWLGraphEdge>> visitedMap = new HashMap<OWLObject,Set<OWLGraphEdge>>();
		visitedObjs.add(s);
		visitedMap.put(s, new HashSet<OWLGraphEdge>());

		// initialize. we seed the search with a reflexive identity edge DEPR
		//edgeStack.add(new OWLGraphEdge(s,s,null,Quantifier.IDENTITY,ontology));

		// seed stack
		edgeStack.addAll(getPrimitiveOutgoingEdges(s));
		closureSet.addAll(edgeStack);
		while (!edgeStack.isEmpty()) {
			OWLGraphEdge ne = edgeStack.pop();
			//System.out.println("NEXT: "+ne+" //stack: "+edgeStack);
			int nextDist = ne.getDistance() + 1;
			Set<OWLGraphEdge> extSet = getPrimitiveOutgoingEdges(ne.getTarget());
			for (OWLGraphEdge extEdge : extSet) {
				//System.out.println("   EXT:"+extEdge);
				OWLGraphEdge nu = combineEdgePair(s, ne, extEdge, nextDist);
				OWLObject nuTarget = nu.getTarget();
				//System.out.println("     COMBINED:"+nu);

				// check for cycles. this is not as simple as
				// checking if we have visited the node, as we are interested
				// in different paths to the same node.
				// todo - check if there is an existing path to this node
				//  that is shorter
				//if (!visitedSet.contains(nu)) {
				boolean isEdgeVisited = false;
				if (visitedObjs.contains(nuTarget)) {
					// we have potentially visited this edge before


					// TODO - this is temporary. need to check edge not node
					//isEdgeVisited = true;
					/*
					 */
					//System.out.println("checking to see if  visisted "+nu);
					//System.out.println(nu.getFinalQuantifiedProperty());
					for (OWLGraphEdge ve : visitedMap.get(nuTarget)) {
						//System.out.println(" ve:"+ve.getFinalQuantifiedProperty());
						if (ve.getFinalQuantifiedProperty().equals(nu.getFinalQuantifiedProperty())) {
							//System.out.println("already visited: "+nu);
							isEdgeVisited = true;
						}
					}
					if (!isEdgeVisited) {
						visitedMap.get(nuTarget).add(nu);
					}
				}
				else {
					visitedObjs.add(nuTarget);
					visitedMap.put(nuTarget, new HashSet<OWLGraphEdge>());
					visitedMap.get(nuTarget).add(nu);
				}

				if (!isEdgeVisited) {
					//System.out.println("      *NOT VISITED:"+nu+" visistedSize:"+visitedSet.size());
					if (nu.getTarget() instanceof OWLNamedObject || 
							config.isIncludeClassExpressionsInClosure) {
						closureSet.add(nu);
					}
					edgeStack.add(nu);
					//visitedSet.add(nu);		

				}

			}
		}

		if (config.isCacheClosure) {
			inferredEdgeBySource.put(s, new HashSet<OWLGraphEdge>(closureSet));
		}
		profiler.endTaskNotify("getOutgoingEdgesClosure");
		return closureSet;
	}

	/**
	 * as getOutgoingEdgesClosure(s), but also includes an identity edge
	 * @param s
	 * @return set of {@link OWLGraphEdge}
	 */
	public Set<OWLGraphEdge> getOutgoingEdgesClosureReflexive(OWLObject s) {
		Set<OWLGraphEdge> edges = getOutgoingEdgesClosure(s);
		edges.add(new OWLGraphEdge(s,s,null,Quantifier.IDENTITY,getSourceOntology()));
		return edges;
	}

	/**
	 * find the set of classes or class expressions subsuming source, using the graph closure.
	 * 
	 * this is just the composition of getOutgoingEdgesClosure and edgeToTargetExpression -- the
	 * latter method "packs" a chain of edges into a class expression
	 * 
	 * only "linear" expressions are found, corresponding to a path in the graph.
	 * e.g. [sub,part_of-some,develops_from-some] ==> part_of some (develops_from some t)
	 * 
	 * if the edge consists entirely of subclass links, the the subsumers will be all
	 * named classes.
	 * 
	 * @param s source
	 * @return set of {@link OWLObject}
	 */
	public Set<OWLObject> getSubsumersFromClosure(OWLObject s) {
		Set<OWLObject> ts = new HashSet<OWLObject>();
		for (OWLGraphEdge e : getOutgoingEdgesClosure(s)) {
			ts.add(edgeToTargetExpression(e));
		}
		return ts;
	}

	public Set<OWLObject> queryDescendants(OWLGraphEdge e) {
		profiler.startTaskNotify("queryDescendants");
		Set<OWLObject> results = new HashSet<OWLObject>();
		// reflexivity
		results.add(this.edgeToTargetExpression(e));
		List<OWLQuantifiedProperty> eqpl = e.getQuantifiedPropertyList();
		// todo - cast
		for (OWLObject d1 : queryDescendants((OWLClassExpression)e.getTarget())) {
			Set<OWLGraphEdge> dEdges = this.getIncomingEdgesClosure(d1);
			for (OWLGraphEdge dEdge : dEdges) {
				OWLGraphEdge newEdge = combineEdgePair(e.getTarget(), 
						new OWLGraphEdge(null, null, Quantifier.SUBCLASS_OF), dEdge, 1);
				List<OWLQuantifiedProperty> dqpl = newEdge.getQuantifiedPropertyList();
				if (dqpl.equals(eqpl)) {
					results.add(dEdge.getSource());
				}
			}
		}
		profiler.endTaskNotify("queryDescendants");
		return results;
	}

	/**
	 * Performs a closed-world query using a DL expression as a set of boolean database-style constraints.
	 * 
	 * No attempt is made to optimize the query. The engine is incomplete and currently ontology implements
	 * queries for constructs that use AND, OR, SOME
	 * 
	 * @param t classExpression
	 * @return set of descendants
	 */
	public Set<OWLObject> queryDescendants(OWLClassExpression t) {
		return queryDescendants(t, true, true);
	}

	public Set<OWLObject> queryDescendants(OWLClassExpression t, boolean isInstances, boolean isClasses) {
		Set<OWLObject> results = new HashSet<OWLObject>();
		results.add(t);

		// transitivity and link composition
		Set<OWLGraphEdge> dEdges = this.getIncomingEdgesClosure(t);
		for (OWLGraphEdge dEdge : dEdges) {
			if (dEdge.getQuantifiedPropertyList().size() > 1)
				continue;
			OWLQuantifiedProperty qp = dEdge.getSingleQuantifiedProperty();
			if ((isInstances && qp.isInstanceOf()) || 
					(isClasses && qp.isSubClassOf()))
				results.add(dEdge.getSource());
		}

		if (t instanceof OWLObjectIntersectionOf) {
			Set<OWLObject> iresults = null;
			for (OWLClassExpression y : ((OWLObjectIntersectionOf)t).getOperands()) {
				if (iresults == null) {
					iresults = queryDescendants(y, isInstances, isClasses);
				}
				else {
					iresults.retainAll(queryDescendants(y, isInstances, isClasses));
				}
				results.addAll(iresults);
			}
		}
		else if (t instanceof OWLObjectUnionOf) {
			for (OWLClassExpression y : ((OWLObjectUnionOf)t).getOperands()) {
				results.addAll(queryDescendants(y, isInstances, isClasses));
			}
		}
		else if (t instanceof OWLRestriction) {
			results.addAll(queryDescendants(restrictionToPrimitiveEdge((OWLRestriction) t)));
		}
		else if (t instanceof OWLObjectComplementOf) {
			// NOTE: this is closed-world negation
			results.removeAll(queryDescendants( ((OWLObjectComplementOf) t).getOperand()));
		}
		// equivalent classes - substitute a named class in the query for an expression
		else if (t instanceof OWLClass) {
			for (OWLOntology ont : this.getAllOntologies()) {
				for (OWLEquivalentClassesAxiom ax : ont.getEquivalentClassesAxioms((OWLClass)t)) {
					for (OWLClassExpression y : ax.getClassExpressions()) {
						if (y instanceof OWLClass)
							continue;
						results.addAll(queryDescendants(y, isInstances, isClasses));
					}
				}
			}
		}
		else {
			LOG.error("Cannot handle:"+t);
		}

		return results;
	}

	/**
	 * @param s source
	 * @param t target
	 * @return all edges connecting source and target in the graph closure
	 */

	public Set<OWLGraphEdge> getEdgesBetween(OWLObject s, OWLObject t) {
		Set<OWLGraphEdge> allEdges = getOutgoingEdgesClosureReflexive(s);
		Set<OWLGraphEdge> edges = new HashSet<OWLGraphEdge>();
		for (OWLGraphEdge e : allEdges) {
			if (e.getTarget().equals(t))
				edges.add(e);
		}
		return edges;
	}


	/**
	 * returns all ancestors of an object. Here, ancestors is defined as any
	 * named object that can be reached from x over some path of asserted edges.
	 * relations are ignored.
	 * 
	 * @param x source
	 * @return all reachable target nodes, regardless of edges
	 */
	public Set<OWLObject> getAncestors(OWLObject x) {
		Set<OWLObject> ancs = new HashSet<OWLObject>();
		for (OWLGraphEdge e : getOutgoingEdgesClosure(x)) {
			ancs.add(e.getTarget());
		}
		return ancs;
	}
	
	/**
	 * returns all ancestors that can be reached over subclass or
	 * the specified set of relations
	 * 
	 * @param x the sourceObject
	 * @param overProps
	 * @return set of ancestors
	 */
	public Set<OWLObject> getAncestors(OWLObject x, Set<OWLPropertyExpression> overProps) {
		Set<OWLObject> ancs = new HashSet<OWLObject>();
		for (OWLGraphEdge e : getOutgoingEdgesClosure(x)) {
			boolean isAddMe = false;
			if (overProps != null) {
				List<OWLQuantifiedProperty> qps = e.getQuantifiedPropertyList();
				if (qps.size() == 0) {
					isAddMe = true;
				}
				else if (qps.size() == 1) {
					OWLQuantifiedProperty qp = qps.get(0);
					if (qp.isIdentity()) {
						isAddMe = true;
					}
					else if (qp.isSubClassOf()) {
						isAddMe = true;
					}
					else if (qp.isSomeValuesFrom() && overProps.contains(qp.getProperty())) {
						isAddMe = true;
					}
				}
				else {
					// no add
				}
			}
			else {
				isAddMe = true;
			}
			if (isAddMe)
				ancs.add(e.getTarget());
		}
		return ancs;
	}

	public Set<OWLObject> getAncestorsReflexive(OWLObject x) {
		Set<OWLObject> ancs = getAncestors(x);
		ancs.add(x);
		return ancs;
	}
	public Set<OWLObject> getAncestorsReflexive(OWLObject x, Set<OWLPropertyExpression> overProps) {
		Set<OWLObject> ancs = getAncestors(x, overProps);
		ancs.add(x);
		return ancs;
	}

	/**
	 * Gets all ancestors that are OWLNamedObjects
	 * 
	 * i.e. excludes anonymous class expressions
	 * 
	 * @param x
	 * @return set of named ancestors
	 */
	public Set<OWLObject> getNamedAncestors(OWLObject x) {
		Set<OWLObject> ancs = new HashSet<OWLObject>();
		for (OWLGraphEdge e : getOutgoingEdgesClosure(x)) {
			if (e.getTarget() instanceof OWLNamedObject)
				ancs.add(e.getTarget());
		}
		return ancs;
	}
	public Set<OWLObject> getNamedAncestorsReflexive(OWLObject x) {
		Set<OWLObject> ancs = getNamedAncestors(x);
		ancs.add(x);
		return ancs;
	}

	/**
	 * gets all descendants d of x, where d is reachable by any path. Excludes self
	 * 
	 * @see #getAncestors
	 * @see owltools.graph
	 * @param x
	 * @return descendant objects
	 */
	public Set<OWLObject> getDescendants(OWLObject x) {
		Set<OWLObject> descs = new HashSet<OWLObject>();
		for (OWLGraphEdge e : getIncomingEdgesClosure(x)) {
			descs.add(e.getSource());
		}
		return descs;
	}
	/**
	 * gets all reflexive descendants d of x, where d is reachable by any path. Includes self
	 * 
	 * @see #getAncestors
	 * @see owltools.graph
	 * @param x
	 * @return descendant objects plus x
	 */
	public Set<OWLObject> getDescendantsReflexive(OWLObject x) {
		Set<OWLObject> getDescendants = getDescendants(x);
		getDescendants.add(x);
		return getDescendants;
	}

	/**
	 * return all individuals i where x is reachable from i
	 * @param x
	 * @return set of individual {@link OWLObject}s
	 */
	public Set<OWLObject> getIndividualDescendants(OWLObject x) {
		Set<OWLObject> descs = new HashSet<OWLObject>();
		for (OWLGraphEdge e : getIncomingEdgesClosure(x)) {
			OWLObject s = e.getSource();
			if (s instanceof OWLIndividual)
				descs.add(s);
		}
		return descs;
	}


	/**
	 * gets all inferred edges coming in to the target edge
	 * 
	 * for every s, if t is reachable from s, then include the inferred edge between s and t.
	 * 
	 * @see #getOutgoingEdgesClosure
	 * @param t target
	 * @return all edges connecting all descendants of target to target
	 */
	public Set<OWLGraphEdge> getIncomingEdgesClosure(OWLObject t) {

		if (config.isCacheClosure) {
			if (inferredEdgeByTarget == null)
				inferredEdgeByTarget = new HashMap<OWLObject,Set<OWLGraphEdge>>();
			if (inferredEdgeByTarget.containsKey(t)) {
				return new HashSet<OWLGraphEdge>(inferredEdgeByTarget.get(t));
			}
		}
		profiler.startTaskNotify("getIncomingEdgesClosure");

		Stack<OWLGraphEdge> edgeStack = new Stack<OWLGraphEdge>();
		Set<OWLGraphEdge> closureSet = new HashSet<OWLGraphEdge>();
		//Set<OWLGraphEdge> visitedSet = new HashSet<OWLGraphEdge>();
		Set<OWLObject> visitedObjs = new HashSet<OWLObject>();
		Map<OWLObject,Set<OWLGraphEdge>> visitedMap = new HashMap<OWLObject,Set<OWLGraphEdge>>();
		visitedObjs.add(t);
		visitedMap.put(t, new HashSet<OWLGraphEdge>());

		// initialize -
		// note that edges are always from src to tgt. here we are extending down from tgt to src

		//edgeStack.add(new OWLGraphEdge(t,t,ontology,new OWLQuantifiedProperty()));
		edgeStack.addAll(getIncomingEdges(t));
		closureSet.addAll(edgeStack);

		while (!edgeStack.isEmpty()) {
			OWLGraphEdge ne = edgeStack.pop();

			int nextDist = ne.getDistance() + 1;

			// extend down from this edge; e.g. [s, extEdge + ne, tgt] 
			Set<OWLGraphEdge> extSet = getIncomingEdges(ne.getSource());
			for (OWLGraphEdge extEdge : extSet) {

				// extEdge o ne --> nu
				//OWLGraphEdge nu = combineEdgePairDown(ne, extEdge, nextDist);
				OWLGraphEdge nu = combineEdgePair(extEdge.getSource(), extEdge, ne, nextDist);
				OWLObject nusource = nu.getSource();

				boolean isEdgeVisited = false;
				if (visitedObjs.contains(nusource)) {
					//isEdgeVisited = true;
					for (OWLGraphEdge ve : visitedMap.get(nusource)) {
						//System.out.println(" ve:"+ve.getFinalQuantifiedProperty());
						if (ve.getFirstQuantifiedProperty().equals(nu.getFirstQuantifiedProperty())) {
							//System.out.println("already visited: "+nu);
							// always favor the shorter path
							if (ve.getQuantifiedPropertyList().size() <= nu.getQuantifiedPropertyList().size()) {
								isEdgeVisited = true;
							}
						}
					}
					if (!isEdgeVisited) {
						visitedMap.get(nusource).add(nu);
					}

				}
				else {
					visitedObjs.add(nusource);
					visitedMap.put(nusource, new HashSet<OWLGraphEdge>());
					visitedMap.get(nusource).add(nu);
				}

				if (!isEdgeVisited) {
					if (nu.getSource() instanceof OWLNamedObject || 
							config.isIncludeClassExpressionsInClosure) {
						closureSet.add(nu);
					}
					edgeStack.add(nu);
					//visitedSet.add(nu);		

				}

			}
		}

		if (config.isCacheClosure) {
			inferredEdgeByTarget.put(t, new HashSet<OWLGraphEdge>(closureSet));
		}
		profiler.endTaskNotify("getIncomingEdgesClosure");
		return closureSet;
	}

	/**
	 * Composes two graph edges into a new edge, using axioms in the ontology to determine the correct composition
	 * 
	 * For example,  Edge(x,SUBCLASS_OF,y) * Edge(y,SUBCLASS_OF,z) yields Edge(x,SUBCLASS_OF,z)
	 * 
	 * Note that property chains of length>2 are currently ignored
	 * 
	 * @param s - source node
	 * @param ne - edge 1
	 * @param extEdge - edge 2
	 * @param nextDist - new distance
	 * @return edge
	 */
	public OWLGraphEdge combineEdgePair(OWLObject s, OWLGraphEdge ne, OWLGraphEdge extEdge, int nextDist) {
		//System.out.println("combing edges: "+s+" // "+ne+ " * "+extEdge);
		// Create an edge with no edge labels; we will fill the label in later
		OWLGraphEdge nu = new OWLGraphEdge(s, extEdge.getTarget());
		List<OWLQuantifiedProperty> qpl1 = new Vector<OWLQuantifiedProperty>(ne.getQuantifiedPropertyList());
		List<OWLQuantifiedProperty> qpl2 = new Vector<OWLQuantifiedProperty>(extEdge.getQuantifiedPropertyList());

		while (qpl1.size() > 0 && qpl2.size() > 0) {
			OWLQuantifiedProperty combinedQP = combinedQuantifiedPropertyPair(qpl1.get(qpl1.size()-1),qpl2.get(0));
			if (combinedQP == null)
				break;
			qpl1.set(qpl1.size()-1, combinedQP);
			if (combinedQP.isIdentity())
				qpl1.subList(qpl1.size()-1,qpl1.size()).clear();
			qpl2.subList(0, 1).clear();
		}
		qpl1.addAll(qpl2);
		nu.setQuantifiedPropertyList(qpl1);
		nu.setDistance(nextDist);
		return nu;
	}

	/**
	 *  combine [srcEdge + tgtEdge]
	 *  
	 *  srcEdge o tgtEdge --> returned edge
	 *  
	 * @see #combineEdgePair(OWLObject s, OWLGraphEdge ne, OWLGraphEdge extEdge, int nextDist) 
	 * @param tgtEdge
	 * @param srcEdge
	 * @param nextDist
	 * @return edge
	 */
	private OWLGraphEdge combineEdgePairDown(OWLGraphEdge tgtEdge, OWLGraphEdge srcEdge, int nextDist) {
		// fill in edge label later
		// todo
		OWLGraphEdge nu = new OWLGraphEdge(srcEdge.getSource(), tgtEdge.getTarget());
		nu.setDistance(nextDist);
		Vector<OWLQuantifiedProperty> qps = new Vector<OWLQuantifiedProperty>();

		// put all but the final one in a new list
		int n = 0;
		int size = tgtEdge.getQuantifiedPropertyList().size();
		OWLQuantifiedProperty finalQP = null;
		for (OWLQuantifiedProperty qp : tgtEdge.getQuantifiedPropertyList()) {
			n++;
			if (n > 1)
				qps.add(qp);
			else
				finalQP = qp;
		}
		// TODO
		// join src+tgt edge
		OWLQuantifiedProperty combinedQP = 
			combinedQuantifiedPropertyPair(srcEdge.getFinalQuantifiedProperty(), tgtEdge.getSingleQuantifiedProperty());
		//combinedQuantifiedPropertyPair(tgtEdge.getFinalQuantifiedProperty(), srcEdge.getSingleQuantifiedProperty());
		if (combinedQP == null) {
			qps.add(finalQP);
			qps.add(srcEdge.getSingleQuantifiedProperty());
		}
		else {
			qps.add(combinedQP);
		}
		nu.setQuantifiedPropertyList(qps);
		return nu;
	}

	/**
	 * Edge composition rules
	 * 
	 * TODO - property chains of length > 2
	 * @param x 
	 * @param y 
	 * @return property or null
	 */
	private OWLQuantifiedProperty combinedQuantifiedPropertyPair(OWLQuantifiedProperty x, OWLQuantifiedProperty y) {

		if (x.isSubClassOf() && y.isSubClassOf()) { // TRANSITIVITY OF SUBCLASS
			return new OWLQuantifiedProperty(Quantifier.SUBCLASS_OF);
		}
		else if (x.isInstanceOf() && y.isSubClassOf()) { // INSTANCE OF CLASS IS INSTANCE OF SUPERCLASS
			return new OWLQuantifiedProperty(Quantifier.INSTANCE_OF);
		}
		else if (x.isSubClassOf() && y.isSomeValuesFrom()) { // TRANSITIVITY OF SUBCLASS: existentials
			return new OWLQuantifiedProperty(y.getProperty(),Quantifier.SOME);
		}
		else if (x.isSomeValuesFrom() && y.isSubClassOf()) { // TRANSITIVITY OF SUBCLASS: existentials
			return new OWLQuantifiedProperty(x.getProperty(),Quantifier.SOME);
		}
		else if (x.isSubClassOf() && y.isAllValuesFrom()) {
			return new OWLQuantifiedProperty(y.getProperty(),Quantifier.ONLY);
		}
		else if (x.isAllValuesFrom() && y.isSubClassOf()) {
			return new OWLQuantifiedProperty(x.getProperty(),Quantifier.ONLY);
		}
		else if (x.isSomeValuesFrom() &&
				y.isSomeValuesFrom() &&
				x.getProperty() != null && 
				x.getProperty().equals(y.getProperty()) && 
				x.getProperty().isTransitive(sourceOntology)) { // todo
			return new OWLQuantifiedProperty(x.getProperty(),Quantifier.SOME);
		}
		else if (x.isSomeValuesFrom() &&
				y.isSomeValuesFrom() &&
				chain(x.getProperty(), y.getProperty()) != null) { // TODO: length>2
			return new OWLQuantifiedProperty(chain(x.getProperty(), y.getProperty()),Quantifier.SOME);
		}
		else if (x.isPropertyAssertion() &&
				y.isPropertyAssertion() &&
				x.getProperty() != null && 
				x.getProperty().equals(y.getProperty()) && 
				x.getProperty().isTransitive(sourceOntology)) { // todo
			return new OWLQuantifiedProperty(x.getProperty(),Quantifier.PROPERTY_ASSERTION);
		}
		else if (x.isPropertyAssertion() &&
				y.isPropertyAssertion() &&
				x.getProperty() != null && 
				isInverseOfPair(x.getProperty(),y.getProperty())) {
			return new OWLQuantifiedProperty(Quantifier.IDENTITY); // TODO - doesn't imply identity for classes
		}
		else {
			// cannot combine - caller will add QP to sequence
			return null;
		}
	}

	// true if there is a property chain such that p1 o p2 --> p3, where p3 is returned
	private OWLObjectProperty chain(OWLObjectProperty p1, OWLObjectProperty p2) {
		if (p1 == null || p2 == null)
			return null;
		if (getPropertyChainMap().containsKey(p1)) {

			for (List<OWLObjectProperty> list : getPropertyChainMap().get(p1)) {
				if (p2.equals(list.get(0))) {
					return list.get(1);
				}
			}
		}
		return null;
	}

	// TODO - currently hardcoded for simple property chains
	Map<OWLObjectProperty,Set<List<OWLObjectProperty>>> pcMap = null;
	private Map<OWLObjectProperty,Set<List<OWLObjectProperty>>> getPropertyChainMap() {
		if (pcMap == null) {
			pcMap = new HashMap<OWLObjectProperty,Set<List<OWLObjectProperty>>>();
			for (OWLSubPropertyChainOfAxiom a : sourceOntology.getAxioms(AxiomType.SUB_PROPERTY_CHAIN_OF)) {
				//LOG.info("CHAIN:"+a+" // "+a.getPropertyChain().size());
				if (a.getPropertyChain().size() == 2) {
					OWLObjectPropertyExpression p1 = a.getPropertyChain().get(0);
					OWLObjectPropertyExpression p2 = a.getPropertyChain().get(1);
					//LOG.info("  xxCHAIN:"+p1+" o "+p2);
					if (p1 instanceof OWLObjectProperty && p2 instanceof OWLObjectProperty) {
						List<OWLObjectProperty> list = new Vector<OWLObjectProperty>();
						list.add((OWLObjectProperty) p2);
						list.add((OWLObjectProperty) a.getSuperProperty());
						if (!pcMap.containsKey(p1)) 
							pcMap.put((OWLObjectProperty) p1, new HashSet<List<OWLObjectProperty>>());
						pcMap.get((OWLObjectProperty) p1).add(list);
						//LOG.info("  xxxCHAIN:"+p1+" ... "+list);
					}
				}
				else {
					// TODO
				}
			}
		}
		return pcMap;
	}

	private boolean isInverseOfPair(OWLObjectProperty p1, OWLObjectProperty p2) {
		for (OWLOntology ont : getAllOntologies()) {
			for (OWLInverseObjectPropertiesAxiom a : ont.getInverseObjectPropertyAxioms(p1)) {
				if (a.getFirstProperty().equals(p2) ||
						a.getSecondProperty().equals(p2)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Find all edges of the form [i INST c] in the graph closure.
	 * (this includes both direct assertions, plus assertions to objects
	 *  that link to c via a chain of SubClassOf assertions)
	 *  
	 *  the semantics are the same as inferred ClassAssertion axioms
	 * 
	 * @param c owlClass
	 * @return all individuals classified here via basic graph traversal
	 */
	public Set<OWLIndividual> getInstancesFromClosure(OWLClass c) {
		Set<OWLIndividual> ins = new HashSet<OWLIndividual>();
		for (OWLOntology o : getAllOntologies()) {
			// iterate through all individuals; sequential scan may be slow for
			// large knowledge bases
			for (OWLIndividual in : o.getIndividualsInSignature()) {
				for (OWLGraphEdge e : getEdgesBetween(in, c)) {
					List<OWLQuantifiedProperty> qps = e.getQuantifiedPropertyList();
					// check for edges of the form < i INSTANCE_OF c >
					// we exclude relation chaims, e.g. <i [INSTANCE_OF PART_OF-some] c>
					if (qps.size() == 1 && qps.get(0).isInstanceOf()) {
						ins.add(in);
						break;
					}
				}
			}
		}
		return ins;
	}

	/**
	 * Finds all edges between an instance i and he given class c.
	 * 
	 * this includes inferred class assertions, as well as chains such as
	 * 
	 * i has_part j, j inst_of k, k part_of some c
	 * 
	 * @param c owlClass
	 * @return all edges in closure between an instance and owlClass
	 */
	public Set<OWLGraphEdge> getInstanceChainsFromClosure(OWLClass c) {
		Set<OWLGraphEdge> edges = new HashSet<OWLGraphEdge>();
		for (OWLOntology o : getAllOntologies()) {
			// iterate through all individuals; sequential scan may be slow for
			// large knowledge bases
			for (OWLIndividual in : o.getIndividualsInSignature()) {
				edges.addAll(getEdgesBetween(in, c));
			}
		}
		return edges;
	}


	// ----------------------------------------
	// BASIC WRAPPER UTILITIES
	// ----------------------------------------

	/**
	 * fetches all classes, individuals and object properties in all ontologies
	 * 
	 * @return all named objects
	 */
	public Set<OWLObject> getAllOWLObjects() {
		Set<OWLObject> obs = new HashSet<OWLObject>();
		for (OWLOntology o : getAllOntologies()) {
			obs.addAll(o.getClassesInSignature());
			obs.addAll(o.getIndividualsInSignature());
			obs.addAll(o.getObjectPropertiesInSignature());
		}
		return obs;
	}

	/**
	 * fetches the rdfs:label for an OWLObject
	 * 
	 * assumes zero or one rdfs:label
	 * 
	 * @param c
	 * @return label
	 */
	public String getLabel(OWLObject c) {
		OWLAnnotationProperty lap = dataFactory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()); 
		return getAnnotationValue(c, lap);
	}
	public String getLabelOrDisplayId(OWLObject c) {
		String label = getLabel(c);
		if (label == null) {
			if (c instanceof OWLNamedObject) {
				OWLNamedObject nc = (OWLNamedObject)c;
				label = nc.getIRI().getFragment();
			}
			else {
				label = c.toString();
			}
		}
		return label;
	}

	/**
	 * tests if an OWLObject has been declared obsolete in the source ontology
	 * 
	 * @param c
	 * @return boolean
	 */
	public boolean isObsolete(OWLObject c) {
		for (OWLAnnotation ann : ((OWLEntity)c).getAnnotations(getSourceOntology())) {
			if (ann.isDeprecatedIRIAnnotation()) {
				return true;
			}
		}
		return false;
	}


	/**
	 * gets the value of rdfs:comment for an OWLObject
	 * 
	 * @param c
	 * @return comment of null
	 */
	public String getComment(OWLObject c) {
		OWLAnnotationProperty lap = dataFactory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_COMMENT.getIRI()); 

		return getAnnotationValue(c, lap);
	}


	/**
	 * fetches the value of a single-valued annotation property for an OWLObject
	 * 
	 * TODO: provide a flag that determines behavior in the case of >1 value
	 * 
	 * @param c
	 * @param lap
	 * @return value
	 */
	public String getAnnotationValue(OWLObject c, OWLAnnotationProperty lap) {
		Set<OWLAnnotation>anns = new HashSet<OWLAnnotation>();
		if (c instanceof OWLEntity) {
			for (OWLOntology ont : getAllOntologies()) {
				anns.addAll(((OWLEntity) c).getAnnotations(ont,lap));
			}
		}
		else {
			return null;
		}
		for (OWLAnnotation a : anns) {
			if (a.getValue() instanceof OWLLiteral) {
				OWLLiteral val = (OWLLiteral) a.getValue();
				return val.getLiteral(); // return first - TODO - check zero or one
			}
		}
		return null;
	}

	/**
	 * gets the values of all annotation assertions to an OWLObject for a particular annotation property
	 * 
	 * @param c
	 * @param lap
	 * @return list of values or null
	 */
	public List<String> getAnnotationValues(OWLObject c, OWLAnnotationProperty lap) {
		Set<OWLAnnotation>anns = new HashSet<OWLAnnotation>();
		if (c instanceof OWLEntity) {
			for (OWLOntology ont : getAllOntologies()) {
				anns.addAll(((OWLEntity) c).getAnnotations(ont,lap));
			}
		}
		else {
			return null;
		}

		ArrayList<String> list = new ArrayList<String>();
		for (OWLAnnotation a : anns) {
			if (a.getValue() instanceof OWLLiteral) {
				OWLLiteral val = (OWLLiteral) a.getValue();
				list.add( val.getLiteral()); 
			}
			else if (a.getValue() instanceof IRI) {
				IRI val = (IRI)a.getValue();
				list.add( getIdentifier(val) ); 
			}

		}

		return list;
	}


	/**
	 * Gets the textual definition of an OWLObject
	 * 
	 * assumes zero or one def
	 * 
	 * 
	 * It returns the definition text (encoded as def in obo format and IAO_0000115 annotation property in OWL format) of a class
	 * @param c
	 * @return definition
	 */
	public String getDef(OWLObject c) {
		OWLAnnotationProperty lap = dataFactory.getOWLAnnotationProperty(Obo2OWLVocabulary.IRI_IAO_0000115.getIRI()); 

		return getAnnotationValue(c, lap);
	}

	/**
	 * It returns the value of the is_metadata_tag tag.
	 * @param c could OWLClass or OWLObjectProperty
	 * @return boolean
	 */
	public boolean getIsMetaTag(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_IS_METADATA_TAG.getTag());

		String val = getAnnotationValue(c, lap);

		return val == null ? false: Boolean.valueOf(val);
	}

	/**
	 * It returns the value of the subset tag.
	 * @param c could OWLClass or OWLObjectProperty
	 * @return values
	 */
	// TODO - return set
	public List<String> getSubsets(OWLObject c) {
		//OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_SUBSET.getTag());
		OWLAnnotationProperty lap = dataFactory.getOWLAnnotationProperty(Obo2OWLVocabulary.IRI_OIO_inSubset.getIRI());
		return getAnnotationValues(c, lap);
	}

	/**
	 * fetches all subset names that are used
	 * 
	 * @return all subsets used in source ontology
	 */
	public Set<String> getAllUsedSubsets() {
		Set<String> subsets = new HashSet<String>();
		for (OWLObject x : getAllOWLObjects()) {
			subsets.addAll(getSubsets(x));
		}
		return subsets;
	}

	/**
	 * given a subset name, find all OWLObjects (typically OWLClasses) assigned to that subset
	 * 
	 * @param subset
	 * @return set of {@link OWLObject}
	 */
	public Set<OWLObject> getOWLObjectsInSubset(String subset) {
		Set<OWLObject> objs = new HashSet<OWLObject>();
		for (OWLObject x : getAllOWLObjects()) {
			if (getSubsets(x).contains(subset))
				objs.add(x);
		}
		return objs;		
	}

	/**
	 * given a subset name, find all OWLClasses assigned to that subset
	 * @param subset
	 * @return set of {@link OWLClass}
	 */
	public Set<OWLClass> getOWLClassesInSubset(String subset) {
		Set<OWLClass> objs = new HashSet<OWLClass>();
		for (OWLObject x : getAllOWLObjects()) {
			if (getSubsets(x).contains(subset) && x instanceof OWLClass)
				objs.add((OWLClass) x);
		}
		return objs;		
	}


	/**
	 * It returns the value of the domain tag
	 * @param prop
	 * @return domain string or null
	 */
	public String getDomain(OWLObjectProperty prop){
		Set<OWLClassExpression> domains = prop.getDomains(ontology);

		for(OWLClassExpression ce: domains){
			return getIdentifier(ce);
		}

		return null;
	}


	/**
	 * It returns the value of the range tag
	 * @param prop
	 * @return range or null
	 */
	public String getRange(OWLObjectProperty prop){
		Set<OWLClassExpression> domains = prop.getRanges(ontology);

		for(OWLClassExpression ce: domains){
			return getIdentifier(ce);
		}

		return null;
	}


	/**
	 * It returns the values of the replaced_by tag or IAO_0100001 annotation.
	 * @param c could OWLClass or OWLObjectProperty
	 * @return list of values
	 */
	public List<String> getReplacedBy(OWLObject c) {
		OWLAnnotationProperty lap = dataFactory.getOWLAnnotationProperty(Obo2OWLVocabulary.IRI_IAO_0100001.getIRI());

		return getAnnotationValues(c, lap);
	}

	/**
	 * It returns the values of the consider tag.
	 * 
	 * @param c could OWLClass or OWLObjectProperty
	 * @return list of values
	 */
	public List<String> getConsider(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_CONSIDER.getTag());

		return getAnnotationValues(c, lap);
	}


	/**
	 * It returns the value of the is-obsolete tag.
	 * @param c could OWLClass or OWLObjectProperty
	 * @return boolean
	 */
	public boolean getIsObsolete(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_IS_OBSELETE.getTag()); 

		String val = getAnnotationValue(c, lap);

		return val == null ? false: Boolean.valueOf(val);
	}



	/**
	 * It returns the values of the alt_id tag
	 * @param c
	 * @return list of identifiers
	 */
	public List<String> getAltIds(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_ALT_ID.getTag());

		return getAnnotationValues(c, lap);
	}

	/**
	 * It returns the value of the builtin tag
	 * @param c
	 * @return
	 */
	@Deprecated
	public boolean getBuiltin(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_BUILTIN.getTag());

		String val = getAnnotationValue(c, lap);

		return val == null ? false: Boolean.valueOf(val);
	}

	/**
	 * It returns the value of the is_anonymous tag
	 * @param c
	 * @return
	 */
	@Deprecated
	public boolean getIsAnonymous(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_IS_ANONYMOUS.getTag());

		String val = getAnnotationValue(c, lap);

		return val == null ? false: Boolean.valueOf(val);
	}


	/**
	 * It translates a oboformat tag into an OWL annotation property
	 * @param tag
	 * @return {@link OWLAnnotationProperty}
	 */
	public OWLAnnotationProperty getAnnotationProperty(String tag){
		return dataFactory.getOWLAnnotationProperty(Obo2Owl.trTagToIRI(tag));
	}


	/**
	 * It returns the value of the OBO-namespace tag
	 * 
	 * Example: if the OWLObject is the GO class GO:0008150, this would return "biological_process"
	 * 
	 * @param c
	 * @return namespace
	 */
	public String getNamespace(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_NAMESPACE.getTag());

		return getAnnotationValue(c, lap);
	}


	/**
	 * It returns the value of the created_by tag
	 * @param c
	 * @return value or null
	 */
	public String getCreatedBy(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_CREATED_BY.getTag()); 

		return getAnnotationValue(c, lap);
	}


	/**
	 * It returns the value of the is_anti_symmetric tag or IAO_0000427 annotation
	 * @param c
	 * @return boolean
	 */
	public boolean getIsAntiSymmetric(OWLObject c) {
		OWLAnnotationProperty lap = dataFactory.getOWLAnnotationProperty(Obo2OWLVocabulary.IRI_IAO_0000427.getIRI());

		String val = getAnnotationValue(c, lap);

		return val == null ? false: Boolean.valueOf(val);
	}


	/**
	 * It returns the value of the is_cyclic tag 
	 * @param c
	 * @return boolean
	 */
	public boolean getIsCyclic(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_IS_CYCLIC.getTag()); 

		String val = getAnnotationValue(c, lap);

		return val == null ? false: Boolean.valueOf(val);
	}


	// TODO - fix for multiple ontologies
	/**
	 * true if c is transitive in the source ontology
	 * 
	 * @param c
	 * @return boolean
	 */
	public boolean getIsTransitive(OWLObjectProperty c) {
		Set<OWLTransitiveObjectPropertyAxiom> ax = sourceOntology.getTransitiveObjectPropertyAxioms(c);

		return ax.size()>0;
	}

	// TODO - fix for multiple ontologies
	/**
	 * true if c is functional in the source ontology
	 * @param c
	 * @return boolean
	 */
	public boolean getIsFunctional(OWLObjectProperty c) {
		Set<OWLFunctionalObjectPropertyAxiom> ax = sourceOntology.getFunctionalObjectPropertyAxioms(c);

		return ax.size()>0;
	}

	// TODO - fix for multiple ontologies
	public boolean getIsInverseFunctional(OWLObjectProperty c) {
		Set<OWLInverseFunctionalObjectPropertyAxiom> ax = sourceOntology.getInverseFunctionalObjectPropertyAxioms(c);

		return ax.size()>0;
	}



	// TODO - fix for multiple ontologies
	public boolean getIsReflexive(OWLObjectProperty c) {
		Set<OWLReflexiveObjectPropertyAxiom> ax = sourceOntology.getReflexiveObjectPropertyAxioms(c);

		return ax.size()>0;
	}

	// TODO - fix for multiple ontologies
	public boolean getIsSymmetric(OWLObjectProperty c) {
		Set<OWLSymmetricObjectPropertyAxiom> ax = sourceOntology.getSymmetricObjectPropertyAxioms(c);

		return ax.size()>0;
	}

	/**
	 * returns parent properties of p in all ontologies
	 * @param p
	 * @return set of properties
	 */
	public Set<OWLObjectPropertyExpression> getSuperPropertiesOf(OWLObjectPropertyExpression p) {
		Set<OWLObjectPropertyExpression> ps = new HashSet<OWLObjectPropertyExpression>();
		for (OWLOntology ont : getAllOntologies()) {
			for (OWLSubObjectPropertyOfAxiom a : ont.getObjectSubPropertyAxiomsForSubProperty(p)) {
				ps.add(a.getSuperProperty());
			}
		}
		return ps;
	}

	/**
	 * get the values of of the obo xref tag
	 * 
	 * @param c
	 * @return It returns null if no xref annotation is found.
	 */

	public List<String> getXref(OWLObject c){
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_XREF.getTag());

		Set<OWLAnnotation>anns = null;
		if (c instanceof OWLEntity) {
			anns = ((OWLEntity) c).getAnnotations(sourceOntology,lap);
		}
		else {
			return null;
		}
		List<String> list = new ArrayList<String>();
		for (OWLAnnotation a : anns) {

			if (a.getValue() instanceof OWLLiteral) {
				OWLLiteral val = (OWLLiteral) a.getValue();
				list.add( val.getLiteral()) ;
			}
		}
		return list;
	}


	/**
	 * Get the definition xrefs (IAO_0000115)
	 * 
	 * @param c
	 * @return list of definition xrefs
	 */
	public List<String> getDefXref(OWLObject c){
		OWLAnnotationProperty lap = dataFactory.getOWLAnnotationProperty(Obo2OWLVocabulary.IRI_IAO_0000115.getIRI()); 
		OWLAnnotationProperty xap = getAnnotationProperty(OboFormatTag.TAG_XREF.getTag());

		List<String> list = new ArrayList<String>();

		if(c instanceof OWLEntity){
			for (OWLAnnotationAssertionAxiom oaax :((OWLEntity) c).getAnnotationAssertionAxioms(sourceOntology)){

				if(lap.equals(oaax.getProperty())){

					for(OWLAnnotation a: oaax.getAnnotations(xap)){
						if(a.getValue() instanceof OWLLiteral){
							list.add( ((OWLLiteral)a.getValue()).getLiteral() );
						}
					}
				}

			}
		}

		return list;
	}


	/**
	 * Return the names of the asserted subClasses of the cls (Class) 
	 * passed in the argument
	 * 
	 * 
	 * @param cls
	 * @return
	 */
	@Deprecated
	public String[] getSubClassesNames(OWLClass cls){
		Set<OWLClassExpression> st = cls.getSubClasses(sourceOntology);


		ArrayList<String> ar = new ArrayList<String>();
		for(OWLClassExpression ce: st){
			if(ce instanceof OWLNamedObject)
				ar.add(getLabel(ce)); 
		}

		return ar.toArray(new String[ar.size()]);
	}

	/**
	 * It returns array of synonyms (is encoded as synonym in obo format and IAO_0000118 annotation property in OWL format) of a class
	 * @param c
	 * @return
	 */
	@Deprecated
	public String[] getSynonymStrings(OWLObject c) {
		OWLAnnotationProperty lap = dataFactory.getOWLAnnotationProperty(IRI.create(DEFAULT_IRI_PREFIX + "IAO_0000118")); 
		Set<OWLAnnotation>anns = null;
		if (c instanceof OWLEntity) {
			anns = ((OWLEntity) c).getAnnotations(sourceOntology,lap);
		}
		else {
			return null;
		}

		ArrayList<String> list = new ArrayList<String>();
		for (OWLAnnotation a : anns) {
			if (a.getValue() instanceof OWLLiteral) {
				OWLLiteral val = (OWLLiteral) a.getValue();
				list.add(val.getLiteral()); // return first - todo - check zero or one
			}
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * It returns array of synonyms as encoded by OBO2OWL.
	 * 
	 * @param c
	 * @return list of synonyms
	 */
	public List<ISynonym> getOBOSynonyms(OWLObject c) {
		OWLEntity e;
		if (c instanceof OWLEntity) {
			e = (OWLEntity) c;
		}
		else {
			return null;
		}
		List<ISynonym> synonyms = null;

		synonyms = merge(synonyms, getOBOSynonyms(e, Obo2OWLVocabulary.IRI_OIO_hasBroadSynonym));
		synonyms = merge(synonyms, getOBOSynonyms(e, Obo2OWLVocabulary.IRI_OIO_hasExactSynonym));
		synonyms = merge(synonyms, getOBOSynonyms(e, Obo2OWLVocabulary.IRI_OIO_hasNarrowSynonym));
		synonyms = merge(synonyms, getOBOSynonyms(e, Obo2OWLVocabulary.IRI_OIO_hasRelatedSynonym));

		return synonyms;
	}

	private <T> List<T> merge(List<T> list1, List<T> list2) {
		if (list1 == null || list1.isEmpty()) {
			return list2;
		}
		if (list2 == null || list2.isEmpty()) {
			return list1;
		}
		List<T> synonyms = new ArrayList<T>(list1.size() + list2.size());
		synonyms.addAll(list1);
		synonyms.addAll(list2);
		return synonyms;
	}

	private List<ISynonym> getOBOSynonyms(OWLEntity e, Obo2OWLVocabulary vocabulary) {
		OWLAnnotationProperty property = dataFactory.getOWLAnnotationProperty(vocabulary.getIRI());
		Set<OWLAnnotation> anns = e.getAnnotations(sourceOntology, property);
		Set<OWLAnnotationAssertionAxiom> annotationAssertionAxioms = e.getAnnotationAssertionAxioms(sourceOntology);
		if (anns != null && !anns.isEmpty()) {
			ArrayList<ISynonym> list = new ArrayList<ISynonym>(anns.size());
			for (OWLAnnotation a : anns) {
				if (a.getValue() instanceof OWLLiteral) {
					OWLLiteral val = (OWLLiteral) a.getValue();
					String label = val.getLiteral();
					if (label != null && label.length() > 0) {
						Set<String> xrefs = getOBOSynonymXrefs(annotationAssertionAxioms, val, property);
						Synonym s = new Synonym(label, vocabulary.getMappedTag(), null, xrefs);
						list.add(s);
					}
				}
			}
			if (!list.isEmpty()) {
				return list;
			}
		}
		return null;
	}

	private Set<String> getOBOSynonymXrefs(Set<OWLAnnotationAssertionAxiom> annotationAssertionAxioms, OWLLiteral val, OWLAnnotationProperty property) {

		if (annotationAssertionAxioms == null || annotationAssertionAxioms.isEmpty()) {
			return null;
		}
		Set<String> xrefs = new HashSet<String>();
		for (OWLAnnotationAssertionAxiom annotationAssertionAxiom : annotationAssertionAxioms) {
			// check if it is the correct property
			if (!property.equals(annotationAssertionAxiom.getProperty())) {
				continue;
			}

			// check if its is the corresponding value
			if (!val.equals(annotationAssertionAxiom.getValue())) {
				continue;
			}
			Set<OWLAnnotation> annotations = annotationAssertionAxiom.getAnnotations();
			for (OWLAnnotation owlAnnotation : annotations) {
				if (owlAnnotation.getValue() instanceof OWLLiteral) {
					OWLLiteral xrefLiteral = (OWLLiteral) owlAnnotation.getValue();
					String xref = xrefLiteral.getLiteral();
					xrefs.add(xref);
				}
			}
		}
		if (!xrefs.isEmpty()) {
			return xrefs;
		}
		return null;
	}

	public static interface ISynonym {
		/**
		 * @return the label
		 */
		public String getLabel();

		/**
		 * @return the scope
		 */
		public String getScope();

		/**
		 * @return the category
		 */
		public String getCategory();

		/**
		 * @return the xrefs
		 */
		public Set<String> getXrefs();
	}


	public static class Synonym implements ISynonym {
		private String label;
		private String scope;
		private String category;
		private Set<String>  xrefs;

		/**
		 * @param label
		 * @param scope
		 * @param category
		 * @param xrefs
		 */
		public Synonym(String label, String scope, String category, Set<String> xrefs) {
			super();
			this.label = label;
			this.scope = scope;
			this.category = category;
			this.xrefs = xrefs;
		}

		//@Override
		public String getLabel() {
			return label;
		}

		//@Override
		public String getScope() {
			return scope;
		}

		//@Override
		public String getCategory() {
			return category;
		}

		//@Override
		public Set<String> getXrefs() {
			return xrefs;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Synonym [");
			builder.append("label=");
			builder.append(label);
			if (scope != null) {
				builder.append(", scope=");
				builder.append(scope);
			}
			if (category != null) {
				builder.append(", category=");
				builder.append(category);
			}
			if (xrefs != null) {
				builder.append(", xrefs=");
				builder.append(xrefs);
			}
			builder.append("]");
			return builder.toString();
		}
	}

	/**
	 * gets the OBO-style ID of the source ontology IRI. E.g. "go"
	 * 
	 * @return id of source ontology
	 */
	public String getOntologyId(){
		return Owl2Obo.getOntologyId(this.getSourceOntology());
	}

	/**
	 * gets the OBO-style ID of the specified object. E.g. "GO:0008150"
	 * 
	 * @param owlObject
	 * @return OBO-style identifier, using obo2owl mapping
	 */
	public String getIdentifier(OWLObject owlObject) {
		return Owl2Obo.getIdentifierFromObject(owlObject, this.sourceOntology, null);
	}


	/**
	 * gets the OBO-style ID of the specified object. E.g. "GO:0008150"
	 * 
	 * @param iriId
	 * @return OBO-style identifier, using obo2owl mapping
	 */
	public String getIdentifier(IRI iriId) {
		return Owl2Obo.getIdentifier(iriId);
	}

	// TODO - use Obo2Owl.oboIdToIRI()
	public IRI getIRIByIdentifier(String id) {
		// TODO - provide a static method for doing this
		Obo2Owl b = new Obo2Owl();
		b.setObodoc(new OBODoc());
		return b.oboIdToIRI(id);
		/*
		new oboIdToIRI()
		String[] parts = id.split(":", 2);
		String s;
		if (parts.length <2) {
			// TODO!
			s = "http://purl.obolibrary.org/obo/TODO_"+parts[0];
		}
		else {
			s = "http://purl.obolibrary.org/obo/"+parts[0]+"_"+parts[1];
		}

		return IRI.create(s);
		*/
	}

	/**
	 * Given an OBO-style ID, return the corresponding OWLObject, if it is declared - otherwise null
	 * 
	 * @param id - e.g. GO:0008150
	 * @return object with id or null
	 */
	public OWLObject getOWLObjectByIdentifier(String id) {
		IRI iri = getIRIByIdentifier(id);
		if (getOWLClass(iri) != null) {
			return getOWLClass(iri);
		}
		else if (getOWLIndividual(iri) != null) {
			return getOWLIndividual(iri);
		}
		else if (getOWLObjectProperty(iri) != null) {
			return getOWLObjectProperty(iri);
		}
		return null;
	}

	/**
	 * Given an OBO-style ID, return the corresponding OWLObjectProperty, if it is declared - otherwise null
	 * 
	 * @param id - e.g. GO:0008150
	 * @return OWLObjectProperty with id or null
	 */
	public OWLObjectProperty getOWLObjectPropertyByIdentifier(String id) {
		return dataFactory.getOWLObjectProperty(getIRIByIdentifier(id));
	}

	/**
	 * Given an OBO-style ID, return the corresponding OWLNamedIndividual, if it is declared - otherwise null
	 * 
	 * @param id - e.g. GO:0008150
	 * @return OWLNamedIndividual with id or null
	 */
	public OWLNamedIndividual getOWLIndividualByIdentifier(String id) {
		return dataFactory.getOWLNamedIndividual(getIRIByIdentifier(id));
	}

	/**
	 * Given an OBO-style ID, return the corresponding OWLClass, if it is declared - otherwise null
	 * 
	 * @param id - e.g. GO:0008150
	 * @return OWLClass with id or null
	 */
	public OWLClass getOWLClassByIdentifier(String id) {
		IRI iri = getIRIByIdentifier(id);
		if (iri != null)
			return getOWLClass(iri);
		return null;
	}

	/**
	 * fetches an OWL Object by rdfs:label
	 * 
	 * if there is >1 match, return the first one encountered
	 * 
	 * @param label
	 * @return object or null
	 */
	public OWLObject getOWLObjectByLabel(String label) {
		IRI iri = getIRIByLabel(label);
		if (iri != null)
			return getOWLObject(iri);
		return null;
	}

	/**
	 * fetches an OWL IRI by rdfs:label
	 * 
	 * @param label
	 * @return IRI or null
	 */
	public IRI getIRIByLabel(String label) {
		try {
			return getIRIByLabel(label, false);
		} catch (SharedLabelException e) {
			// note that it should be impossible to reach this point
			// if getIRIByLabel is called with isEnforceUnivocal = false
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * fetches an OWL IRI by rdfs:label, optionally testing for uniqueness
	 *
	 * TODO: index labels. This currently scans all labels in the ontology, which is expensive
	 * 
	 * @param label
	 * @param isEnforceUnivocal
	 * @return IRI or null
	 * @throws SharedLabelException if >1 IRI shares input label
	 */
	public IRI getIRIByLabel(String label, boolean isEnforceUnivocal) throws SharedLabelException {
		IRI iri = null;
		for (OWLOntology o : getAllOntologies()) {
			Set<OWLAnnotationAssertionAxiom> aas = o.getAxioms(AxiomType.ANNOTATION_ASSERTION);
			for (OWLAnnotationAssertionAxiom aa : aas) {
				OWLAnnotationValue v = aa.getValue();
				OWLAnnotationProperty property = aa.getProperty();
				if (property.isLabel() && v instanceof OWLLiteral) {
					if (label.equals( ((OWLLiteral)v).getLiteral())) {
						OWLAnnotationSubject subject = aa.getSubject();
						if (subject instanceof IRI) {
							if (isEnforceUnivocal) {
								if (iri != null && !iri.equals((IRI)subject)) {
									throw new SharedLabelException(label,iri,(IRI)subject);
								}
								iri = (IRI)subject;
							}
							else {
								return (IRI)subject;
							}
						}
						else {
							//return null;
						}
					}
				}
			}
		}
		return iri;
	}



	/**
	 * Returns an OWLClass given an IRI string
	 * 
	 * the class must be declared in either the source ontology, or in a support ontology,
	 * otherwise null is returned
	 * 
	 * @param s IRI string
	 * @return {@link OWLClass}
	 */
	public OWLClass getOWLClass(String s) {
		IRI iri = IRI.create(s);
		return getOWLClass(iri);
	}

	/**
	 * Returns an OWLClass given an IRI
	 * 
	 * the class must be declared in either the source ontology, or in a support ontology,
	 * otherwise null is returned
	 *
	 * @param iri
	 * @return {@link OWLClass}
	 */
	public OWLClass getOWLClass(IRI iri) {
		OWLClass c = getDataFactory().getOWLClass(iri);
		for (OWLOntology o : getAllOntologies()) {
			if (o.getDeclarationAxioms(c).size() > 0) {
				return c;
			}
		}
		return null;
	}

	/**
	 * @param x
	 * @return {@link OWLClass}
	 */
	public OWLClass getOWLClass(OWLObject x) {
		return dataFactory.getOWLClass(((OWLNamedObject)x).getIRI());
	}


	/**
	 * Returns an OWLNamedIndividual with this IRI <b>if it has been declared</b>
	 * in the source or support ontologies. Returns null otherwise.
	 * @param iri
	 * @return {@link OWLNamedIndividual}
	 */
	public OWLNamedIndividual getOWLIndividual(IRI iri) {
		OWLNamedIndividual c = dataFactory.getOWLNamedIndividual(iri);
		for (OWLOntology o : getAllOntologies()) {
			for (OWLDeclarationAxiom da : o.getDeclarationAxioms(c)) {
				if (da.getEntity() instanceof OWLNamedIndividual) {
					return (OWLNamedIndividual) da.getEntity();
				}
			}
		}
		return null;
	}

	/**
	 * @see #getOWLIndividual(IRI)
	 * @param s
	 * @return {@link OWLNamedIndividual}
	 */
	public OWLNamedIndividual getOWLIndividual(String s) {
		IRI iri = IRI.create(s);
		return getOWLIndividual(iri);
	}

	/**
	 * Returns the OWLObjectProperty with this IRI
	 * 
	 * Must have been declared in one of the ontologies
	 * 
	 * @param iri
	 * @return {@link OWLObjectProperty}
	 */
	public OWLObjectProperty getOWLObjectProperty(String iri) {
		return getOWLObjectProperty(IRI.create(iri));
	}

	public OWLObjectProperty getOWLObjectProperty(IRI iri) {
		OWLObjectProperty p = dataFactory.getOWLObjectProperty(iri);
		for (OWLOntology o : getAllOntologies()) {
			if (o.getDeclarationAxioms(p).size() > 0) {
				return p;
			}
		}
		return null;
	}


	/**
	 * Returns the OWLObject with this IRI
	 * (where IRI is specified as a string - e.g http://purl.obolibrary.org/obo/GO_0008150)
	 * 
	 * @param s IRI string
	 * @see #getOWLObject(IRI iri)
	 * @return {@link OWLObject}
	 */
	public OWLObject getOWLObject(String s) {
		return getOWLObject(IRI.create(s));
	}

	/**
	 * Returns the OWLObject with this IRI
	 * 
	 * Must have been declared in one of the ontologies
	 * 
	 * Currently OWLObject must be one of OWLClass, OWLObjectProperty or OWLNamedIndividual
	 * 
	 * If the ontology employs punning and there different entities with the same IRI, then
	 * the order of precedence is OWLClass then OWLObjectProperty then OWLNamedIndividual
	 *
	 * @param s entity IRI
	 * @return {@link OWLObject}
	 */
	public OWLObject getOWLObject(IRI s) {
		OWLObject o;
		o = getOWLClass(s);
		if (o == null) {
			o = getOWLIndividual(s);
		}
		if (o == null) {
			o = getOWLObjectProperty(s);
		}
		return o;
	}


}
