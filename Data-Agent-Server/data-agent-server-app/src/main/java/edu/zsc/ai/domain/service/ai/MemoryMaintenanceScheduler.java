package edu.zsc.ai.domain.service.ai;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edu.zsc.ai.config.ai.MemoryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryMaintenanceScheduler {

    private final MemoryService memoryService;
    private final MemoryProperties memoryProperties;

    @Scheduled(fixedDelayString = "${memory.maintenance.fixed-delay-ms:3600000}")
    public void runScheduledMaintenance() {
        if (!memoryProperties.isEnabled() || !memoryProperties.getMaintenance().isEnabled()) {
            return;
        }
        try {
            var report = memoryService.runGlobalMaintenance();
            log.info("Memory maintenance summary: disabledProcessed={}, enabledCount={}, disabledCount={}, duplicateEnabledCount={}",
                    report.getProcessedDisabledCount(),
                    report.getEnabledMemoryCount(),
                    report.getDisabledMemoryCount(),
                    report.getDuplicateEnabledMemoryCount());
        } catch (Exception e) {
            log.warn("Scheduled memory maintenance failed", e);
        }
    }
}
