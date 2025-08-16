package com.github.activityconnector.model;

import java.io.Serializable;

public class GitHubUser implements Serializable {
    private String name;

    public GitHubUser() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
