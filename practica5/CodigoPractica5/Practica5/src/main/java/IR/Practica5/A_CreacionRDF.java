package IR.Practica5;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.VCARD;

/**
 * Ejemplo de como construir un modelo de Jena y añadir nuevos recursos 
 * mediante la clase Model
 */
public class A_CreacionRDF {
	
	/**
	 * muestra un modelo de jena de ejemplo por pantalla
	 */
	public static void main (String args[]) {
        Model model = A_CreacionRDF.generarEjemplo();
        // write the model in the standar output
        model.write(System.out); 
    }
	
	/**
	 * Genera un modelo de jena de ejemplo
	 */
	public static Model generarEjemplo(){
		// definiciones
        String personURI    = "http://somewhere/JohnSmith";
        String givenName    = "John";
        String familyName   = "Smith";
        String fullName     = givenName + " " + familyName;

        String personURI1   = "http://somewhere/HugoMateo";
        String givenName1   = "Hugo";
        String familyName1  = "Mateo";
        String fullName1    = givenName1 + " " + familyName1;

        String personURI2   = "http://somewhere/JaimeBielsa";
        String givenName2   = "Jaime";
        String familyName2  = "Bielsa";
        String fullName2    = givenName2 + " " + familyName2;

        // crea un modelo vacio
        Model model = ModelFactory.createDefaultModel();

        // le a�ade las propiedades
        Resource johnSmith = model.createResource(personURI)
             .addProperty(VCARD.FN, fullName)
             .addProperty(VCARD.N, 
                      model.createResource()
                           .addProperty(VCARD.Given, givenName)
                           .addProperty(VCARD.Family, familyName));

        Resource hugoMateo = model.createResource(personURI1)
                .addProperty(VCARD.FN, fullName1)
                .addProperty(VCARD.N,
                        model.createResource()
                                .addProperty(VCARD.Given, givenName1)
                                .addProperty(VCARD.Family, familyName1));

        Resource jaimeBielsa = model.createResource(personURI2)
                .addProperty(VCARD.FN, fullName2)
                .addProperty(VCARD.N,
                        model.createResource()
                                .addProperty(VCARD.Given, givenName2)
                                .addProperty(VCARD.Family, familyName2));

        Resource r = model.createResource(personURI);
        Property p = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        RDFNode n = model.createResource("http://xmlns.com/foaf/0.1/person");
        model.add(r, p, n);

        Resource r1 = model.createResource(personURI1);
        Property p1 = model.createProperty("http://xmlns.com/foaf/0.1/knows");
        RDFNode n1 = model.createResource(personURI2);
        model.add(r1, p1, n1);

        Resource r2 = model.createResource(personURI2);
        Property p2 = model.createProperty("http://xmlns.com/foaf/0.1/knows");
        RDFNode n2 = model.createResource(personURI1);
        model.add(r2, p2, n2);

        return model;

	}
	
	
}
