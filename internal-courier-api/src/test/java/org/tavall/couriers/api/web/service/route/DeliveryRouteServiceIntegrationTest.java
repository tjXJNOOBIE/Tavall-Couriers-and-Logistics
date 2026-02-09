package org.tavall.couriers.api.web.service.route;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.route.cache.RouteCacheService;
import org.tavall.couriers.api.web.entities.DeliveryRouteEntity;
import org.tavall.couriers.api.web.entities.DeliveryRouteStopEntity;
import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;
import org.tavall.couriers.api.web.repositories.DeliveryRouteRepository;
import org.tavall.couriers.api.web.repositories.DeliveryRouteStopRepository;
import org.tavall.couriers.api.web.service.shipping.ShippingLabelMetaDataService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryRouteServiceIntegrationTest {

    @Test
    void createRouteFromLabelsTest() {
        Log.info("Starting test: createRouteFromLabelsTest");
        DeliveryRouteRepository routeRepository = mock(DeliveryRouteRepository.class);
        DeliveryRouteStopRepository stopRepository = mock(DeliveryRouteStopRepository.class);
        ShippingLabelMetaDataService shippingService = mock(ShippingLabelMetaDataService.class);
        RoutePlannerService routePlanner = mock(RoutePlannerService.class);
        RouteCacheService routeCache = new RouteCacheService();

        DeliveryRouteService service = new DeliveryRouteService(
                routeRepository,
                stopRepository,
                shippingService,
                routePlanner,
                routeCache
        );

        ShippingLabelMetaDataEntity first = buildLabel("uuid-a", "TAV-A");
        ShippingLabelMetaDataEntity second = buildLabel("uuid-b", "TAV-B");
        when(shippingService.findByUuid("uuid-a")).thenReturn(first);
        when(shippingService.findByUuid("uuid-b")).thenReturn(second);
        when(routePlanner.planRoute(anyList(), anyDouble(), anyInt())).thenReturn(new RoutePlan(List.of("uuid-b", "uuid-a"), "planned"));

        when(routeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stopRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryRouteEntity route = service.createRouteFromLabels(List.of("uuid-a", "uuid-b"));
        assertNotNull(route);
        assertTrue(route.getRouteId().startsWith("RTE-"));
        assertEquals(2, route.getLabelCount());
        assertEquals("planned", route.getNotes());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DeliveryRouteStopEntity>> stopsCaptor =
                ArgumentCaptor.forClass((Class) List.class);
        verify(stopRepository, timeout(500)).saveAll(stopsCaptor.capture());

        List<DeliveryRouteStopEntity> stops = stopsCaptor.getValue();
        assertEquals(2, stops.size());
        assertEquals("uuid-b", stops.get(0).getLabelUuid());
        assertEquals(1, stops.get(0).getStopOrder());
        assertEquals("uuid-a", stops.get(1).getLabelUuid());
        assertEquals(2, stops.get(1).getStopOrder());
        Log.success("createRouteFromLabelsTest passed validation.");
    }

    @Test
    void addStopsTest() {
        Log.info("Starting test: addStopsTest");
        DeliveryRouteRepository routeRepository = mock(DeliveryRouteRepository.class);
        DeliveryRouteStopRepository stopRepository = mock(DeliveryRouteStopRepository.class);
        ShippingLabelMetaDataService shippingService = mock(ShippingLabelMetaDataService.class);
        RoutePlannerService routePlanner = mock(RoutePlannerService.class);
        RouteCacheService routeCache = new RouteCacheService();

        DeliveryRouteService service = new DeliveryRouteService(
                routeRepository,
                stopRepository,
                shippingService,
                routePlanner,
                routeCache
        );

        DeliveryRouteEntity existing = new DeliveryRouteEntity(
                "RTE-1",
                "PLANNED",
                1,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                null
        );
        when(routeRepository.findById("RTE-1")).thenReturn(Optional.of(existing));

        DeliveryRouteStopEntity existingStop = new DeliveryRouteStopEntity(
                "stop-1",
                "RTE-1",
                "uuid-a",
                1,
                Instant.now()
        );
        AtomicInteger stopQueryCount = new AtomicInteger(0);
        when(stopRepository.findByRouteIdOrderByStopOrderAsc("RTE-1"))
                .thenAnswer(invocation -> {
                    if (stopQueryCount.getAndIncrement() == 0) {
                        return List.of(existingStop);
                    }
                    DeliveryRouteStopEntity secondStop = new DeliveryRouteStopEntity(
                            "stop-2",
                            "RTE-1",
                            "uuid-b",
                            2,
                            Instant.now()
                    );
                    DeliveryRouteStopEntity thirdStop = new DeliveryRouteStopEntity(
                            "stop-3",
                            "RTE-1",
                            "uuid-c",
                            3,
                            Instant.now()
                    );
                    return List.of(existingStop, secondStop, thirdStop);
                });

        ShippingLabelMetaDataEntity second = buildLabel("uuid-b", "TAV-B");
        ShippingLabelMetaDataEntity third = buildLabel("uuid-c", "TAV-C");
        when(shippingService.findByUuid("uuid-b")).thenReturn(second);
        when(shippingService.findByUuid("uuid-c")).thenReturn(third);
        when(routePlanner.planRoute(anyList(), anyDouble(), anyInt())).thenReturn(new RoutePlan(List.of("uuid-c", "uuid-b"), "extra"));

        when(routeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stopRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryRouteEntity updated = service.addStops("RTE-1", List.of("uuid-b", "uuid-c"));
        assertNotNull(updated);
        assertEquals(3, updated.getLabelCount());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DeliveryRouteStopEntity>> stopsCaptor =
                ArgumentCaptor.forClass((Class) List.class);
        verify(stopRepository, timeout(500)).saveAll(stopsCaptor.capture());

        List<DeliveryRouteStopEntity> stops = stopsCaptor.getValue();
        assertEquals(2, stops.size());
        assertEquals("uuid-c", stops.get(0).getLabelUuid());
        assertEquals(2, stops.get(0).getStopOrder());
        assertEquals("uuid-b", stops.get(1).getLabelUuid());
        assertEquals(3, stops.get(1).getStopOrder());
        Log.success("addStopsTest passed validation.");
    }

    private ShippingLabelMetaDataEntity buildLabel(String uuid, String trackingNumber) {
        return new ShippingLabelMetaDataEntity(
                uuid,
                trackingNumber,
                "Recipient",
                "555-0101",
                "123 Market St",
                "San Jose",
                "CA",
                "95112",
                "USA",
                false,
                Instant.now(),
                null
        );
    }
}
