package edu.zsc.ai.domain.mapper.db;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.domain.model.entity.db.DbConnection;
import org.apache.ibatis.annotations.Mapper;

/**
 * Database Connection Mapper
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Mapper
public interface DbConnectionMapper extends BaseMapper<DbConnection> {

}