package edu.zsc.ai.plugin.mysql.util;

import edu.zsc.ai.plugin.capability.MysqlIdentifierEscaper;
import org.apache.commons.lang3.StringUtils;

public final class MysqlIdentifierBuilder {

    private MysqlIdentifierBuilder() {
    }

    public static String buildFullIdentifier(String catalog, String objectName) {
        if (StringUtils.isBlank(objectName)) {
            throw new IllegalArgumentException("Object name must not be null or empty");
        }

        if (StringUtils.isNotBlank(catalog)) {
            String quotedCatalog = MysqlIdentifierEscaper.getInstance().quoteIdentifier(catalog);
            String quotedObject = MysqlIdentifierEscaper.getInstance().quoteIdentifier(objectName);
            return String.format("%s.%s", quotedCatalog, quotedObject);
        } else {
            return MysqlIdentifierEscaper.getInstance().quoteIdentifier(objectName);
        }
    }
}
