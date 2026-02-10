# README

⚠️ JUDGES: VIEW COMMIT HISTORY HERE ⚠️
> **Note:** Due to a git configuration error during the hackathon crunch,> [CLICK HERE TO VIEW PROJECT HISTORY](https://github.com/YOUR_USER/YOUR_REPO/commits/master)
> all development history (200+ commits) is on the `master` branch.
> [CLICK HERE TO VIEW PROJECT HISTORY](https://github.com/tjXJNOOBIE/Tavall-Couriers-and-Logistics/tree/master)


## Table of Contents

I. [ARECHITECTURE.MD](ARECHITECTURE.MD)  
II. [CHALLENGES.md](CHALLENGES.md)  
III. [FLOWS.MD](FLOWS.MD)  
IV. [GEMINI_USAGE.MD](GEMINI_USAGE.MD)  
V. [SECURITY.MD](SECURITY.MD)  
VI. [token-saving-strategies.md](token-saving-strategies.md)  
VII. [spring-webview/main/resources/API_ENDPOINTS.md](spring-webview/main/resources/API_ENDPOINTS.md)  
VIII. [spring-webview/target/classes/API_ENDPOINTS.md](spring-webview/target/classes/API_ENDPOINTS.md)  
IX. [TODO.md](TODO.md)

# Tavall Couriers Architecture 

## About

A document that contains decisions and purposes related to the architecture of the Tavall Couriers project. This includes the design of the Gemini Client, the internal API, and the external API. **Not for system administrator architecture decisions.**


# Tech Stack

* **Java 25** (Preview Features Enabled)
* **Spring Boot 4.0.2**
* **PostgreSQL**
* **Thymeleaf (HTML)**
* **Raw/Custom CSS and JS**
    * **Why?**: Lower complexity, higher security (zero npm supply chain vulnerabilities), no hydration bloat.

## Gemini Clients

### Separate Gemini Clients
The Gemini Clients are simply a group of classes that delegate tasks to different AI models. Each model has its own specific function and requires different data objects, so these classes act as the dedicated managers for those unique needs.

### What this Solves
1.  **Type Safety:** We don't accidently send a video byte array to a text-only model. The `ImageClient` only accepts `byte[]` and the `TextClient` only accepts `String`.
2.  **Single Responsibility:** If we need to change how we handle image compression, we only edit `Gemini3ImageClient`. The text processing logic remains untouched.
3.  **Config Isolation:** The "Flash" model needs a lower temperature than the "Pro" model. Separating clients allows us to hard-code these optimized settings per client type.

# SECURITY POLICY & ARCHITECTURE

## 1. Overview

This document outlines the security posture for the Gemini Courier Workflow Platform. Our philosophy is **"Trust Nothing, Verify Everything."** We assume the client is compromised and the network is hostile. The QR code is public information we must protect.

## 2. QR Creation Security (The "Dumb Label")

We deliberately avoid embedding sensitive data such as address, keys, phone numbers, endpoints, etc. into the physical QR codes.

* **Payload:** The QR code contains *only* a UUID.
* **Zero Trust:** A scanned UUID grants **zero privileges** on its own. It is merely a pointer to a database record.
* **Immutability:** Physical labels are static; therefore, security measures must be dynamic (server-side).

## 3. QR Scanning Security (The "Smart Scanner")

Access to the scanning endpoint is strictly gated behind role-based authentication.

* **Auth Identity:** Spring Boot uses OAuth2 for authentication.
* **RBAC (Role-Based Access Control):**
    * `ADMIN`: Overall system control.
    * `VENDOR`: Creates intake forms for drivers to scan/print.
    * `DRIVER`: Can only scan to change status (Pickup -> In Transit -> Delivered).
    * `DEFAULT`: No access. Scanning the QR code without a session fails gracefully.

# Gemini 3 Usages

I used Java 25 to leverage Gemini 3's function calling, image processing, and used the LiveAPI with Gemini 3 Flash.

## Main Gemini Use Cases

### Function calling
* Gemini 3 Flash uses a function calling to create a label deterministically
* Gemini 3 Flash uses function calling to call for route building after second tier address validation.

### Image processing
* Gemini 3 Flash uses image processing to scan QR codes and extract data

## Data Validation
* Gemini 3 Flash uses Grounding with Google Maps for address validation for route building.

### Live Camera View
* Gemini 3 Flash provides a live camera view for first-step address validation.
    * I further validate this data on a new thread without LiveAPI before building to ensure accurate routing.

# Application Flows 

## Flow Rules:

* Spring Boot is used for the Web Server for this project

1. All data creation, data persistence, and data retrieval are handled in java, meaning these flows should remain as much as a visual layer as possible.
    * Any data objects not created were either missed or not yet created.
2. Web flows should keep mobile optimization (view and performance) in mind.

# Data Storage

## Memory Storage

* A 'AbstractCache.java' system was built to deal with in memory storage, mostly concurrently.
    * View package: **com.tavall.couriers.api.cache**

## Persistent Storage

* **Postgres** is used to store our data long-term. It is also used to rehydrate caches from downed systems.
    * View package: **com.tavall.couriers.api.database**

# About
This document contains the challenges for the Tavall Couriers project. Each challenge is described in detail, along with the expected implementation and any additional context or requirements.

# CHALLENGE 1: Gemini 3 Scan QR and OCR scan Concurrency

## Solution
1. Stream Gemini 3's process using Google's Gemini 3 SDK built-in stream response.
2. Log in the background; Let the user see a completed scan, but run the scan in the background.
3. Process scan results in the background while the user is scanning.

# CHALLENGE 3: Wasted Compute on Bad Frames (The "Bouncer")

## Solution
We implemented a custom "Bouncer" algorithm that runs locally on the CPU *before* we even think about calling Gemini.
* **Technique:** Analyze the raw byte array for `luminance` variance and edge density.
* **Result:** If the image is too dark, too blurry, or lacks contrast (like a blank wall), we kill the request instantly. No API call, no cost, zero latency. Only the "good stuff" gets through.

# CHALLENGE 4: Thread Scalability (Project Loom)

## Solution
We ditched traditional pools for **Java 25 Virtual Threads**.
* **The Flex:** These threads are managed by the JVM, not the OS. They are practically free.
* **Result:** We can spawn a fresh virtual thread for *every single scan request*. No blocking, no "thread pool exhaustion," and we aren't eating RAM like it's a Chrome tab. Massive vertical scalability for free.

# CHALLENGE 6: Verification Cost Efficiency

## Solution
A tiered escalation system.
* **Tier 1 (The Intern):** We use `Gemini 2 Flash Lite` for rapid, cheap text verification.
* **Tier 2 (The Boss):** If the Lite model gets confused or the image is tricky, we escalate to `Gemini 3 Flash/Pro` to handle the heavy lifting.
* **Result:** We save budget on the easy stuff so we can afford the smarts for the hard stuff.

# CHALLENGE 7: Frontend Complexity & Security

## Solution
"No-Build" Architecture.
* **Tech:** Server-side rendered Thymeleaf with raw, hand-tuned CSS and Vanilla JS.
* **Result:** * **Speed:** Pages load instantly (no hydration wait times).
    * **Security:** Zero npm supply chain vulnerabilities.
    * **Sanity:** No webpack config hell. It just works.

# Token Savings Playbook

This document captures the pragmatic knobs we turn to reduce Gemini/LiveAPI token usage while still powering the features requested above.

## 1. Local Pre-Processing
- **Local QR detector first.** We decode or extract QR payloads on the client (camera page) or backend service before reaching Gemini. This lets us skip AI rounds that only lookup an already-known UUID or tracking number.
- **LiveAPI gating.** The camera feed initially runs the local detector and only forwards image bytes (or metadata) to Gemini when the detector signals *missing* data or ambiguous input. That avoids repeated AI calls per frame.

# Tavall Couriers API Endpoints

## Scan Operations

### Scan QR Code (Entry Point)
```
GET /api/scan/{qrId}
Auth: Required
Response: {
  qrId,
  hasPayload: boolean,
  payload?: PayloadDTO,
  allowedActions: string[]
}
```

## Route Management

### Create Route
```
POST /api/routes
Auth: Required (ADMIN, OPERATOR)
Body: { routeName, assignedDriver? }
Response: RouteDTO
```

## Authentication

### Login
```
POST /api/auth/login
Body: { username, password }
Response: { token, username, role }
```

### Current User
```
GET /api/auth/me
Auth: Required
Response: UserDTO
```

## Role-Based Permissions

- **ADMIN**: All operations
- **OPERATOR**: Create QR, create routes, assign QR to routes, view all
- **DRIVER**: Scan, update payload, update delivery status, view assigned routes
- **VIEWER**: Read-only access

## License

**Copyright (c) 2026 Taheesh (TJ) Valentine**

This project is licensed under the **PolyForm Noncommercial License 1.0.0**.

**You are free to:**
* **View** and **Run** the code for educational or evaluation purposes.
* **Edit** and **Modify** the code for personal experiments.
* **Share** the code with others (non-commercially).

**You may NOT:**
* Use this project for any **commercial purpose** (making money, business use, etc.).
* Sell or license this code to others.

For the full legal text, please view the [LICENSE](LICENSE) file in this repository.
