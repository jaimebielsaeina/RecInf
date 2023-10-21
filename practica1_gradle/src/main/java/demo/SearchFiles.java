package demo;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.model.BaseModel;
import opennlp.tools.util.model.UncloseableInputStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;

/**
 * Simple command-line based search demo.
 */
public class SearchFiles {

    public static final int INITIAL_HITS = 100;

    public SearchFiles() {
    }

    /**
     * Simple command-line based search demo.
     */
    public static void main(String[] args) throws Exception {
        String usage =
                "Usage:\tjava org.apache.lucene.demo.SearchFiles [-index dir] [-field f] [-info]";
        if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
            System.out.println(usage);
            System.exit(0);
        }

        // Cargar el modelo POS en español
        InputStream modelIn = new FileInputStream("es-pos-maxent.model");
        POSModel posModel = new POSModel(modelIn);

        // Inicializar el etiquetador POS
        POSTaggerME posTagger = new POSTaggerME(posModel);

        // Oración de ejemplo
        String sentence = "Estoy interesado en mecanismos de comunicación entre procesos en " +
                "entornos distribuidos. Preferiría ver descripciones de mecanismos completos, " +
                "con o sin implementaciones, pero no trabajos teóricos sobre un problema " +
                "abstracto. Remote procedure calls y message-passing son ejemplos de mis " +
                "intereses.";

        // Tokeniza la oración
        String[] tokens = sentence.split(" ");

        // Realiza el etiquetado gramatical
        String[] tags = posTagger.tag(tokens);

        List<String> sustantivos = new ArrayList<>();
        List<String> adjetivos = new ArrayList<>();

        for (int i = 0; i < tokens.length; i++) {
            if (tags[i].startsWith("N")) {
                sustantivos.add(tokens[i]);

                if (i < tokens.length - 1 && tags[i + 1].startsWith("A"))
                    adjetivos.add(tokens[i + 1]);
            }
        }

        // Cerrar el flujo del modelo
        modelIn.close();


        String index = "";
        String queryFile = "";
        String outFile = "";

        for (int i = 0; i < args.length; i++) {
            if ("-index".equals(args[i])) {
                index = args[++i];
            } else if ("-infoNeeds".equals(args[i])) {
                queryFile = args[++i];
            } else if ("-output".equals(args[i])) {
                outFile = args[++i];
            }
        }

        if (index.isEmpty() || queryFile.isEmpty() || outFile.isEmpty()) {
            System.out.println("Please provide index and query files as well as the results file properly.");
            System.exit(1);
        }

        // Create processing classes
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new StandardAnalyzer();

        System.out.println("Processing queries in file " + queryFile + ".");

        try {
            BufferedReader queriesReader = new BufferedReader(new FileReader(queryFile, StandardCharsets.UTF_8));
            BufferedWriter resultsWriter = new BufferedWriter(new FileWriter(outFile));
            String queryStr;
            QueryParser parser;
            int i = 0;
            BooleanQuery.Builder queries;

            // For each query
            while ((queryStr = queriesReader.readLine()) != null) {
                queries = new BooleanQuery.Builder();
                if (!queryStr.isEmpty()) {
                    // Clean query
                    queryStr = queryStr.trim();
                    if (!queryStr.isEmpty()) {

                        // Create a Pattern object for the spatial regular expression
                        Pattern regex1 = Pattern.compile("spatial:-?\\d+.\\d+,-?\\d+.\\d+,-?\\d+.\\d+,-?\\d+.\\d+");
                        Pattern regex2 = Pattern.compile("created:\\[(\\d+|\\*) TO (\\d+|\\*)]");
                        Pattern regex3 = Pattern.compile("issued:\\[(\\d+|\\*) TO (\\d+|\\*)]");
                        Pattern regex4 = Pattern.compile("created:\\d+");
                        Pattern regex5 = Pattern.compile("issued:\\d+");

                        // Create a Matcher object to find matches in the query string
                        Matcher matcher1 = regex1.matcher(queryStr);
                        Matcher matcher2 = regex2.matcher(queryStr);
                        Matcher matcher3 = regex3.matcher(queryStr);
                        Matcher matcher4 = regex4.matcher(queryStr);
                        Matcher matcher5 = regex5.matcher(queryStr);

                        String spatialQueryStr = "";
                        String createdQueryStr = "";
                        String issuedQueryStr = "";
                        String createdNRQueryStr = "";
                        String issuedNRQueryStr = "";
                        String otherQueryStr = queryStr; // Initialize with the original query

                        // Check if the spatial part is found and extract it
                        if (matcher1.find()) {
                            spatialQueryStr = matcher1.group(0);
                            // Remove the spatial part from the rest
                            otherQueryStr = otherQueryStr.replaceFirst("spatial:-?\\d+.\\d+,-?\\d+.\\d+,-?\\d+.\\d+,-?\\d+.\\d+", "").trim();
                        }
                        if (matcher2.find()) {
                            createdQueryStr = matcher2.group(0);
                            // Remove the spatial part from the rest
                            otherQueryStr = otherQueryStr.replaceFirst("created:\\[(\\d+|\\*) TO (\\d+|\\*)]", "").trim();
                        }
                        if (matcher3.find()) {
                            issuedQueryStr = matcher3.group(0);
                            // Remove the spatial part from the rest
                            otherQueryStr = otherQueryStr.replaceFirst("issued:\\[(\\d+|\\*) TO (\\d+|\\*)]", "").trim();
                        }
                        if (matcher4.find()) {
                            createdNRQueryStr = matcher4.group(0);
                            // Remove the spatial part from the rest
                            otherQueryStr = otherQueryStr.replaceFirst("created:\\d+", "").trim();
                        }
                        if (matcher5.find()) {
                            issuedNRQueryStr = matcher5.group(0);
                            // Remove the spatial part from the rest
                            otherQueryStr = otherQueryStr.replaceFirst("issued:\\d+", "").trim();
                        }

                        if (!spatialQueryStr.isEmpty()) {
                            String[] coordinates = queryStr.substring(8).split(" ")[0].split(",");
                            Query westRangeQuery = DoublePoint.newRangeQuery("west", Double.NEGATIVE_INFINITY, Double.parseDouble(coordinates[1]));
                            Query southRangeQuery = DoublePoint.newRangeQuery("south", Double.NEGATIVE_INFINITY, Double.parseDouble(coordinates[3]));
                            Query eastRangeQuery = DoublePoint.newRangeQuery("east", Double.parseDouble(coordinates[0]), Double.POSITIVE_INFINITY);
                            Query northRangeQuery = DoublePoint.newRangeQuery("north", Double.parseDouble(coordinates[2]), Double.POSITIVE_INFINITY);

                            BooleanQuery spatialQuery = new BooleanQuery.Builder().add(westRangeQuery, BooleanClause.Occur.MUST)
                                                                                  .add(southRangeQuery, BooleanClause.Occur.MUST)
                                                                                  .add(eastRangeQuery, BooleanClause.Occur.MUST)
                                                                                  .add(northRangeQuery, BooleanClause.Occur.MUST).build();
                            queries.add(spatialQuery, BooleanClause.Occur.SHOULD);
                        }

                        Pattern pattern = Pattern.compile("\\[(\\d+|\\*) TO (\\d+|\\*)]");
                        Pattern patternNR = Pattern.compile("(\\d+)");
                        String date1, date2;
                        Matcher numberMatcher1 = pattern.matcher(createdQueryStr);
                        if (numberMatcher1.find()) {
                            date1 = numberMatcher1.group(1);
                            date2 = numberMatcher1.group(2);
                            queries.add(TermRangeQuery.newStringRange("created", date1.equals("*")?null:date1, date2.equals("*")?null:date2, true, true), BooleanClause.Occur.SHOULD);
                        }
                        Matcher numberMatcher2 = pattern.matcher(issuedQueryStr);
                        if (numberMatcher2.find()) {
                            date1 = numberMatcher2.group(1);
                            date2 = numberMatcher2.group(2);
                            queries.add(TermRangeQuery.newStringRange("issued", date1.equals("*")?null:date1, date2.equals("*")?null:date2, true, true), BooleanClause.Occur.SHOULD);
                        }
                        Matcher numberMatcher3 = patternNR.matcher(createdNRQueryStr);
                        if (numberMatcher3.find()) {
                            date1 = numberMatcher3.group(1);
                            queries.add(new TermQuery(new Term("created", date1)), BooleanClause.Occur.SHOULD);
                        }
                        Matcher numberMatcher4 = patternNR.matcher(issuedNRQueryStr);
                        if (numberMatcher4.find()) {
                            date1 = numberMatcher4.group(1);
                            queries.add(new TermQuery(new Term("issued", date1)), BooleanClause.Occur.SHOULD);
                        }

                        if (!otherQueryStr.isEmpty()) {
                            // Parse query
                            parser = new QueryParser(queryStr, analyzer);
                            queries.add(parser.parse(otherQueryStr), BooleanClause.Occur.SHOULD);
                        }

                        showResults(searcher, queries.build(), ++i, resultsWriter);

                    }
                }
            }
            queriesReader.close();
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
    public static void showResults(IndexSearcher searcher, Query query, int order, BufferedWriter outFile) throws IOException {
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
                    outFile.write(order + "\t" + doc.get("identifier") + "\n");
                }
            }
        }
    }
}
