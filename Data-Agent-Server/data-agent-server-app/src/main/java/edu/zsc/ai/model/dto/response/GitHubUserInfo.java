package edu.zsc.ai.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * GitHub User Information Response
 *
 * @author Data-Agent Team
 */
@Data
public class GitHubUserInfo {

    /**
     * GitHub user ID
     */
    @JsonProperty("id")
    private Long githubId;

    /**
     * GitHub login username
     */
    @JsonProperty("login")
    private String login;

    /**
     * User's display name
     */
    @JsonProperty("name")
    private String name;

    /**
     * User's email address
     */
    @JsonProperty("email")
    private String email;

    /**
     * User's avatar URL
     */
    @JsonProperty("avatar_url")
    private String avatarUrl;

    /**
     * User's bio
     */
    @JsonProperty("bio")
    private String bio;

    /**
     * User's location
     */
    @JsonProperty("location")
    private String location;

    /**
     * User's company
     */
    @JsonProperty("company")
    private String company;
}
