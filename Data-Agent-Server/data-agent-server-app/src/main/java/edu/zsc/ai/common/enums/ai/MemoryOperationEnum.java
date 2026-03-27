package edu.zsc.ai.common.enums.ai;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemoryOperationEnum {

    CREATE("CREATE"),
    UPDATE("UPDATE"),
    DELETE("DELETE");

    private final String code;

    public static MemoryOperationEnum fromCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        return Arrays.stream(values())
                .filter(value -> value.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElse(null);
    }

    public static List<String> validCodes() {
        return Arrays.stream(values())
                .map(MemoryOperationEnum::getCode)
                .toList();
    }
}
