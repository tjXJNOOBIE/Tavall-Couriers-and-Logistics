# Architecture Decisions & Purposes

## About

A document that contains decisions and purposes related to the architecture of the Tavall Couriers project. This includes the design of the Gemini Client, the internal API, and the external API. **Not for system administrator architecture decisions.**

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

//TODO: Add section about module separation

//TODO: Add section about package/class separation and namespaces

//TODO: Possible sction abot BarcodeFormats