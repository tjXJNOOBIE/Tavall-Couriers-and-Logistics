package org.tavall.couriers.api.web.endpoints.camera;


import org.tavall.couriers.api.web.endpoints.AppEndpoint;

public enum CameraFeedEndpoints implements AppEndpoint {
    SCAN_PACKAGE("/internal/api/v1/scan", "POST"),
    CONFIRM_ROUTE("/internal/api/v1/route/confirm", "POST"),
    STREAM_FRAME("/internal/api/v1/stream/frame", "POST");
    public static final String STREAM_FRAME_PATH = "/internal/api/v1/stream/frame";
    private final String endpoint;
    private final String method;


    CameraFeedEndpoints(String endpoint, String method) {

        this.endpoint = endpoint;
        this.method = method;
    }


    @Override
    public String endpoint() {

        return endpoint;
    }


    @Override
    public String method() {

        return method;
    }
}
