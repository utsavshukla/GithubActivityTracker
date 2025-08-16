package com.github.activityconnector.constants;

public final class RedisConstants {
    
    // Redis key patterns
    public static final String REPOS_KEY_PREFIX = "repos:";
    public static final String COMMITS_KEY_PREFIX = "commits:";
    public static final String PAT_KEY_PREFIX = "PAT:";
    public static final int PAGE_SIZE = 20;
    
    // Private constructor to prevent instantiation
    private RedisConstants() {}
}
