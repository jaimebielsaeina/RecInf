package IR.Practica5;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.vocabulary.DC;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class SemanticSearcher {
    public static final int INITIAL_HITS = 100;

    /**
     * Transforms a string, converting Spanish special symbols into non-special ones.
     * @param in String to transformed.
     * @return Transformed string.
     */
    /*private static String transformString (String in) {
        // For each character:
        for (int k=0; k<in.length(); k++) {
            // If it's special, it's changed for a non-special one.
            switch (in.charAt(k)) {
                case 193:
                    in = in.replaceAll(in.substring(k, k+1), "A"); break;
                case 201:
                    in = in.replaceAll(in.substring(k, k+1), "E"); break;
                case 205:
                    in = in.replaceAll(in.substring(k, k+1), "I"); break;
                case 211:
                    in = in.replaceAll(in.substring(k, k+1), "O"); break;
                case 218:
                case 220:
                    in = in.replaceAll(in.substring(k, k+1), "U"); break;
                case 209:
                    in = in.replaceAll(in.substring(k, k+1), "N"); break;
                case 225:
                    in = in.replaceAll(in.substring(k, k+1), "a"); break;
                case 233:
                    in = in.replaceAll(in.substring(k, k+1), "e"); break;
                case 237:
                    in = in.replaceAll(in.substring(k, k+1), "i"); break;
                case 243:
                    in = in.replaceAll(in.substring(k, k+1), "o"); break;
                case 250:
                case 252:
                    in = in.replaceAll(in.substring(k, k+1), "u"); break;
                case 241:
                    in = in.replaceAll(in.substring(k, k+1), "n"); break;
                case 161:
                case 191:
                    in = in.replaceAll(in.substring(k, k+1), ""); break;
            }
        }
        return in;
    }*/

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

            //definimos la configuración del repositorio indexado
            EntityDefinition entDef = new EntityDefinition("uri", "identifier", ResourceFactory.createProperty("http://purl.org/dc/elements/1.1/","identifier"));
            entDef.set("description", DC.description.asNode());
            entDef.set("subject", DC.subject.asNode());
            TextIndexConfig config = new TextIndexConfig(entDef);
            config.setAnalyzer(new SpanishAnalyzer());
            config.setQueryAnalyzer(new SpanishAnalyzer());
            config.setMultilingualSupport(true);

            //definimos el repositorio indexado todo en disco
            //se borra el repositorio para forzar a que cada vez que lo ejecutamos se cree de cero
            FileUtils.deleteDirectory(new File("rdfPath"));
            Dataset ds1 = TDB2Factory.connectDataset(rdfPath + "/tdb2");
            Directory dir =  new MMapDirectory(Paths.get("./" + rdfPath + "/lucene"));
            Dataset ds = TextDatasetFactory.createLucene(ds1, dir, config) ;

            // cargamos el fichero deseado y lo almacenamos en el repositorio indexado
            ds.begin(ReadWrite.WRITE) ;
            RDFDataMgr.read(ds.getDefaultModel(), "bbcColeccion.ttl") ;
            ds.commit();
            ds.end();


/*
            // Retrieving the RDF graph.
		    Dataset data = TDB2Factory.connectDataset(rdfPath);
            data.begin(ReadWrite.READ) ;
		    Model model = data.getDefaultModel();
*/

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

                query = QueryFactory.create(queryStr) ;
                ds.begin(ReadWrite.READ) ;
                try (QueryExecution qexec = QueryExecutionFactory.create(query, ds)) {
                    ResultSet results = qexec.execSelect() ;
                    while (results.hasNext()) {
                        QuerySolution sol = results.nextSolution() ;
                        resultsWriter.write(sol + "\n");
                    }
                }
                ds.end();

                        //QuerySolution sol = results.nextSolution() ;
                        //RDFNode x = sol.get("document") ;
                        //if (x.isLiteral()) resultsWriter.write(identifier + "\t" + x.toString() + "\n");
                        //else resultsWriter.write(identifier + "\t" + x.asResource().getURI() + "\n");

                    // Show results as usual.
                    //showResults(searcher, query, identifier, resultsWriter);

            }
            //queriesReader.close();
            resultsWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        /*if (queryString != null && !queryString.isEmpty()) {
            queryString = queryString.trim();
            if (!queryString.isEmpty()) {
                Query query = parser.parse(queryString);
                showResults(searcher, query, additionalInfo);
            }
        }*/
    }

    /**
     * Typical procedure to show the results of a query. For performance issues, the list of results is usually limited to a maximum around 1,000 records.
     * More details at https://lucene.apache.org/core/9_7_0/core/org/apache/lucene/search/IndexSearcher.html
     */
    /*public static void showResults(IndexSearcher searcher, Query query, String searchIdentifier, BufferedWriter outFile) throws IOException {
        // Execute query
        TopDocs results = searcher.search(query, INITIAL_HITS);
        int numTotalHits = Math.toIntExact(results.totalHits.value);
        System.out.println(numTotalHits + " total matching documents");

        // If there were any hits
        if (numTotalHits>0) {
            // Order the hits by score
            ScoreDoc[] hits = searcher.search(query, numTotalHits).scoreDocs;
            StoredFields storedFields = searcher.storedFields();

            // For each hit
            for (ScoreDoc hit : hits) {
                // Get document fields
                Document doc = storedFields.document(hit.doc);
                String path = doc.get("path");

                // Print document identifyer
                if (path != null) {
                    outFile.write(searchIdentifier + "\t" + doc.get("identifier") + "\n");
                }
            }
        }
    }*/
}
