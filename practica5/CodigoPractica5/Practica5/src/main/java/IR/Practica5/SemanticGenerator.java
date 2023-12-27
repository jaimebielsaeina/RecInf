package IR.Practica5;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.jena.tdb2.TDB2Factory;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SemanticGenerator {

    // Field names.
    final private static String[] fieldNames = {
        "title",
        "subject",
        "type",
        "description",
        "creator",
        "publisher",
        "format",
        "language",
        "contributor",
        "relation",
        "rights",
        "date",
        "created",
        "issued",
    };

    // Field URIs.
    final private static Property[] fieldRelations = new Property[fieldNames.length];

    /** Index all text files under a directory. */
    public static void main(String[] args) {
        String usage = "java org.apache.lucene.demo.IndexFiles"
                + " [-rdf RDF_PATH] [-docs DOCS_PATH]\n\n"
                + "This generates RDF graphs in RDF_PATH from the documents"
                + "in INDEX_PATH.";
        String rdfPath = null;
        String docsPath = null;
        for(int i=0; i<args.length; i++) {
            if ("-rdf".equals(args[i])) {
                rdfPath = args[i+1];
                i++;
            } else if ("-docs".equals(args[i])) {
                docsPath = args[i+1];
                i++;
            }
        }

        if (rdfPath == null || docsPath == null) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        final File docDir = new File(docsPath);
        if (!docDir.exists() || !docDir.canRead()) {
            System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        Date start = new Date();

        // Creates an empty model.
        Model model = ModelFactory.createDefaultModel();

        // Generates the relations which will be used which will be used to make the trios.
        for (int i = 0; i < fieldNames.length; i++)
                fieldRelations[i] = model.createProperty("http://purl.org/dc/elements/1.1/" + fieldNames[i]);

        try {

            System.out.println("Creating RDF graph on directory '" + rdfPath + "'...");

            addDocsToRDFGraph(model, docDir);

            Date end = new Date();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds");

            //lo guardamos en un fichero rdf en formato xml
            model.write(new FileOutputStream(new File("data.rdf")), "RDF/XML-ABBREV");

            /*// Deleting the previous information on the directory expected to store the data.
            FileUtils.deleteDirectory(new File(rdfPath));
		    Dataset data = TDB2Factory.connectDataset(rdfPath + "/tdb2");

            // Making a write transaction and confirming the changes.
            data.begin(ReadWrite.WRITE) ;
            data.getDefaultModel().add(model);
            data.commit();
            data.end();*/

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }

    private static void addDocsToRDFGraph(Model model, File file) throws IOException {
        if (file.canRead()) {
            if (file.isDirectory()) {
                String[] files = file.list();
                if (files != null) {
                    for (String f : files) {
                        addDocsToRDFGraph(model, new File(file, f));
                    }
                }
            } else {
                System.out.println("adding " + file);
                addDocToRDFGraph(model, file);
            }
        }
    }

    private static void addDocToRDFGraph(Model model, File file) throws IOException {

        try {

            // Create a DocumentBuilderFactory.
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Create a DocumentBuilder.
            DocumentBuilder builder = factory.newDocumentBuilder();
            // Parse the XML file.
            org.w3c.dom.Document document = builder.parse(file);

            // Getting the identifier. It will be needed for every trio.
            String docIdent = document.getElementsByTagName("dc:identifier").item(0).getTextContent();
            Resource res = model.createResource(docIdent);

            NodeList elements;
            for (int i=0; i<fieldNames.length; i++) {
                // Traverse and manipulate the XML document.
                elements = document.getElementsByTagName("dc:" + fieldNames[i]);

                // For each property of the document, create a trio linking it as the following:
                // <identifier> dc:<property> <literal>
                for  (int j=0; j<elements.getLength(); j++) {
                    RDFNode object = model.createLiteral(((Element)elements.item(j)).getTextContent());
                    model.add (res, fieldRelations[i], object);
                }
            }

        } catch (Exception e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }

    }

}
