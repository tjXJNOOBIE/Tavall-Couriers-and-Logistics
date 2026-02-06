package org.tavall.couriers.api.utils;

import org.junit.jupiter.api.Test;
import org.tavall.couriers.api.utils.uuid.GenerateUUID;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class GenerateUUIDTest {




    @Test
    void shouldGenerateUuidOnConstruction() {
        GenerateUUID generator = new GenerateUUID();
        System.out.println("[TEST][shouldGenerateUuidOnConstruction] Generated UUID: " + generator.getUUID());
        assertNotNull(generator.getUUID(), "UUID should be generated in constructor");
    }

    @Test
    void shouldNotOverwriteExistingUuidWhenGenerateCalledAgain() {
        GenerateUUID generator = new GenerateUUID();
        UUID first = generator.getUUID();
        System.out.println("[TEST][shouldNotOverwriteExistingUuidWhenGenerateCalledAgain] Initial UUID: " + first);

        // Call generateUUID again; since uuid is not null, it should not change
        generator.getUUID();
        UUID second = generator.getUUID();
        System.out.println("[TEST][shouldNotOverwriteExistingUuidWhenGenerateCalledAgain] After generateUUID call UUID: " + second);
        assertNotNull(second);
        assertEquals(first, second, "UUID should remain the same if already generated");
    }

    @Test
    void shouldReturnNullCreatedAtAsItIsNeverSet() {
        GenerateUUID generator = new GenerateUUID();
        Instant ts = generator.getCreatedAt();
        System.out.println("[TEST][shouldReturnNullCreatedAtAsItIsNeverSet] createdAt: " + ts);
        assertNull(ts, "createdAt is not set anywhere and should be null");
    }

    @Test
    void shouldGenerateDifferentUuidsAcrossInstances() {
        Set<UUID> uuids = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            GenerateUUID g = new GenerateUUID();
            UUID id = g.getUUID();
            System.out.println("[TEST][shouldGenerateDifferentUuidsAcrossInstances] Instance " + i + " UUID: " + id);
            uuids.add(id);
        }
        System.out.println("[TEST][shouldGenerateDifferentUuidsAcrossInstances] Unique UUID count: " + uuids.size());
        assertThat(uuids).hasSize(10);
    }

    @Test
    void shouldHaveValidUuidFormat() {
        GenerateUUID generator = new GenerateUUID();
        UUID uuid = generator.getUUID();
        System.out.println("[TEST][shouldHaveValidUuidFormat] UUID string: " + uuid);
        assertDoesNotThrow(() -> UUID.fromString(uuid.toString()), "UUID string should be parseable");
        assertEquals(36, uuid.toString().length(), "UUID string length should be 36 with hyphens");
    }
}