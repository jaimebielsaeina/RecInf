package demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


import org.apache.lucene.analysis.Analyzer;
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

        String index = "index";
        String field = "contents";
        boolean additionalInfo = false;

        for (int i = 0; i < args.length; i++) {
            if ("-index".equals(args[i])) {
                index = args[++i];
            } else if ("-field".equals(args[i])) {
                field = args[++i];
            } else if ("-info".equals(args[i])) {
                additionalInfo = true;
            }
        }

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new StandardAnalyzer();

        System.out.println("Enter query: ");

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        QueryParser parser = new QueryParser(field, analyzer);

        String queryString = in.readLine();

        if (queryString != null && !queryString.isEmpty()) {
            queryString = queryString.trim();
            if (!queryString.isEmpty()) {
                Query query = parser.parse(queryString);
                showResults(searcher, query, additionalInfo);
            }
        }
    }

    /**
     * Typical procedure to show the results of a query. For performance issues, the list of results is usually limited to a maximum around 1,000 records.
     * More details at https://lucene.apache.org/core/9_7_0/core/org/apache/lucene/search/IndexSearcher.html
     */
    public static void showResults(IndexSearcher searcher, Query query, boolean additionalInfo) throws IOException {
        System.out.println("Searching for: " + query.toString());

        TopDocs results = searcher.search(query, INITIAL_HITS);
        int numTotalHits = Math.toIntExact(results.totalHits.value);
        System.out.println(numTotalHits + " total matching documents");

        if (numTotalHits>0) {
            ScoreDoc[] hits = searcher.search(query, numTotalHits).scoreDocs;
            StoredFields storedFields = searcher.storedFields();
            for (int i = 0; i < hits.length; i++) {
                Document doc = storedFields.document(hits[i].doc);
                String path = doc.get("path");
                if (path != null) {
                    System.out.println((i + 1) + ". " + path);
                } else {
                    System.out.println((i + 1) + ". No path for this document");
                }
                if (additionalInfo) {
                    System.out.println("\tdoc=" + hits[i].doc + " score=" + hits[i].score);

                    long ms = Long.parseLong(doc.get("modified"));

                    // Convert milliseconds to CEST timezone
                    Instant instant = Instant.ofEpochMilli(ms);
                    ZoneId cestZone = ZoneId.of("Europe/Paris");
                    ZonedDateTime cestTime = instant.atZone(cestZone);

                    // Define a custom date-time formatter
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

                    // Format the ZonedDateTime and print
                    String formattedDate = cestTime.format(formatter);
                    System.out.println(formattedDate);
                }
            }
        }
    }
}
