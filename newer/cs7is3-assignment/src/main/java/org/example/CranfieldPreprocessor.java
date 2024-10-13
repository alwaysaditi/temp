package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CranfieldPreprocessor {
    public static List<CranfieldDocument> preprocess(String filePath) {
        List<CranfieldDocument> documents = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            String docId = null, title = null, author = null, body = null;
            StringBuilder bodyBuilder = null;
            StringBuilder titleBuilder = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(".I")) {
                    // If we have read a document, add it to the list
                    if (docId != null) {
                        documents.add(new CranfieldDocument(docId, title, author, bodyBuilder.toString()));
                    }
                    // Start a new document
                    docId = line.substring(3).trim();
                    title = author = body = null;
                    bodyBuilder = new StringBuilder();
                    titleBuilder = new StringBuilder();
                } else if (line.startsWith(".T")) {
                   // title = reader.readLine();  // Title is the next line
                    while ((line = reader.readLine()) != null && !line.startsWith(".")) {
                        //next section always begins with .
                        titleBuilder.append(line).append("\n");
                    }
                    System.out.println("TITLE");
                    System.out.println(titleBuilder.toString());
                } else if (line.startsWith(".A")) {
                    author = reader.readLine();  // Author is the next line
                } else if (line.startsWith(".W")) {
                    // Body starts after .W until the next section begins
                    while ((line = reader.readLine()) != null && !line.startsWith(".")) {
                        //next section always begins with .
                        bodyBuilder.append(line).append("\n");
                    }
                }
            }
            // Add the last document (as the loop may end after the last document)
            if (docId != null) {
                documents.add(new CranfieldDocument(docId, titleBuilder.toString(), author, bodyBuilder.toString()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return documents;
    }
}

