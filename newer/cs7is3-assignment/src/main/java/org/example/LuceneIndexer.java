package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.miscellaneous.LengthFilterFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.example.Graph.startExpansion;

public class LuceneIndexer {

    /* Indexed, tokenized, stored. */
    public static final FieldType TYPE_STORED = new FieldType(TextField.TYPE_STORED);

    static {

        TYPE_STORED.setStoreTermVectors(true); // Enable term vectors
        TYPE_STORED.setStoreTermVectorPositions(true); // Enable term vector positions
        TYPE_STORED.setTokenized(true);
        TYPE_STORED.setStored(true);
        TYPE_STORED.setStoreTermVectors(true);
        TYPE_STORED.setStoreTermVectorPositions(true);
        TYPE_STORED.freeze();
    }

   public static Analyzer analyzer;

    static {
        try {
            analyzer = CustomAnalyzer.builder(Paths.get(""))
                    .withTokenizer(StandardTokenizerFactory.class) // Use StandardTokenizerFactory
                    .addTokenFilter(LowerCaseFilterFactory.class) // Convert tokens to lowercase
                    .addTokenFilter(StopFilterFactory.class)
                    .addTokenFilter(LengthFilterFactory.class, "min", "2", "max", "20")
                    .addTokenFilter(PorterStemFilterFactory.class)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static final String CONTENT = "content";

    public LuceneIndexer() throws IOException {
    }

    static void createIndexDocuments(IndexWriter writer, Path file) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {

            BufferedReader buffer = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

            String id = "", title = "", author = "", bib = "", content = "", state = "";
            Boolean first = true;
            String line;

            System.out.println("Indexing documents.");

            // Read in lines from the cranfield collection and create indexes for them
            while ((line = buffer.readLine()) != null){
                switch(line.substring(0,2)){ //
                    case ".I":
                        if(!first){
                            Document d = createDocument(id,title,author,bib,content);
                            writer.addDocument(d);
                        }
                        else{ 
                            first=false; 
                        }
                        title = ""; author = ""; bib = ""; content = "";
                        id = line.substring(3,line.length()); break;
                    case ".T":
                    case ".A":
                    case ".B":
                    case ".W":
                        state = line; break;
                    default:
                        switch(state){
                            case ".T": title += line + " "; break;
                            case ".A": author += line + " "; break;
                            case ".B": bib += line + " "; break;
                            case ".W": content += line + " "; break;
                        }
                }
            }
            //System.out.println(content);
            Document d = createDocument(id,title,author,bib,content);
            writer.addDocument(d);
        }

    }

    public static String processQuery(String queryString)
    {
        String processedQuery = "";
        // Analyze the string
        try (TokenStream tokenStream = analyzer.tokenStream("fieldName", queryString)) {
            // Get the CharTermAttribute from the token stream
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);

           
            tokenStream.reset();

           // System.out.println("Tokens:");
            while (tokenStream.incrementToken()) {
                String token = charTermAttribute.toString();
               // System.out.println(token); // Print the processed token
                processedQuery = processedQuery + " "+token;
            }

            //System.out.println(processedQuery);

            // Call end() and close the token stream
            tokenStream.end();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return processedQuery;
    }

    public static void formatQuery() throws IOException, ParseException, java.text.ParseException {

        String queriesPath = "cs7is3-assignment/cran/cran.qry";
        BufferedReader buffer = Files.newBufferedReader(Paths.get(queriesPath), StandardCharsets.UTF_8);
        QueryParser parser = new QueryParser("content",analyzer);
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("new_index")));
        String queryString = "";
        Integer queryNumber = 1;
        String line;
        Boolean first = true;
        String qno = "";
int count = 1;
         String processedQuery = "";


             while ((line = buffer.readLine()) != null) {
            if (line.substring(0, 2).equals(".I")) {
                // Process the current query before resetting for the next one
                if (!queryString.isEmpty()) {
                    System.out.println("qno = " + qno + " query= " + queryString);
                    processedQuery = startExpansion(queryString);
                                        //processedQuery = processQuery(queryString);
                   // System.out.println(processedQuery);
                  
                     QuerySearcher.searchQuery(processedQuery, qno, count);
                    BM25Searcher.search(processedQuery, count);
                    count++;
                }
                qno = line.split(" ")[1]; // Reset for new query
                queryString = "";          // Reset query string
            } else if (!line.substring(0, 2).equals(".W")) {
                queryString += " " + line;
            }
        }

        // Process the last query after the loop ends
        if (!queryString.isEmpty()) {
            System.out.println("qno = " + qno + " query= " + queryString);
            processedQuery = processQuery(queryString);
            System.out.println(processedQuery);
      
             QuerySearcher.searchQuery(processedQuery, qno, count);
             BM25Searcher.search(processedQuery, count);
        }
   

        reader.close();
    }


    static Document createDocument(String id, String title, String author, String bib, String content){
        Document doc = new Document();
        doc.add(new StringField("id", id, Field.Store.YES));
        doc.add(new StringField("path", id, Field.Store.YES));
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("author", author, Field.Store.YES));
        doc.add(new TextField("bibliography", bib, Field.Store.YES));
        Field field = new Field(CONTENT, content, TYPE_STORED);
        doc.add(field);
   
        return doc;
    }
    public static void main(String[] args) {

        //---------------- Set up file paths ----------------

        String indexPath = "new_index";
        String docsPath = "cs7is3-assignment/cran/cran.all.1400";
        HashSet<String> stopWords = new HashSet<>(Arrays.asList("the", "is", "in", "at", "of", "on", "and", "a", "to"));
        final Path docDir = Paths.get(docsPath);
        CharArraySet stopSet = new CharArraySet(stopWords, true);
        if (!Files.isReadable(docDir)) {
            System.out.println("Document directory '" +docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        try {

            Directory dir = FSDirectory.open(Paths.get(indexPath));

            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(dir, iwc);

            // Index the files
            createIndexDocuments(writer, docDir);
            writer.commit();
            writer.close();
            System.out.println("created index successfully");

            formatQuery();


        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
   } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }

    }
}
