package owltools.io;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;

/**
 * Renders the graph closure - i.e. the inferred paths emanating from all named entities
 * 
 * @author cjm
 *
 */
public class TableRenderer extends AbstractRenderer implements GraphRenderer {

	public TableRenderer(PrintStream stream) {
		super(stream);
	}

	public TableRenderer(String file) {
		super(file);
	}
	

	public void render(OWLGraphWrapper g) {
		graph = g;
		
		Set<OWLObject> objs = new HashSet<OWLObject>(g.getSourceOntology().getClassesInSignature(false));
		objs.addAll(g.getSourceOntology().getIndividualsInSignature(false));

		for (OWLObject obj : objs) {
			if (obj.equals(g.getDataFactory().getOWLNothing()))
				continue;
			if (obj.equals(g.getDataFactory().getOWLThing()))
				continue;
			if (obj instanceof OWLNamedObject)
				render((OWLNamedObject)obj);
		}
		stream.close();
	}
	

	// TODO - make this configurable
	private void render(OWLNamedObject obj) {
		print(obj.getIRI().toString());
		sep();
		print(graph.getLabel(obj));
		sep();
		print(graph.getDef(obj));
		nl();
	}

	

}

