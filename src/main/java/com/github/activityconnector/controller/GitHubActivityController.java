package com.github.activityconnector.controller;

import com.github.activityconnector.exception.AuthenticationException;
import com.github.activityconnector.model.GitHubCommit;
import com.github.activityconnector.model.PaginatedResponse;
import com.github.activityconnector.model.UserActivityResponse;
import com.github.activityconnector.service.RedisDataService;
import com.github.activityconnector.constants.RedisConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class GitHubActivityController {
    private static final Logger logger = LoggerFactory.getLogger(GitHubActivityController.class);

    private final RedisDataService redisDataService;

    public GitHubActivityController(RedisDataService redisDataService) {
        this.redisDataService = redisDataService;
    }

    /**
     * Get user activity (repositories) from Redis only
     * GET /api/v1/activity/{username}?page=0
     * Page size is fixed at 20 items per page
     */
    @GetMapping("/activity/{username}")
    public ResponseEntity<PaginatedResponse<com.github.activityconnector.model.GitHubRepository>> getUserActivity(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        final int PAGE_SIZE = 20;
        logger.info("Received request for user activity: {} (page={}, size={})", username, page, PAGE_SIZE);
        
        String pat = extractPatFromAuthHeader(authHeader);
        if (pat == null) {
            throw new AuthenticationException("Missing or invalid Authorization header");
        }
        
        if (!redisDataService.isValidUserWithPat(username, pat)) {
            throw new AuthenticationException("Invalid Personal Access Token");
        }
        
        redisDataService.checkRateLimit(username);
        
        PaginatedResponse<com.github.activityconnector.model.GitHubRepository> response = 
            redisDataService.getRepositoriesFromRedis(username, page, PAGE_SIZE);
        return ResponseEntity.ok(response);
    }

    /**
     * Get commits for a single repository from Redis only
     * GET /api/v1/commits/{username}/{repo}?page=0
     * Page size is fixed at 20 items per page
     */
    @GetMapping("/commits/{username}/{repo}")
    public ResponseEntity<PaginatedResponse<GitHubCommit>> getRepositoryCommits(
            @PathVariable String username, 
            @PathVariable String repo,
            @RequestParam(defaultValue = "0") int page,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Received request for commits: {}/{} (page={}, size={})", username, repo, page, RedisConstants.PAGE_SIZE);

        String pat = extractPatFromAuthHeader(authHeader);
        if (pat == null) {
            throw new AuthenticationException("Missing or invalid Authorization header");
        }
        
        if (!redisDataService.isValidUserWithPat(username, pat)) {
            throw new AuthenticationException("Invalid Personal Access Token");
        }

        redisDataService.checkRateLimit(username);
        
        PaginatedResponse<GitHubCommit> response = redisDataService.getCommitsFromRedis(username, repo, page, RedisConstants.PAGE_SIZE);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Extract PAT from Authorization header
     * Expected format: "Bearer {PAT}" or "token {PAT}"
     */
    private String extractPatFromAuthHeader(String authHeader) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            return null;
        }
        
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        } else if (authHeader.startsWith("token ")) {
            return authHeader.substring(6).trim();
        }
        
        return null;
    }
}
