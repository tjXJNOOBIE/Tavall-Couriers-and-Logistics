# SECURITY POLICY & ARCHITECTURE

## 1. Overview

This document outlines the security posture for the Gemini Courier Workflow Platform. Our philosophy is **"Trust Nothing, Verify Everything."** We assume the client is compromised and the network is hostile. The QR code is public information we must protect.


**SSL with TLS 1.3** is used for all communication between the client and the server.

## 2. QR Creation Security (The "Dumb Label")

We deliberately avoid embedding sensitive data such as address, keys, phone numbers, endpoints, etc. into the physical QR codes.

* **Payload:** The QR code contains *only* a UUID.
* **Zero Trust:** A scanned UUID grants **zero privileges** on its own. It is merely a pointer to a database record.
* **Immutability:** Physical labels are static; therefore, security measures must be dynamic (server-side).

## 3. QR Scanning Security (The "Smart Scanner")

Access to the scanning endpoint is strictly gated behind role-based authentication.

* **Auth Identity:** Spring Boot uses OAuth2 for authentication.
* **Session Security:** Private keys and decryption logic remain strictly server-side. The browser client holds only a short-lived access token (HttpOnly cookies preferred to prevent XSS).
* **RBAC (Role-Based Access Control):**
    * `ADMIN`: Overall system control.
    * `VENDOR`: Creates intake forms for drivers to scan/print.
    * `DRIVER`: Can only scan to change status (Pickup -> In Transit -> Delivered).
    * `DEFAULT`: No access. Scanning the QR code without a session fails gracefully.

## 4. QR Scab Edge Cases


On every scan event:
1.  **Velocity Checks:** Impossible travel detection (e.g., scanning the same package in two cities within 5 minutes).
2.  **State Validation:** A package cannot be "Delivered" if it is currently "Pending Pickup." The server enforces state transitions.
3.  **Audit Logging:** Every interaction with a UUID is logged with:
    * Timestamp
    * User ID
    * IP Address & User Agent
    * Geolocation (if granted)


## 5. Known Limitations
* **Physical Cloning:** We cannot prevent a bad actor from photocopying a label. However, the *utility* of a cloned label is nullified by our state machine (a delivered package cannot be delivered again).