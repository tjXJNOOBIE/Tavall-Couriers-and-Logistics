package org.tavall.couriers.api.web.service.hq;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.tavall.couriers.api.console.Log;
import org.tavall.couriers.api.web.entities.HqLocationEntity;
import org.tavall.couriers.api.web.repositories.HqLocationRepository;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class HqLocationService {

    private static final String DEFAULT_NAME = "TAVALL COURIERS HQ";
    private static final String DEFAULT_ADDRESS = "123 Java Stream Blvd, Colton, CA 92324";

    private final HqLocationRepository repository;
    private final AtomicReference<HqLocationEntity> defaultLocation = new AtomicReference<>();

    public HqLocationService(HqLocationRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void primeCache() {
        refreshDefault();
    }

    public HqLocationEntity getDefaultLocation() {
        HqLocationEntity cached = defaultLocation.get();
        if (cached != null) {
            return cached;
        }
        refreshDefault();
        return defaultLocation.get();
    }

    public String resolveFromName() {
        HqLocationEntity location = getDefaultLocation();
        if (location != null && !isBlank(location.getName())) {
            return location.getName().trim();
        }
        return DEFAULT_NAME;
    }

    public String resolveFromAddressLine() {
        HqLocationEntity location = getDefaultLocation();
        String formatted = formatAddress(location);
        return !isBlank(formatted) ? formatted : DEFAULT_ADDRESS;
    }

    public String formatAddress(HqLocationEntity location) {
        if (location == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        appendDelimited(builder, location.getAddress(), ", ");
        appendDelimited(builder, location.getCity(), ", ");
        appendDelimited(builder, formatStateZip(location.getState(), location.getZipCode()), ", ");
        appendDelimited(builder, location.getCountry(), ", ");
        return builder.toString().trim();
    }

    private void refreshDefault() {
        try {
            HqLocationEntity resolved = repository.findFirstByDefaultLocationTrue();
            if (resolved == null) {
                resolved = repository.findFirstByOrderByCreatedAtAsc();
            }
            if (resolved != null) {
                defaultLocation.set(resolved);
            }
        } catch (Exception ex) {
            Log.warn("[HqLocation] Unable to load HQ location: " + ex.getMessage());
            Log.exception(ex);
        }
    }

    private void appendDelimited(StringBuilder builder, String value, String delimiter) {
        if (isBlank(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(delimiter);
        }
        builder.append(value.trim());
    }

    private String formatStateZip(String state, String zip) {
        if (isBlank(state) && isBlank(zip)) {
            return null;
        }
        if (isBlank(state)) {
            return zip.trim();
        }
        if (isBlank(zip)) {
            return state.trim();
        }
        return state.trim() + " " + zip.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
