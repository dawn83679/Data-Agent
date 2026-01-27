package edu.zsc.ai.domain.model.enums;

import lombok.Getter;

@Getter
public enum AuthProviderEnum {
    EMAIL("email"),
    GOOGLE("google"),
    GITHUB("github");

    private final String value;

    AuthProviderEnum(String value) {
        this.value = value;
    }
}
