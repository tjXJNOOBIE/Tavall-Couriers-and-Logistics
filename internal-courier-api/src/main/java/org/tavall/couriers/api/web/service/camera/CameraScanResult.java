package org.tavall.couriers.api.web.service.camera;

import org.tavall.couriers.api.qr.scan.metadata.ScanResponse;

public record CameraScanResult(boolean forbidden, ScanResponse response) {
}
