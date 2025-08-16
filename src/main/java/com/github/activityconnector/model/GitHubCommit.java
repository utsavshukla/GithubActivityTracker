package com.github.activityconnector.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class GitHubCommit implements Serializable {
    private String message;
    private String author;
    private LocalDateTime timestamp;

    public GitHubCommit() {}

    public GitHubCommit(String message, String author, LocalDateTime timestamp) {
        this.message = message;
        this.author = author;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "GitHubCommit{" +
                "message='" + message + '\'' +
                ", author='" + author + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
