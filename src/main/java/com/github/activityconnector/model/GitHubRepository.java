package com.github.activityconnector.model;

import java.io.Serializable;
import java.util.List;

public class GitHubRepository implements Serializable {
    private String name;
    private String description;
    private List<GitHubCommit> recentCommits;

    public GitHubRepository() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<GitHubCommit> getRecentCommits() { return recentCommits; }
    public void setRecentCommits(List<GitHubCommit> recentCommits) { this.recentCommits = recentCommits; }

    @Override
    public String toString() {
        return "GitHubRepository{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", recentCommits=" + (recentCommits != null ? recentCommits.size() + " commits" : "null") +
                '}';
    }
}
