package org.example;

public class CranfieldDocument {
    private String id;
    private String title;
    private String author;
    private String body;

    public CranfieldDocument(String id, String title, String author, String body) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.body = body;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }
}