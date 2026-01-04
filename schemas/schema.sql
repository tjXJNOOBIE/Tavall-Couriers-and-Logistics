-- QR Code table: stores immutable QR identifiers only
CREATE TABLE qr_codes (
    id VARCHAR(36) PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    qr_image_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

-- QR Payload table: stores business data associated with QR ID
CREATE TABLE qr_payloads (
    id BIGSERIAL PRIMARY KEY,
    qr_id VARCHAR(36) NOT NULL UNIQUE REFERENCES qr_codes(id),
    recipient_name VARCHAR(200) NOT NULL,
    recipient_address TEXT NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    delivery_notes TEXT,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL
);

-- Route table: groups multiple QR codes together
CREATE TABLE routes (
    id VARCHAR(36) PRIMARY KEY,
    route_name VARCHAR(200) NOT NULL,
    route_status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    assigned_driver VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked BOOLEAN NOT NULL DEFAULT FALSE
);

-- Route-QR mapping table: many-to-many relationship
CREATE TABLE route_qr_mappings (
    id BIGSERIAL PRIMARY KEY,
    route_id VARCHAR(36) NOT NULL REFERENCES routes(id),
    qr_id VARCHAR(36) NOT NULL REFERENCES qr_codes(id),
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by VARCHAR(100) NOT NULL,
    UNIQUE(route_id, qr_id)
);

-- Scan logs: audit trail of all QR scans
CREATE TABLE scan_logs (
    id BIGSERIAL PRIMARY KEY,
    qr_id VARCHAR(36) NOT NULL REFERENCES qr_codes(id),
    scanned_by VARCHAR(100) NOT NULL,
    scanned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    action_type VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    metadata JSONB
);

-- Users table: basic authentication
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    full_name VARCHAR(200),
    email VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_qr_payloads_qr_id ON qr_payloads(qr_id);
CREATE INDEX idx_qr_payloads_status ON qr_payloads(delivery_status);
CREATE INDEX idx_routes_status ON routes(route_status);
CREATE INDEX idx_route_qr_route_id ON route_qr_mappings(route_id);
CREATE INDEX idx_route_qr_qr_id ON route_qr_mappings(qr_id);
CREATE INDEX idx_scan_logs_qr_id ON scan_logs(qr_id);
CREATE INDEX idx_scan_logs_scanned_at ON scan_logs(scanned_at);
CREATE INDEX idx_users_username ON users(username);
