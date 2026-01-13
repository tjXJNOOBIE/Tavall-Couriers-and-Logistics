package org.tavall.gemini.clients.response;


import org.tavall.gemini.clients.response.metadata.ClientResponseMetadata;
import org.tavall.gemini.utils.AIResponseParser;

//Class to parse generic objects, we should make this a abstract class
// and pass response objects to other class via inheritance
public class Gemini3ImageClientResponse<T> {

    private final T response;
    private final ClientResponseMetadata responseMetadata;
   public Gemini3ImageClientResponse(T response, ClientResponseMetadata responseMetadata, ClientResponseMetadata responseMetadata1) {
       this.response = response;
       this.responseMetadata = responseMetadata1;
   }

   public T getResponse() {
       return response;
   }

   public ClientResponseMetadata getClientResponseMetadata() {
       return responseMetadata;
   }

   public void makeObjects(){
       //TODO: Get repose and parser with AIPrasrer
   }




}