package com.github.activityconnector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.activityconnector.constants.RedisConstants;
import com.github.activityconnector.exception.AuthenticationException;
import com.github.activityconnector.exception.DataNotFoundException;
import com.github.activityconnector.model.GitHubCommit;
import com.github.activityconnector.model.GitHubRepository;
import com.github.activityconnector.model.PaginatedResponse;
import com.github.activityconnector.model.UserActivityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class RedisDataService {
    private static final Logger logger = LoggerFactory.getLogger(RedisDataService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisDataService(RedisTemplate<String, Object> redisTemplate, StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Validate user PAT by comparing hash with stored hash in Redis
     */
    public boolean isValidUserWithPat(String username, String pat) {
        String patKey = RedisConstants.PAT_KEY_PREFIX + username;
        try {
            String storedHashedPat = stringRedisTemplate.opsForValue().get(patKey);
            if (storedHashedPat == null) {
                logger.debug("PAT validation for user {}: invalid (not found)", username);
                return false;
            }
            
            String hashedPat = hashPat(pat);
            boolean isValid = storedHashedPat.equals(hashedPat);
            logger.debug("PAT validation for user {}: {}", username, isValid ? "valid" : "invalid");
            return isValid;
        } catch (Exception e) {
            logger.warn("Error validating PAT for user {}: {}", username, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check rate limit using Redis INCR and EXPIRE commands
     * @param username the username to check
     * @param maxRequests maximum requests allowed per minute
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    public void checkRateLimit(String username, int maxRequests) {
        String rateLimitKey = "rate_limit:" + username;
        
        try {
            Long currentCount = stringRedisTemplate.opsForValue().increment(rateLimitKey);
            
            if (currentCount == 1) {
                stringRedisTemplate.expire(rateLimitKey, java.time.Duration.ofMinutes(1));
            }
            
            if (currentCount > maxRequests) {
                Long ttl = stringRedisTemplate.getExpire(rateLimitKey, java.util.concurrent.TimeUnit.SECONDS);
                long retryAfter = ttl != null && ttl > 0 ? ttl : 60;
                
                logger.warn("Rate limit exceeded for user: {} ({}/{})", username, currentCount, maxRequests);
                throw new com.github.activityconnector.exception.RateLimitExceededException(
                    String.format("Rate limit exceeded. Maximum %d requests per minute allowed.", maxRequests), 
                    retryAfter
                );
            }
            
            logger.debug("Rate limit check passed for user: {} ({}/{})", username, currentCount, maxRequests);
            
        } catch (com.github.activityconnector.exception.RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error checking rate limit for user {}: {}", username, e.getMessage());
        }
    }
    
    /**
     * Check rate limit with default limit of 5 requests per minute (for testing)
     */
    public void checkRateLimit(String username) {
        checkRateLimit(username, 5);
    }
    
    /**
     * Hash PAT using SHA-256
     */
    private String hashPat(String pat) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pat.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to hash PAT", e);
        }
    }

    /**
     * Get user activity data from Redis only - no GitHub API calls
     */
    public UserActivityResponse getUserActivityFromRedis(String username) {
        logger.info("Fetching user activity from Redis for: {}", username);
        
        List<GitHubRepository> repositories = getRepositoriesFromRedis(username);
        
        for (GitHubRepository repo : repositories) {
            List<GitHubCommit> commits = getCommitsFromRedis(username, repo.getName());
            repo.setRecentCommits(commits);
        }
        
        UserActivityResponse response = new UserActivityResponse(username, repositories);
        logger.info("Retrieved {} repositories from Redis for user: {}", repositories.size(), username);
        
        return response;
    }

    /**
     * Get repositories for a user from Redis
     */
    public List<GitHubRepository> getRepositoriesFromRedis(String username) {
        String reposKey = RedisConstants.REPOS_KEY_PREFIX + username;
        List<GitHubRepository> repositories = new ArrayList<>();
        
        try {
            Set<Object> repoKeys = redisTemplate.opsForHash().keys(reposKey);
            
            for (Object repoKey : repoKeys) {
                Object repoObj = redisTemplate.opsForHash().get(reposKey, repoKey);
                if (repoObj != null) {
                    try {
                        GitHubRepository repo = objectMapper.convertValue(repoObj, GitHubRepository.class);
                        repositories.add(repo);
                    } catch (Exception e) {
                        logger.warn("Error converting repository object for {}: {}", repoKey, e.getMessage());
                    }
                }
            }
            
            logger.debug("Found {} repositories in Redis for user: {}", repositories.size(), username);
        } catch (Exception e) {
            logger.warn("Error retrieving repositories from Redis for user {}: {}", username, e.getMessage());
        }
        
        return repositories;
    }

    /**
     * Get paginated repositories for a user from Redis with commits populated
     */
    public PaginatedResponse<GitHubRepository> getRepositoriesFromRedis(String username, int page, int size) {
        List<GitHubRepository> allRepositories = getRepositoriesFromRedis(username);
        
        // Populate commits for each repository
        for (GitHubRepository repo : allRepositories) {
            List<GitHubCommit> commits = getCommitsFromRedis(username, repo.getName());
            repo.setRecentCommits(commits);
        }
        
        int totalElements = allRepositories.size();
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);
        
        List<GitHubRepository> paginatedRepos = new ArrayList<>();
        if (startIndex < totalElements) {
            paginatedRepos = allRepositories.subList(startIndex, endIndex);
        }
        
        logger.debug("Returning page {} of repositories for user {}: {} items with commits", page, username, paginatedRepos.size());
        return new PaginatedResponse<>(paginatedRepos, page, size, totalElements);
    }

    /**
     * Get commits for a specific repository from Redis
     */
    public List<GitHubCommit> getCommitsFromRedis(String username, String repoName) {
        String commitsKey = RedisConstants.COMMITS_KEY_PREFIX + username + ":" + repoName;
        List<GitHubCommit> commits = new ArrayList<>();
        
        try {
            List<Object> commitObjects = redisTemplate.opsForList().range(commitsKey, 0, 19);
            
            if (commitObjects != null) {
                for (Object commitObj : commitObjects) {
                    try {
                        GitHubCommit commit = objectMapper.convertValue(commitObj, GitHubCommit.class);
                        commits.add(commit);
                    } catch (Exception e) {
                        logger.warn("Error converting commit object: {}", e.getMessage());
                    }
                }
            }
            
            logger.debug("Found {} commits in Redis for {}/{}", commits.size(), username, repoName);
        } catch (Exception e) {
            logger.warn("Error retrieving commits from Redis for {}/{}: {}", username, repoName, e.getMessage());
        }
        
        return commits;
    }

    /**
     * Get paginated commits for a specific repository from Redis
     */
    public PaginatedResponse<GitHubCommit> getCommitsFromRedis(String username, String repoName, int page, int size) {
        String commitsKey = RedisConstants.COMMITS_KEY_PREFIX + username + ":" + repoName;
        List<GitHubCommit> commits = new ArrayList<>();
        
        try {
            Long totalElements = redisTemplate.opsForList().size(commitsKey);
            if (totalElements == null) totalElements = 0L;
            
            int startIndex = page * size;
            int endIndex = startIndex + size - 1;
            
            List<Object> commitObjects = redisTemplate.opsForList().range(commitsKey, startIndex, endIndex);
            
            if (commitObjects != null) {
                for (Object commitObj : commitObjects) {
                    try {
                        GitHubCommit commit = objectMapper.convertValue(commitObj, GitHubCommit.class);
                        commits.add(commit);
                    } catch (Exception e) {
                        logger.warn("Error converting commit object: {}", e.getMessage());
                    }
                }
            }
            
            logger.debug("Found {} commits in Redis for {}/{} (page {})", commits.size(), username, repoName, page);
            return new PaginatedResponse<>(commits, page, size, totalElements);
            
        } catch (Exception e) {
            logger.warn("Error retrieving commits from Redis for {}/{}: {}", username, repoName, e.getMessage());
            return new PaginatedResponse<>(commits, page, size, 0L);
        }
    }

}
