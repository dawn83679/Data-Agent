package edu.zsc.ai.domain.service.db.support;

import edu.zsc.ai.domain.service.db.ConnectionAccessService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class ConnectionAccessBootstrap {

    @Resource
    public void setConnectionAccessService(ConnectionAccessService connectionAccessService) {
        ConnectionAccessSupport.setDelegate(connectionAccessService);
    }
}
