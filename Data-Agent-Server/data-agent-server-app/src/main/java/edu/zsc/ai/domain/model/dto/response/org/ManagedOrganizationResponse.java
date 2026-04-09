package edu.zsc.ai.domain.model.dto.response.org;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagedOrganizationResponse {

    private Long id;
    private String orgCode;
    private String orgName;
}
