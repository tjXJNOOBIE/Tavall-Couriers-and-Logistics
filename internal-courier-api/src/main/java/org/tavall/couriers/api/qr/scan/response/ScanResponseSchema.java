package org.tavall.couriers.api.qr.scan.response;


import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScanResponseSchema  {


    public ScanResponseSchema() {

    }

    /**
     *
     */
    //TODO: Update schema to match ScanRsponse/Shipping Label Meta Data
    public Schema getScanResponseSchema() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("uuid", Schema.builder()
                .type(Type.Known.STRING)
                .description("The UUID extracted from the QR code if present.")
                .nullable(true)
                .build());

        properties.put("cameraState", Schema.builder()
                .type(Type.Known.STRING)
                .enum_(List.of("SEARCHING", "ANALYZING", "FOUND", "ERROR"))
                .description("SEARCHING when no document is visible, ANALYZING while processing a frame, FOUND when a label is detected, ERROR on failure.")
                .build());

        properties.put("trackingNumber", Schema.builder()
                .type(Type.Known.STRING)
                .description("The courier tracking number.")
                .nullable(true)
                .build());

        properties.put("name", Schema.builder()
                .type(Type.Known.STRING)
                .description("Recipient name.")
                .nullable(true)
                .build());

        properties.put("address", Schema.builder()
                .type(Type.Known.STRING)
                .description("Full recipient address.")
                .nullable(true)
                .build());

        properties.put("city", Schema.builder()
                .type(Type.Known.STRING)
                .description("Recipient city.")
                .nullable(true)
                .build());

        properties.put("state", Schema.builder()
                .type(Type.Known.STRING)
                .description("Recipient state.")
                .nullable(true)
                .build());

        properties.put("zipCode", Schema.builder()
                .type(Type.Known.STRING)
                .description("Recipient zip code.")
                .nullable(true)
                .build());

        properties.put("country", Schema.builder()
                .type(Type.Known.STRING)
                .description("Recipient country.")
                .nullable(true)
                .build());

        properties.put("phoneNumber", Schema.builder()
                .type(Type.Known.STRING)
                .description("Recipient phone number.")
                .nullable(true)
                .build());

        properties.put("deadline", Schema.builder()
                .type(Type.Known.STRING)
                .description("Deliver By date in strict ISO-8601 format (e.g., 2026-01-14T15:00:00Z).")
                .nullable(true)
                .build());

        properties.put("notes", Schema.builder()
                .type(Type.Known.STRING)
                .description("Physical condition notes (e.g. 'Box dented', 'Do not bend').")
                .nullable(true)
                .build());

        Map<String, Schema> functionArgs = new HashMap<>();
        functionArgs.put("uuid", Schema.builder().type(Type.Known.STRING).nullable(true).build());
        functionArgs.put("trackingNumber", Schema.builder().type(Type.Known.STRING).nullable(true).build());
        functionArgs.put("name", Schema.builder().type(Type.Known.STRING).nullable(true).build());
        functionArgs.put("address", Schema.builder().type(Type.Known.STRING).nullable(true).build());
        functionArgs.put("city", Schema.builder().type(Type.Known.STRING).nullable(true).build());
        functionArgs.put("state", Schema.builder().type(Type.Known.STRING).nullable(true).build());
        functionArgs.put("zipCode", Schema.builder().type(Type.Known.STRING).nullable(true).build());
        functionArgs.put("country", Schema.builder().type(Type.Known.STRING).nullable(true).build());
        functionArgs.put("phoneNumber", Schema.builder().type(Type.Known.STRING).nullable(true).build());
        functionArgs.put("deadline", Schema.builder().type(Type.Known.STRING).nullable(true).build());

        Map<String, Schema> functionCallProps = new HashMap<>();
        functionCallProps.put("name", Schema.builder()
                .type(Type.Known.STRING)
                .description("Function name to call, if any.")
                .build());
        functionCallProps.put("arguments", Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(functionArgs)
                .build());

        properties.put("functionCall", Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(functionCallProps)
                .nullable(true)
                .build());

        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(properties)
                .required(List.of("cameraState")) // Only State is strictly required
                .build();
    }

}
