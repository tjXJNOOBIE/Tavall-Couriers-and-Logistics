-- Enums (match your Java enums)
CREATE TYPE qr_type AS ENUM ('UUID', 'CUSTOM');
CREATE TYPE qr_state AS ENUM ('ACTIVE', 'INACTIVE', 'EXPIRED');
CREATE TYPE delivery_state AS ENUM (
    'LABEL_CREATED', 'DELIVERED', 'IN_TRANSIT', 'IN_HQ',
    'IN_MIDDLEMAN', 'CANCELLED', 'ON_HOLD', 'OUT_FOR_DELIVERY', 'RETRY'
);
CREATE TYPE live_camera_state AS ENUM ('IDLE', 'SCANNING', 'ANALYZING', 'FOUND', 'ERROR');

-- Tables
CREATE TABLE qr_metadata (
                             uuid uuid PRIMARY KEY,
                             qr_data text NOT NULL,
                             created_at timestamptz NOT NULL,
                             qr_type qr_type NOT NULL,
                             qr_state qr_state NOT NULL);

CREATE TABLE shipping_label_metadata (
                                         uuid varchar(36) PRIMARY KEY,
                                         tracking_number varchar(64) NOT NULL,
                                         recipient_name varchar(160) NOT NULL,
                                         phone_number varchar(30),
                                         address text NOT NULL,
                                         city varchar(120) NOT NULL,
                                         state varchar(120) NOT NULL,
                                         zip_code varchar(20) NOT NULL,
                                         country varchar(120) NOT NULL,
                                         priority boolean NOT NULL,
                                         deliver_by timestamptz,
                                         delivery_state delivery_state
);

CREATE TABLE scan_response (
                               uuid varchar(36) PRIMARY KEY,
                               camera_state live_camera_state NOT NULL,
                               tracking_number varchar(64),
                               name varchar(160),
                               address text,
                               city varchar(120),
                               state varchar(120),
                               zip_code varchar(20),
                               country varchar(120),
                               phone_number varchar(30),
                               deadline timestamptz,
                               notes text
);

CREATE TABLE tracking_number_metadata (
                                          tracking_number varchar(64) PRIMARY KEY,
                                          qr_uuid uuid NOT NULL
);

-- Indexes
CREATE INDEX idx_qr_metadata_state ON qr_metadata (qr_state);
CREATE INDEX idx_qr_metadata_type ON qr_metadata (qr_type);

CREATE INDEX idx_shipping_label_tracking_number ON shipping_label_metadata (tracking_number);
CREATE INDEX idx_shipping_label_delivery_state ON shipping_label_metadata (delivery_state);
CREATE INDEX idx_shipping_label_priority ON shipping_label_metadata (priority);

CREATE INDEX idx_scan_response_tracking_number ON scan_response (tracking_number);
CREATE INDEX idx_scan_response_camera_state ON scan_response (camera_state);

CREATE INDEX idx_tracking_number_metadata_qr_uuid ON tracking_number_metadata (qr_uuid);