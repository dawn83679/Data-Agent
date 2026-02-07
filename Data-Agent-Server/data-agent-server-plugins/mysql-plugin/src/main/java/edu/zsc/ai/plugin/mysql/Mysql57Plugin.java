package edu.zsc.ai.plugin.mysql;

import edu.zsc.ai.plugin.annotation.PluginInfo;
import edu.zsc.ai.plugin.enums.DbType;

@PluginInfo(
    id = "mysql-5.7",
    name = "MySQL 5.7",
    version = "0.0.1",
    dbType = DbType.MYSQL,
    description = "MySQL 5.7 database plugin with full CRUD and metadata support",
    supportMinVersion = "5.7.0",
    supportMaxVersion = "7.9.99"
)
public class Mysql57Plugin extends DefaultMysqlPlugin {
    
    @Override
    protected String getDriverClassName() {
        return "com.mysql.jdbc.Driver";
    }
}

