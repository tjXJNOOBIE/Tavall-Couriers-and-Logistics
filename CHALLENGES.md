# About
This document contains the challenges for the Tavall Couriers project. Each challenge is described in detail, along with the expected implementation and any additional context or requirements.

# CHALLENGE 1: Gemini 3 Scan QR and OCR scan Concurrency
## Problem
Gemini 3 scans a QR code and a page's metadata, but this process takes 8.1 seconds and will stall our scanner. This process must be concurrent to improve the visual performance and flow of the scanning flow.
<details>
<summary>Gemini 3 Scan Response</summary>

<pre><code>--- STARTING GEMINI 3 FLASH VISION TEST ---
Loading Image: F:\workspace\TavallCouriers\spring-webview\QRWithData.png
--- GEMINI RESPONSE (8157ms) ---
ScanResponse[uuid=550e8400-e29b-41d4-a716-446655440000, cameraState=FOUND, trackingNumber=TRK-GOOGLE-9988, name=Google Gemini, address=1600 Amphitheatre Pkwy, Mountain View, CA 94043, phoneNumber=(555) 019-2834, deadline=null, notes=Do not bend. Contains data.]</code></pre>

</details>

## Solution
1. Stream Gemini 3's process using Google's Gemini 3 SDK built-in stream response.
2. Log in the background; Let the user see a completed scan, but run the scan in the background.
3. Process scan results in the background while the user is scanning

## Edge Cases
If a user is done scanning before streaming is complete?
 * This is where we finally show the user a waiting screen OR
 * Possible upgrade: Go to the confirmation page for scanning, fill any data we already have, then live-fill the rest of the data as scan responses come in. This way the user can start viewing confirmation/routes without having to wait.

# CHALLENGE 2: I/O Performance
## Problem
While Gemini 3 is sending a response, we have a "File" object open in linux. If multiple active scans are running, we may run into bottlenecks. 

## Solution
1. Change Linux file system limits to allow more open files.
   * **Important**: Running the app as a Linux service on in something like Docker will nullify the Linux File Limit changes.
2. How to change Linux file system limits? 
   * To set the limit to 1048576, run increase_file_limit.sh
   * To set the limit higher, run(kernel changes):
   <pre><code>sysctl -w fs.nr_open=20000000
   sysctl -w fs.file-max=20000000
   ulimit -n 20000000</code></pre>
   * View PERFORMANCE_LIMTS.MD to view application performance limits