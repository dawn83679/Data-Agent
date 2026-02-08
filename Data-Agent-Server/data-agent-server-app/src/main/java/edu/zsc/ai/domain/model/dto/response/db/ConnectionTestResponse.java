package edu.zsc.ai.domain.model.dto.response.db;

import edu.zsc.ai.common.enums.db.ConnectionTestStatuEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Connection Test Response DTO
 * Contains detailed information about the connection test result.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionTestResponse {
    
    /**
     * Test status
     */
    private ConnectionTestStatuEnum status;
    
    /**
     * DBMS information: "MySQL (ver. 8.0.43)"
     */
    private String dbmsInfo;
    
    /**
     * Driver information: "MySQL Connector/J (ver. mysql-connector-j-8.2.0, JDBC4.2)"
     */
    private String driverInfo;
    
    /**
     * Connection response time in milliseconds
     */
    private Long ping;
}

