package edu.zsc.ai.api.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatUserMention {

    private String token;

    private String objectType;

    private Long connectionId;

    private String connectionName;

    private String catalogName;

    private String schemaName;

    private String objectName;
}
