package demo;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;


/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class IndexFiles {

    private IndexFiles() {}

    /** Index all text files under a directory. */
    public static void main(String[] args) {
        String usage = "java org.apache.lucene.demo.IndexFiles"
                + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
                + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                + "in INDEX_PATH that can be searched with SearchFiles";
        String indexPath = "index";
        String docsPath = null;
        boolean create = true;
        for(int i=0;i<args.length;i++) {
            if ("-index".equals(args[i])) {
                indexPath = args[i+1];
                i++;
            } else if ("-docs".equals(args[i])) {
                docsPath = args[i+1];
                i++;
            } else if ("-update".equals(args[i])) {
                create = false;
            }
        }

        if (docsPath == null) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        final File docDir = new File(docsPath);
        if (!docDir.exists() || !docDir.canRead()) {
            System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        Date start = new Date();
        try {
            System.out.println("Indexing to directory '" + indexPath + "'...");

            Directory dir = FSDirectory.open(Paths.get(indexPath));
            Analyzer analyzer = new SpanishAnalyzer2();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

            if (create) {
                // Create a new index in the directory, removing any
                // previously indexed documents:
                iwc.setOpenMode(OpenMode.CREATE);
            } else {
                // Add new documents to an existing index:
                iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            }

            // Optional: for better indexing performance, if you
            // are indexing many documents, increase the RAM
            // buffer.  But if you do this, increase the max heap
            // size to the JVM (eg add -Xmx512m or -Xmx1g):
            //
            // iwc.setRAMBufferSizeMB(256.0);

            IndexWriter writer = new IndexWriter(dir, iwc);
            indexDocs(writer, docDir);

            // NOTE: if you want to maximize search performance,
            // you can optionally call forceMerge here.  This can be
            // a terribly costly operation, so generally it's only
            // worth it when your index is relatively static (ie
            // you're done adding documents to it):
            //
            // writer.forceMerge(1);

            writer.close();

            Date end = new Date();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds");

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }

    /**
     * Adds a field to a Lucene document, using elements as content and fieldName as field name.
     * Applies a fixed field type to depending on the field name.
     *
     * @param fieldName Name of the field(s) to add.
     * @param elements Value(s) of the field(s) to add.
     * @param doc Document in which the field(s) will be added.
     */
    static void addData(String fieldName, NodeList elements, Document doc)
    {
        boolean isTextField = !(fieldName.equals("identifier") ||
                fieldName.equals("type") ||
                fieldName.equals("format") ||
                fieldName.equals("language") ||
                fieldName.equals("relation") ||
                fieldName.equals("rights") ||
                fieldName.equals("date") ||
                fieldName.equals("created") ||
                fieldName.equals("issued"));

        for  (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            String value = element.getTextContent();

            if (fieldName.equals("created") || fieldName.equals("issued")) {
                value = value.replace("-", "");
            }

            // Saving the field with the corresponding name. Identifier must be remembered.
            if (isTextField)
                doc.add(new TextField(fieldName, transformString(value), Field.Store.NO));
            else if (fieldName.equals("identifier"))
                doc.add(new StringField(fieldName, transformString(value), Field.Store.YES));
            else
                doc.add(new StringField(fieldName, transformString(value), Field.Store.NO));
        }
    }

    /**
     * Indexes the given file using the given writer, or if a directory is given,
     * recurses over files and directories found under the given directory.
     *
     * @param writer Writer to the index where the given file/dir info will be stored
     * @param file The file to index, or the directory to recurse into to find files to index
     * @throws IOException If there is a low-level I/O error
     */
    static void indexDocs(IndexWriter writer, File file)
            throws IOException {

        String[] fieldNames = {
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

        // Do not try to index files that cannot be read.
        if (file.canRead()) {
            if (file.isDirectory()) {
                String[] files = file.list();
                // An IO error could occur.
                if (files != null) {
                    List<String> fileList = new LinkedList<String>(Arrays.asList(files));
                    Collections.sort(fileList);
                    for (String fileName: fileList) {
                        indexDocs(writer, new File(file, fileName));
                    }
                }
            } else {

                try {
                    // Create a DocumentBuilderFactory.
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

                    // Create a DocumentBuilder.
                    DocumentBuilder builder = factory.newDocumentBuilder();

                    // Parse the XML file.
                    org.w3c.dom.Document document = builder.parse(file);

                    // make a new, empty document.
                    Document doc = new Document();
                    Field pathField = new StringField("path", file.getPath(), Field.Store.YES);
                    doc.add(pathField);
                    doc.add(new StoredField("modified", file.lastModified()));

                    NodeList elements;
                    for (String fieldName : fieldNames) {
                        // Traverse and manipulate the XML document.
                        elements = document.getElementsByTagName(((fieldName.equals("issued")||fieldName.equals("created"))?"dcterms:":"dc:") + fieldName);

                        addData(fieldName, elements, doc);
                    }

                    // Saving document coordinates.
                    String value;
                    String[] fields;
                    NodeList boundingBoxes = document.getElementsByTagName("ows:BoundingBox");
                    // If one Bounding is found, it will be saved too into the index.
                    if (boundingBoxes.getLength() == 1) {
                        elements = ((Element)boundingBoxes.item(0)).getElementsByTagName("ows:LowerCorner");
                        if (elements.getLength() == 1) {
                            value = elements.item(0).getTextContent();
                            fields = value.split(" ");
                            doc.add(new DoublePoint("west", Double.parseDouble(fields[0])));
                            doc.add(new DoublePoint("south", Double.parseDouble(fields[1])));
                        }
                        elements = ((Element)boundingBoxes.item(0)).getElementsByTagName("ows:UpperCorner");
                        if (elements.getLength() == 1) {
                            value = elements.item(0).getTextContent();
                            fields = value.split(" ");
                            doc.add(new DoublePoint("east", Double.parseDouble(fields[0])));
                            doc.add(new DoublePoint("north", Double.parseDouble(fields[1])));
                        }
                    }

                    if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
                        // New index, so we just add the document (no old document can be there):
                        System.out.println("adding " + file);
                        writer.addDocument(doc);
                    } else {
                        // Existing index (an old copy of this document may have been indexed) so
                        // we use updateDocument instead to replace the old one matching the exact
                        // path, if present:
                        System.out.println("updating " + file);
                        writer.updateDocument(new Term("path", file.getPath()), doc);
                    }

                } catch (FileNotFoundException fnfe) {
                    // at least on windows, some temporary files raise this exception with an "access denied" message
                    // checking if the file can be read doesn't help
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
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
}