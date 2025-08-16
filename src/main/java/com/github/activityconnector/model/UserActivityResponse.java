package com.github.activityconnector.model;

import java.io.Serializable;
import java.util.List;

public class UserActivityResponse implements Serializable {
    private String username;
    private List<GitHubRepository> repositories;

    public UserActivityResponse() {}

    public UserActivityResponse(String username, List<GitHubRepository> repositories) {
        this.username = username;
        this.repositories = repositories;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public List<GitHubRepository> getRepositories() { return repositories; }
    public void setRepositories(List<GitHubRepository> repositories) { 
        this.repositories = repositories;
    }

    @Override
    public String toString() {
        return "UserActivityResponse{" +
                "username='" + username + '\'' +
                ", repositories=" + (repositories != null ? repositories.size() + " repos" : "null") +
                '}';
    }
}
