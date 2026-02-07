/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.couriers.api.web.user;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


public interface UserAccountRepository {
    Optional<UserAccount> findById(UUID id);
    Optional<UserAccount> findByExternalSubject(String subject);
    UserAccount save(UserAccount account);
    boolean existsByExternalSubject(String subject);
}