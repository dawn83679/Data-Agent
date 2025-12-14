package edu.zsc.ai.domain.model.entity.db;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Database Connection Entity
 * Represents a persistent database connection configuration.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "db_connections", autoResultMap = true)
public class DbConnection {

    /**
     * Primary key
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Connection name, must be globally unique
     */
    private String name;

    /**
     * Database type: mysql, postgresql, oracle, redis, etc.
     */
    private String dbType;

    /**
     * Host address
     */
    private String host;

    /**
     * Port number
     */
    private Integer port;

    /**
     * Database name
     */
    private String database;

    /**
     * Database username
     */
    private String username;

    /**
     * Database password (encrypted storage)
     */
    private String password;

    /**
     * Path to external JDBC driver JAR file
     */
    private String driverJarPath;

    /**
     * Connection timeout in seconds (default 30)
     */
    private Integer timeout;

    /**
     * Connection properties in JSON format (passwords, ssl settings, etc.)
     */
    private String properties;

    /**
     * User ID who owns this connection
     */
    private Long userId;

    /**
     * Creation time
     */
    private LocalDateTime createdAt;

    /**
     * Update time
     */
    private LocalDateTime updatedAt;
}