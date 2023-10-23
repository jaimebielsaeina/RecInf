package demo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;


import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Simple command-line based search demo.
 */
public class SearchFiles {

    public static final int INITIAL_HITS = 100;

    public SearchFiles() {
    }

    /**
     * Transforms a string, converting Spanish special symbols into non-special ones.
     * @param in String to transformed.
     * @return Transformed string.
     */
    private static String transformString (String in) {
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

        // Getting the parameters.
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

        // Exiting if one of the parameters was not provided.
        if (index.isEmpty() || queryFile.isEmpty() || outFile.isEmpty()) {
            System.out.println("Please provide index and query files as well as the results file properly.");
            System.exit(1);
        }

        System.out.println("Processing queries in file " + queryFile + ".");

        // Create processing classes.
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new SpanishAnalyzer2();
        BooleanQuery.Builder queries;
        String queryStr;

        try {

            // Loading Spanish POS model.
            InputStream modelIn = new FileInputStream("es-pos-maxent.model");
            POSModel posModel = new POSModel(modelIn);

            // Initializing POS tagging.
            POSTaggerME posTagger = new POSTaggerME(posModel);

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
                String text = infoNeedText.item(0).getTextContent();

                queryStr = text;
                queries = new BooleanQuery.Builder();

                // Tokenize the phrase.
                String[] tokens = text.split(" ");
                for (int k=0; k<tokens.length; k++)
                    tokens[k] = transformString(tokens[k]);

                // Initialize two StringBuilder objects to store the tokens in separate fields.
                StringBuilder noField = new StringBuilder();
                StringBuilder restField = new StringBuilder();

                boolean noFound = false;
                String token;

                for (int j=0; j<tokens.length; j++) {
                    token = tokens[j];
                    // If there's a "no" word, do a signal.
                    if (token.equalsIgnoreCase("no")) {
                        noFound = true;
                        noField.append(token).append(" ");
                    // If the phrase ends, any negation will end.
                    } else if (token.endsWith(",") || token.endsWith(".")) {
                        if (noFound) {
                            noField.append(token);
                        } else {
                            restField.append(token).append(" ");
                        }
                        noFound = false;
                    // If there's a period in the phrase, do a date restriction over the query.
                    } else if (token.equalsIgnoreCase("entre") && tokens.length > j+3) {
                        String term1 = tokens[j+1].replaceAll("\\p{Punct}", "");
                        String term2 = tokens[j+2].replaceAll("\\p{Punct}", "");
                        String term3 = tokens[j+3].replaceAll("\\p{Punct}", "");
                        if (term1.matches("[0-9]+") &&
                            term2.equalsIgnoreCase("y") &&
                            term3.matches("[0-9]+")) {
                            queries.add(TermRangeQuery.newStringRange("date", term1, term3, true, true), BooleanClause.Occur.MUST);
                            j += 3;
                        }
                    } else {
                        // If there was a "no" before, mark the word not to appear on the results.
                        if (noFound) {
                            noField.append(token).append(" ");
                        // Else, mark the word to appear on the results.
                        } else {
                            restField.append(token).append(" ");
                        }
                    }
                }

                // Convert the StringBuilder objects to strings.
                String noString = noField.toString().trim();
                String restString = restField.toString().trim();

                String[] restTokens = restString.split(" ");
                String[] noTokens = noString.split(" ");

                // Grammatical tagging.
                String[] noTags = posTagger.tag(noTokens);
                String[] restTags = posTagger.tag(restTokens);

                // Makes lists containing names that should/mustn't be included.
                List<String> sustantivos = new ArrayList<>();
                List<String> adjetivos = new ArrayList<>();
                List<String> noSustantivos = new ArrayList<>();
                List<String> noAdjetivos = new ArrayList<>();
                for (int j = 0; j < restTokens.length; j++) {
                    if (restTags[j].startsWith("N")) {
                        sustantivos.add(restTokens[j]);
                        if (j < restTokens.length - 1 && restTags[j + 1].startsWith("A"))
                            adjetivos.add(restTokens[j + 1]);
                    }
                }
                for (int j = 0; j < noTokens.length; j++) {
                    if (noTags[j].startsWith("N")) {
                        noSustantivos.add(noTokens[j]);

                        if (j < noTokens.length - 1 && noTags[j + 1].startsWith("A"))
                            noAdjetivos.add(noTokens[j + 1]);
                    }
                }

                if (!queryStr.isEmpty()) {
                    String[] data = {
                            "title",
                            "identifier",
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

                    // Building strings for que queries.
                    String shouldContain = "";
                    String mustntContain = "";
                    for (String s : sustantivos) shouldContain = shouldContain.concat(" " + s);
                    for (String a : adjetivos) shouldContain = shouldContain.concat(" " + a);
                    for (String s : noSustantivos) mustntContain = mustntContain.concat(" " + s);
                    for (String a : noAdjetivos) mustntContain = mustntContain.concat(" " + a);
                    // Query with the words that documents should contain.
                    queries.add(new MultiFieldQueryParser(data, analyzer).parse(shouldContain.trim()), BooleanClause.Occur.SHOULD);
                    // Query with the words that documents mustn't contain.
                    if (!mustntContain.isEmpty())
                        queries.add(new MultiFieldQueryParser(data, analyzer).parse(mustntContain.trim()), BooleanClause.Occur.MUST_NOT);

                    // Show results as usual.
                    showResults(searcher, queries.build(), identifier, resultsWriter);

                }
            }
            //queriesReader.close();
            resultsWriter.close();

            // Cerrar el flujo del modelo
            modelIn.close();

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
    public static void showResults(IndexSearcher searcher, Query query, String searchIdentifier, BufferedWriter outFile) throws IOException {
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
    }
}
