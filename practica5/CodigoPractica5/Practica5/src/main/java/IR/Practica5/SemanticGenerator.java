package IR.Practica5;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Date;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.VCARD;
import org.apache.jena.rdf.model.Resource;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/*import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.RankDir;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;*/


public class SemanticGenerator {
    //private IndexFiles() {}
    final private static String[] fieldNames = {
        "title",
        //"identifier",
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

        // crea un modelo vacio
        Model model = ModelFactory.createDefaultModel();

        for (int i = 0; i < fieldNames.length; i++) {
            fieldRelations[i] = model.createProperty("http://purl.org/dc/elements/1.1/" + fieldNames[i]);
        }

        try {
            System.out.println("Creating RDF graph on directory '" + rdfPath + "'...");

            Directory dir = FSDirectory.open(Paths.get(rdfPath));
            //Analyzer analyzer = new SpanishAnalyzer2();
            //IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

            addDocsToRDFGraph(model, docDir);

            Date end = new Date();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds");

            // Print the RDF graph
            // printRDFGraph(model);
            //plotRDF(model);

            FileUtils.deleteDirectory(new File(rdfPath));
		    Dataset data = TDB2Factory.connectDataset(rdfPath);
            //hacemos una transacción de escritura y confirmamos los cambios
            data.begin(ReadWrite.WRITE) ;
            data.getDefaultModel().add(model);
            data.commit();
            data.end();

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

            String docIdent = document.getElementsByTagName("dc:identifier").item(0).getTextContent();
            Resource res = model.createResource(docIdent);

            NodeList elements;
            for (int i=0; i<fieldNames.length; i++) {
                // Traverse and manipulate the XML document.
                elements = document.getElementsByTagName("dc:" + fieldNames[i]);

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

    private static void printRDFGraph(Model model) {
        // Iterate over statements in the model
        StmtIterator iterator = model.listStatements();
        while (iterator.hasNext()) {
            Statement statement = iterator.next();
            System.out.println(statement);
        }
    }
    
    /*public static void plotRDF(Model model) throws IOException
    {
        String dot = convertToDOT(model);
        renderDOT(dot, "output.png");
    }*/

    /*public static void renderDOT(String dot, String outputPath) throws IOException {
        MutableGraph g = Graphviz.fromString(dot).engine(Format.DOT).render(RankDir.LEFT_TO_RIGHT);

        // You can customize the graph appearance using attributes
        g.graphAttrs()
                .add(Font.name("Arial"))
                .add(Color.WHITE);
        g.nodeAttrs().add(Shape.RECTANGLE);

        // Save the graph as an image
        Graphviz.fromGraph(g).width(800).render(Format.PNG).toFile(new File(outputPath));*/


        /*try {
            // Write DOT content to a file
            FileWriter fileWriter = new FileWriter("graph.dot");
            fileWriter.write(dot);
            fileWriter.close();

            // Execute the Graphviz dot command to generate an image
            Process process = Runtime.getRuntime().exec("dot -Tpng graph.dot -o " + outputImagePath);
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }*/
    //}



    public static String convertToDOT(Model model) {
        StringBuilder dotStringBuilder = new StringBuilder();
        dotStringBuilder.append("digraph RDFGraph {\n");

        // Iterate over statements in the model
        StmtIterator iterator = model.listStatements();
        while (iterator.hasNext()) {
            Statement statement = iterator.next();
            String subject = statement.getSubject().toString();
            String predicate = statement.getPredicate().toString();
            String object = statement.getObject().toString();

            // Escape special characters
            subject = escapeForDOT(subject);
            predicate = escapeForDOT(predicate);
            object = escapeForDOT(object);

            dotStringBuilder.append(String.format("  \"%s\" -> \"%s\" [label=\"%s\"];\n", subject, object, predicate));
        }

        dotStringBuilder.append("}\n");
        return dotStringBuilder.toString();
    }

    private static String escapeForDOT(String input) {
        // Implement your own escaping logic if needed
        return input.replace("\"", "\\\"");
    }
}
