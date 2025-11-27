package edu.zsc.ai.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Google User Info Response
 * Data returned from Google OAuth API
 *
 * @author Data-Agent Team
 */
@Data
public class GoogleUserInfo {

    /**
     * Google user ID
     */
    @JsonProperty("sub")
    private String googleId;

    /**
     * User email
     */
    private String email;

    /**
     * Email verified status
     */
    @JsonProperty("email_verified")
    private Boolean emailVerified;

    /**
     * User name
     */
    private String name;

    /**
     * User picture URL
     */
    private String picture;

    /**
     * Given name (first name)
     */
    @JsonProperty("given_name")
    private String givenName;

    /**
     * Family name (last name)
     */
    @JsonProperty("family_name")
    private String familyName;

    /**
     * Locale
     */
    private String locale;
}
