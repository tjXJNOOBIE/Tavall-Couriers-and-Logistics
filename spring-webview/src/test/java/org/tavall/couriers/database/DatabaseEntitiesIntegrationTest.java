package org.tavall.couriers.database;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.tavall.couriers.api.database.QRMetaDataEntity;
import org.tavall.couriers.api.database.ScanResponseEntity;
import org.tavall.couriers.api.database.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.database.TrackingNumberMetaDataEntity;
import org.tavall.couriers.api.delivery.state.DeliveryState;
import org.tavall.couriers.api.qr.enums.QRState;
import org.tavall.couriers.api.qr.enums.QRType;
import org.tavall.couriers.api.qr.scan.state.LiveCameraState;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@EntityScan(basePackages = "org.tavall.couriers.api.database")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DatabaseEntitiesIntegrationTest {

    @Autowired
    private EntityManager entityManager;
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("tavall_test")
            .withUsername("tavall")
            .withPassword("tavall");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
//    @BeforeAll
//    static void beforeAll() {
//        // Initialize container with a specific Docker image
//        postgresContainer = new PostgreSQLContainer("postgres:16-alpine");
//        postgresContainer.start();
//        // The container is now running and connection details can be retrieved
//    }
//
//    @AfterAll
//    static void afterAll() {
//        if (postgresContainer != null) {
//            postgresContainer.stop();
//        }
//    }
//
//
//    @Test
//    void testConnection() throws SQLException {
//        // Use the container's generated connection details to connect
//        String jdbcUrl = postgresContainer.getJdbcUrl();
//        String username = postgresContainer.getUsername();
//        String password = postgresContainer.getPassword();
//
//        // Connect to the database using JDBC
//        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
//            // Your test logic goes here
//            // e.g., execute a simple query to verify connection
//            System.out.println("Connection successful!");
//        }
//    }
    @Test
    void persistsAndLoadsAllEntities() {
        UUID qrUuid = UUID.randomUUID();
        QRMetaDataEntity qrMetaData = new QRMetaDataEntity(
                qrUuid,
                "QR-CONTENT",
                Instant.now(),
                QRType.UUID,
                QRState.ACTIVE);

        ShippingLabelMetaDataEntity shippingLabel = new ShippingLabelMetaDataEntity(
                "label-" + qrUuid,
                "TRACK-10001",
                "Taylor Recipient",
                "555-0101",
                "123 Courier Ave",
                "Denver",
                "CO",
                "80202",
                "US",
                true,
                Instant.now().plusSeconds(86_400),
                DeliveryState.IN_TRANSIT
        );

        TrackingNumberMetaDataEntity trackingMeta = new TrackingNumberMetaDataEntity(
                "TRACK-10001",
                qrUuid
        );

        ScanResponseEntity scanResponse = new ScanResponseEntity(
                "scan-" + qrUuid,
                LiveCameraState.FOUND,
                "TRACK-10001",
                "Taylor Recipient",
                "123 Courier Ave",
                "Denver",
                "CO",
                "80202",
                "US",
                "555-0101",
                Instant.now().plusSeconds(3_600),
                "Left at front desk"
        );

        entityManager.persist(qrMetaData);
        entityManager.persist(shippingLabel);
        entityManager.persist(trackingMeta);
        entityManager.persist(scanResponse);
        entityManager.flush();
        entityManager.clear();

        QRMetaDataEntity loadedQr = entityManager.find(QRMetaDataEntity.class, qrUuid);
        assertThat(loadedQr).isNotNull();
        assertThat(loadedQr.getQrState()).isEqualTo(QRState.ACTIVE);

        ShippingLabelMetaDataEntity loadedLabel = entityManager.find(ShippingLabelMetaDataEntity.class, shippingLabel.getUuid());
        assertThat(loadedLabel).isNotNull();
        assertThat(loadedLabel.getTrackingNumber()).isEqualTo("TRACK-10001");

        TrackingNumberMetaDataEntity loadedTracking = entityManager.find(TrackingNumberMetaDataEntity.class, "TRACK-10001");
        assertThat(loadedTracking).isNotNull();
        assertThat(loadedTracking.getQrUuid()).isEqualTo(qrUuid);

        ScanResponseEntity loadedScan = entityManager.find(ScanResponseEntity.class, scanResponse.getUuid());
        assertThat(loadedScan).isNotNull();
        assertThat(loadedScan.getCameraState()).isEqualTo(LiveCameraState.FOUND);
    }
}