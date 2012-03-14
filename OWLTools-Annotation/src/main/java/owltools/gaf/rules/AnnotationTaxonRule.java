package owltools.gaf.rules;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;

/**
 * Checks if an annotation is valid according to taxon constraints.
 * 
 * @author cjm
 */
public class AnnotationTaxonRule extends AbstractAnnotationRule {
	
	private final OWLGraphWrapper graphWrapper;
	private final OWLGraphWrapper taxGraphWrapper;
	private final OWLObject rNever;
	private final OWLObject rOnly;
	
	/**
	 * @param graphWrapper
	 * @param taxGraphWrapper
	 * @param neverId
	 * @param onlyId 
	 */
	public AnnotationTaxonRule(OWLGraphWrapper graphWrapper, OWLGraphWrapper taxGraphWrapper, 
			String neverId, String onlyId) {
		
		if(graphWrapper == null || taxGraphWrapper == null){
			throw new IllegalArgumentException("OWLGraphWrappers may not be null");
		}
		this.graphWrapper = graphWrapper;
		this.taxGraphWrapper = taxGraphWrapper;
		rNever = graphWrapper.getOWLObjectPropertyByIdentifier(neverId);
		rOnly = graphWrapper.getOWLObjectPropertyByIdentifier(onlyId);
	}

	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {
		return _getRuleViolations(a.getCls(), a.getBioentityObject().getNcbiTaxonId(),a);
	}

	public boolean check(String annotationCls, String taxonCls) {
		return getRuleViolations(annotationCls, taxonCls).size() == 0;
	
	}

	public Set<AnnotationRuleViolation> getRuleViolations(String annotationCls, String taxonCls) {
		return _getRuleViolations(annotationCls, taxonCls,null);
	}
	
	private Set<AnnotationRuleViolation> _getRuleViolations(String annotationCls, String taxonCls, GeneAnnotation a) {
		Set<AnnotationRuleViolation> violations = new HashSet<AnnotationRuleViolation>();
	
		if(taxonCls == null){
			AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), "Taxon id is null", a);
			violations.add(v);
			return violations;
		}
		
		OWLObject cls = graphWrapper.getOWLObjectByIdentifier(annotationCls);
		OWLObject tax = taxGraphWrapper.getOWLObjectByIdentifier(taxonCls);
		
		
		Set<OWLGraphEdge> edges = taxGraphWrapper.getOutgoingEdgesClosure(cls);

		for (OWLGraphEdge ge : edges) {
			OWLObject tgt = ge.getTarget();
			if (!(tgt instanceof OWLNamedObject))
				continue;
			OWLObject p = taxGraphWrapper.getOWLClass(tgt);
			if (p == null) {
				continue;
			}
			OWLQuantifiedProperty qp = ge.getLastQuantifiedProperty();
			if (qp.isQuantified() && qp.getProperty().equals(rOnly)) {
				// ONLY
				if (!taxGraphWrapper.getAncestorsReflexive(tax).contains(p)) {
					StringBuilder sb = new StringBuilder();
					sb.append("The term ");
					renderEntity(sb, cls, graphWrapper);
					sb.append("requires the taxon ");
					renderEntity(sb, p, taxGraphWrapper);
					sb.append(", but ");
					renderEntity(sb, tax, taxGraphWrapper);
					sb.append(" does not match this constraint");
					// System.out.println("   "+sb);
					
					AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), sb.toString(), a);
					violations.add(v);
				}
			}
			else if (qp.isQuantified() && qp.getProperty().equals(rNever)) {
				// NEVER
				if (taxGraphWrapper.getAncestorsReflexive(tax).contains(p)) {
					StringBuilder sb = new StringBuilder();
					sb.append("The term ");
					renderEntity(sb, cls, graphWrapper);
					sb.append(" may not be used with ");
					renderEntity(sb, tax, taxGraphWrapper);
					sb.append(" reason: excluded ancestor ");
					renderEntity(sb, p, taxGraphWrapper);
					AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), sb.toString(), a);
					violations.add(v);
				}
			}
		}
		return violations;	
	}
	
	private void renderEntity(StringBuilder sb, OWLObject o, OWLGraphWrapper graph) {
		sb.append(graph.getIdentifier(o));
		String label = graph.getLabel(o);
		if (label != null) {
			sb.append(" '");
			sb.append(label);
			sb.append('\'');
		}
	}
}

