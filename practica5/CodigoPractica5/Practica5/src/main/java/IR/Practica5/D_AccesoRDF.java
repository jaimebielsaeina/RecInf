package IR.Practica5;

import openllet.core.rules.rete.Tuple;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Ejemplo de lectura de un modelo RDF de un fichero de texto 
 * y como acceder por API a los elementos que contiene
 */
public class D_AccesoRDF {
	
	public static class RelAndObj {
		public String rel, obj;
		public boolean isLiteral;
		public RelAndObj (Statement s) {
			rel = s.getPredicate().getURI();
			isLiteral = s.getObject().isLiteral();
			obj = isLiteral ? s.getLiteral().toString() : s.getResource().getURI();
		}
		public boolean equals (Statement s) {
			return rel.equals(s.getPredicate().getURI())
				&& isLiteral == s.getObject().isLiteral()
				&& obj.equals(isLiteral ? s.getLiteral().toString() : s.getResource().getURI());
		}
	}
	
	public static void main(String args[]) {

		// cargamos el fichero deseado
		Model model = FileManager.get().loadModel("card.rdf");

		// obtenemos todos los statements del modelo
		StmtIterator it = model.listStatements();

		// mostramos todas las tripletas cuyo objeto es un literal
		while (it.hasNext()) {
			Statement st = it.next();

			if (st.getObject().isLiteral()) {
				System.out.println(st.getSubject().getURI() + " - "
						+ st.getPredicate().getURI() + " - "
						+ st.getLiteral().toString());
			}
		}

		System.out.println("----------------------------------------");

		// mostramos los valores de todas las propiedades de un recurso
		// determinado
		Resource res = model
				.getResource("http://dig.csail.mit.edu/2008/webdav/timbl/foaf.rdf");
		it = res.listProperties();
		while (it.hasNext()) {
			Statement st = it.next();

			if (st.getObject().isLiteral()) {
				System.out.println(st.getSubject().getURI() + " - "
						+ st.getPredicate().getURI() + " - "
						+ st.getLiteral().toString());
			} else {
				System.out.println(st.getSubject().getURI() + " - "
						+ st.getPredicate().getURI() + " - "
						+ st.getResource().getURI());
			}
		}

		System.out.println("----------------------------------------");

		// mostramos todos los recursos que contienen una propiedad determinada
		Property prop = model
				.getProperty("http://purl.org/dc/elements/1.1/title");
		ResIterator ri = model.listSubjectsWithProperty(prop);
		while (ri.hasNext()) {
			Resource r = ri.next();
			System.out.println(r.getURI());
		}

		System.out.println("----------------------------------------");

		// mostramos todos los recursos que contienen una propiedad determinada
		// forma alternativa que usa un filtro sobre los statements a recuperar
		it = model.listStatements(null, prop, (RDFNode) null);
		while (it.hasNext()) {
			Statement st = it.next();

			if (st.getObject().isLiteral()) {
				System.out.println(st.getSubject().getURI() + " - "
						+ st.getPredicate().getURI() + " - "
						+ st.getLiteral().toString());
			}
		}

		System.out.println("----------------------------------------");

		// mostramos todos los sujetos que comparten una propiedad con otro sujeto
		String ourURI = "http://dig.csail.mit.edu/2008/webdav/timbl/foaf.rdf";

		List<RelAndObj> properties = new ArrayList<RelAndObj>();
		res = model.getResource(ourURI);
		it = res.listProperties();
		while (it.hasNext()) {
			Statement st = it.next();

			if (st.getSubject().getURI().equals(ourURI)) {
				properties.add(new RelAndObj(st));
				System.out.println(st.getSubject().getURI() + " - "
						+ st.getPredicate().getURI() + " - "
						+ (st.getObject().isLiteral() ? st.getLiteral().toString() : st.getResource().getURI()));
			}
		}
		System.out.println("········································");
		it = model.listStatements();
		while (it.hasNext()) {
			Statement st = it.next();
			for (RelAndObj rao : properties)
					if (rao.equals(st)) {
						System.out.println(st.getSubject().getURI() + " - "
								+ st.getPredicate().getURI() + " - "
								+ (st.getObject().isLiteral() ? st.getLiteral().toString() : st.getResource().getURI()));
			}
		}
	}

}
