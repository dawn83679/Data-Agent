package edu.zsc.ai.domain.service.oauth;

import edu.zsc.ai.util.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OAuthStrategyFactory {

    private final Map<String, OAuthStrategy> strategyMap;

    public OAuthStrategyFactory(List<OAuthStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(OAuthStrategy::getProviderName, Function.identity()));
    }

    public OAuthStrategy getStrategy(String provider) {
        OAuthStrategy strategy = strategyMap.get(provider.toUpperCase());
        BusinessException.assertNotNull(strategy, "Unsupported OAuth provider: " + provider);
        return strategy;
    }
}
