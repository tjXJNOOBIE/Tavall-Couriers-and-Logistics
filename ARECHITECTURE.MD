# Architecture Decisions & Purposes

## About

A document that contains decisions and purposes related to the architecture of the Tavall Couriers project. This includes the design of the Gemini Client, the internal API, and the external API. **Not for system administrator architecture decisions.**


# Tech Stack
1. Java
2. Spring Boot
3. PostgreSQL
4. Thymeleaf
5. Raw CSS and JS. No dedicated frontend frameworks.
    * **Why?**: Lower complexity, more secure to known vulnerabilities (no npm packages), less dependencies/bloat,

## Third Party Libraries
1. Maven minifier Plugin[https://github.com/logicsquad/minifier-maven-plugin]
2. Spring Data JPA
3. 
## System Abstractions

* **Gemini Clients(_org.tavall.gemini.clients_\):** Gemini3Client(abstract class) -> Gemini3ImageClient

## Gemini Client Separation

### System Explanation
The Gemini Clients are simply a group of classes that delegate tasks to different AI models. Each model has its own specific function and requires different data objects, so these classes act as the dedicated managers for those unique needs.

### What this Solves
We have different methods and objects that should not share space. Some models shouldn't be getting routed at all (isolated running). By splitting them, we ensure:

**Client = Gemini 3 SDK; AI API by Google, Inc.**

* **Improved Client Building:** Custom methods to add bew functions and data objects to client building process.
* **Client Isolation:** Run specific systems (like the Scanner) independently, on its on model. Make data objects and fuctions only certain models can access.
* **Data Containerization:** Prevents accidental mismatching of data (e.g., sending raw images to a text-only router).
* **Safety:** Prevents accidental mismatching of data (e.g., sending raw images to a text-only router).
* **Code Clarity:** Each client exposes only the functionality it supports.

### Other Solutions

#### The Single Service ("God Class")
* **The Idea:** One giant `GeminiService` class that handles everything by passing a "Model Type" flag (e.g., `geminiService.call(Model.FLASH, data)`).
* **Why Rejected:** Flags are type safe be can be mismanaged. It also makes it difficult to add new models without changing the `GeminiService` class. It also makes it difficult to add new models without changing the `GeminiService` class. It also allows you to pass, mutate, and return data to the wrong model because the method signature has to be generic enough to accept everything.

#### Direct SDK Access
* **The Idea:** Just letting our Manager classes call Google's `Client` directly.
* **Why Rejected:** It creates tight coupling.


## Namespaces
* **Package names**: Verbose > Clean
  * Example org.tavall.couriers.api.web.endpoints.camera.metadata > org.tavall.couriers.api.camera
* **Class names**: Verbose > Simple
  * **Example**: CameraFeedEndpoints > CameraEndpoints
  * **Why**: This is a camera **FEED**, not just a camera.

## Module Separation
* **gemini-api:** Contains Gemini SDK/API logic, manages AI objects and util classes such as, Gemini3ImageClient and AIResposneParser.
* **internal-courier-api:** Contains internal API logic, manages internal logic, that is later used in module "spring-webview".

## Concurrency

### Custom Concurrency Lib using Java Virtual Threads 
* A custom concurrency library built around Java Virual Threads is used to manage concurrent tasks. We confirm Virtual Threads are truly conccurern before building the library. Test class: "StrucutedConcurrentTest.java"
<pre><code>[Main] Opening StructuredTaskScope...
[Main] Forking tasks...
  -> [Thread: ] START test (Slow)
  -> [Thread: ] START test2 (Fast)
  -> [Thread: ] FINISH test2 (Fast)
  -> [Thread: ] FINISH test (Slow)
[Main] Tasks finished. Combining results.
Final Result: test2test
Total Duration: 68ms</code></pre>
_"test" method is slept 50ms to test concurrency. Both methods simply return strings.

#### Java Virtual Threads Limits
* Not everything is automatically atomic. We still need to declare Atomic objects, like AtomicInteger for exmaple.
* If two Substack(task) touch mutable data that aren't Atomic, we need proprer concurrency controls for those objects.

### Scanning Concurrency
We use Java Virtual Threads for scanner concurrency, instead of ThreadPools. Virtual Threads are more efficient and scalable than manually managing threads.
* **Thread Pool Limits:** Memory cost ~2MB per thread, pre-set pool size, every thread made is a real OS thread (blocking behavior), which isn't needed for scanning.
* **Virtual Thread Advantages:** One scan response = One Thread. Threads are not "pooled," allowing true concurrency. Less worry about thread safety, allocation amount, and can be scaled up or down dynamically without extra logic.




//TODO: Add section about module separation

//TODO: Add section about package/class separation and namespaces

//TODO: Possible sction abot BarcodeFormats