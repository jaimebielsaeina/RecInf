package demo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

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
        Analyzer analyzer = new SpanishAnalyzer2();

        System.out.println("Processing queries in file " + queryFile + ".");

        try {
            BufferedReader queriesReader = new BufferedReader(new FileReader(queryFile, StandardCharsets.UTF_8));
            BufferedWriter resultsWriter = new BufferedWriter(new FileWriter(outFile));
            String queryStr;
            QueryParser parser;
            Query query;
            int i = 0;

            // For each query
            while ((queryStr = queriesReader.readLine()) != null) {
                if (!queryStr.isEmpty()) {
                    // Clean query
                    queryStr = queryStr.trim();
                    if (!queryStr.isEmpty()) {
                        // Parse query
                        parser = new QueryParser(queryStr, analyzer);
                        query = parser.parse(queryStr);

                        // Execute query and show results
                        showResults(searcher, query, ++i, resultsWriter);
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
