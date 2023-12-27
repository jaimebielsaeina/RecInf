package IR.Practica5;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.query.text.TextIndexConfig;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.vocabulary.DC;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class SemanticSearcher {
    public static final int INITIAL_HITS = 100;

    /**
     * Simple command-line based search demo.
     */
    public static void main(String[] args) throws Exception {
        String usage =
                "Usage:\tSemanticSearcher [-rdf ref_path] [-infoNeeds info_needs_path] [-output output_file]\n";
        if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
            System.out.println(usage);
            System.exit(0);
        }

        // Getting the parameters.
        String rdfPath = "";
        String queryFile = "";
        String outFile = "";
        for (int i = 0; i < args.length; i++) {
            if ("-rdf".equals(args[i])) {
                rdfPath = args[++i];
            } else if ("-infoNeeds".equals(args[i])) {
                queryFile = args[++i];
            } else if ("-output".equals(args[i])) {
                outFile = args[++i];
            }
        }

        // Exiting if one of the parameters was not provided.
        if (rdfPath.isEmpty() || queryFile.isEmpty() || outFile.isEmpty()) {
            System.out.println("Please provide index and query files as well as the results file properly.");
            System.exit(1);
        }

        System.out.println("Processing queries in file " + queryFile + ".");

        // Create processing classes.
        Query query;
        String queryStr;

        try {

            // Defining the indexed repository settings.
            EntityDefinition entDef = new EntityDefinition("uri", "identifier", ResourceFactory.createProperty("http://purl.org/dc/elements/1.1/","identifier"));
            entDef.set("contributor", DC.contributor.asNode());
            entDef.set("creator", DC.creator.asNode());
            entDef.set("description", DC.description.asNode());
            entDef.set("publisher", DC.publisher.asNode());
            entDef.set("subject", DC.subject.asNode());
            entDef.set("title", DC.title.asNode());
            TextIndexConfig config = new TextIndexConfig(entDef);
            config.setAnalyzer(new SpanishAnalyzer());
            config.setQueryAnalyzer(new SpanishAnalyzer());
            config.setMultilingualSupport(true);

            // Defining the indexed repository.
            FileUtils.deleteDirectory(new File("rdfPath"));
            Dataset ds1 = TDB2Factory.connectDataset(rdfPath + "/tdb2");
            Directory dir =  new MMapDirectory(Paths.get("./" + rdfPath + "/lucene"));
            Dataset ds = TextDatasetFactory.createLucene(ds1, dir, config) ;

            // Loading the file and sroting it on the indexed repository.
            ds.begin(ReadWrite.WRITE) ;
            RDFDataMgr.read(ds.getDefaultModel(), "bbcColeccion.ttl") ;
            ds.commit();
            ds.end();

            // Parsing XML file.
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new InputSource(new InputStreamReader(new FileInputStream(queryFile), StandardCharsets.UTF_8)));
            NodeList infoNeeds = document.getElementsByTagName("informationNeed");

            // Creating file where results will be written.
            BufferedWriter resultsWriter = new BufferedWriter(new FileWriter(outFile));

            // For each information need:
            for (int i = 0; i < infoNeeds.getLength(); i++) {

                // Getting the identifier and the text of the info. need.
                Element infoNeed = (Element) infoNeeds.item(i);

                NodeList infoNeedIdentifier = infoNeed.getElementsByTagName("identifier");
                if (infoNeedIdentifier.getLength() != 1) continue;
                String identifier = infoNeedIdentifier.item(0).getTextContent();
                System.out.println(identifier);

                NodeList infoNeedText = infoNeed.getElementsByTagName("text");
                if (infoNeedText.getLength() != 1) continue;
                queryStr = infoNeedText.item(0).getTextContent();

                // Doing the SPAQRL query on the text field.
                query = QueryFactory.create(queryStr) ;
                ds.begin(ReadWrite.READ) ;
                try (QueryExecution qexec = QueryExecutionFactory.create(query, ds)) {
                    ResultSet results = qexec.execSelect() ;
                    while (results.hasNext()) {
                        // Print the retrieved documents.
                        QuerySolution sol = results.nextSolution() ;
                        resultsWriter.write(identifier + "\t" + sol.get("document")/* + "\t" + sol.get("score")*/ + "\n");
                    }
                }
                ds.end();

            }
            resultsWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
