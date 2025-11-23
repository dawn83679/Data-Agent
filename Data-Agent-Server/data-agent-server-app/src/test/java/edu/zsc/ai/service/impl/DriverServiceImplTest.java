package edu.zsc.ai.service.impl;

import edu.zsc.ai.model.dto.response.db.AvailableDriverResponse;
import edu.zsc.ai.model.dto.response.db.InstalledDriverResponse;
import edu.zsc.ai.service.db.DriverService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DriverService.
 */
@SpringBootTest
class DriverServiceImplTest {
    
    @Autowired
    private DriverService driverService;
    
    @Test
    void testListAvailableDrivers_MySQL() {
        List<AvailableDriverResponse> drivers = driverService.listAvailableDrivers("MySQL");
        
        assertNotNull(drivers);
        assertFalse(drivers.isEmpty(), "Should have at least one MySQL driver version available from Maven Central");
        
        AvailableDriverResponse driver = drivers.get(0);
        assertEquals("MySQL", driver.getDatabaseType());
        assertNotNull(driver.getVersion());
        assertNotNull(driver.getGroupId());
        assertNotNull(driver.getArtifactId());
        assertNotNull(driver.getMavenCoordinates());
        assertNotNull(driver.getInstalled());
        
        System.out.println("Available MySQL driver versions from Maven Central: " + drivers.size());
        drivers.stream().limit(10).forEach(d -> 
            System.out.println("  - " + d.getVersion() + (d.getInstalled() ? " [installed]" : "")));
    }
    
    @Test
    void testListAvailableDrivers_UnknownDatabaseType() {
        assertThrows(IllegalArgumentException.class, () -> {
            driverService.listAvailableDrivers("UnknownDB");
        });
    }
    
    @Test
    void testListInstalledDrivers() {
        // This test scans the real user directory
        // Just verify it doesn't throw exceptions
        List<InstalledDriverResponse> drivers = driverService.listInstalledDrivers("mysql");
        
        assertNotNull(drivers);
        // May be empty or have drivers, both are valid
        
        System.out.println("Installed drivers: " + drivers.size());
        drivers.forEach(d -> System.out.println("  - " + d.getFileName() + " (v" + d.getVersion() + ")"));
    }
}

