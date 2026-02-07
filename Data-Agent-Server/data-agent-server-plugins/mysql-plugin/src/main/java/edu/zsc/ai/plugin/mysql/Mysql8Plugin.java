package edu.zsc.ai.plugin.mysql;

import edu.zsc.ai.plugin.annotation.PluginInfo;
import edu.zsc.ai.plugin.enums.DbType;

@PluginInfo(
    id = "mysql-8",
    name = "MySQL 8.0+",
    version = "0.0.1",
    dbType = DbType.MYSQL,
    description = "MySQL 8.0+ database plugin with full CRUD and metadata support, including new features like CTE and window functions",
    supportMinVersion = "8.0.0"
)
public class Mysql8Plugin extends DefaultMysqlPlugin {
    
    @Override
    protected String getDriverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }
}

