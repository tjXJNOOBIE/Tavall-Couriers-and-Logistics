package org.tavall.couriers.api.qr.scan.response;


import com.google.genai.types.Schema;
import com.google.genai.types.Type;


public class ScanResponseSchema  {


    public ScanResponseSchema() {

    }

    /**
     *
     */
    //TODO: Update schema to match ScanRsponse/Shipping Label Meta Data
    public Schema getScanResponseSchema() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(new java.util.HashMap<>() {{
                    put("uuid", Schema.builder()
                            .type(Type.Known.STRING)
                            .description("The UUID extracted from the QR code if present.")
                            .nullable(true)
                            .build());

                    put("cameraState", Schema.builder()
                            .type(Type.Known.STRING)
                            .enum_(java.util.Arrays.asList("FOUND", "ERROR"))
                            .description("FOUND if label data is legible. ERROR if label is damaged or unreadable.")
                            .build());

                    put("trackingNumber", Schema.builder()
                            .type(Type.Known.STRING)
                            .description("The courier tracking number.")
                            .nullable(true)
                            .build());

                    put("name", Schema.builder()
                            .type(Type.Known.STRING)
                            .description("Recipient name.")
                            .nullable(true)
                            .build());

                    put("address", Schema.builder()
                            .type(Type.Known.STRING)
                            .description("Full recipient address.")
                            .nullable(true)
                            .build());

                    put("phoneNumber", Schema.builder()
                            .type(Type.Known.STRING)
                            .description("Recipient phone number.")
                            .nullable(true)
                            .build());

                    put("deadline", Schema.builder()
                            .type(Type.Known.STRING)
                            .description("Deliver By date in strict ISO-8601 format (e.g., 2026-01-14T15:00:00Z).")
                            .nullable(true)
                            .build());

                    put("notes", Schema.builder()
                            .type(Type.Known.STRING)
                            .description("Physical condition notes (e.g. 'Box dented', 'Do not bend').")
                            .nullable(true)
                            .build());
                }})
                .required(java.util.Arrays.asList("cameraState")) // Only State is strictly required
                .build();
    }

}