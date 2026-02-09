-- Enums (match your Java enums)
CREATE SCHEMA IF NOT EXISTS courier_schemas;
CREATE TYPE qr_type AS ENUM ('UUID', 'CUSTOM');
CREATE TYPE qr_state AS ENUM ('ACTIVE', 'INACTIVE', 'EXPIRED');
CREATE TYPE delivery_state AS ENUM (
    'LABEL_CREATED', 'DELIVERED', 'IN_TRANSIT', 'IN_HQ',
    'IN_MIDDLEMAN', 'CANCELLED', 'ON_HOLD', 'OUT_FOR_DELIVERY', 'RETRY'
);
CREATE TYPE camera_state AS ENUM ('SEARCHING', 'ANALYZING', 'FOUND', 'ERROR');

-- Tables
CREATE TABLE courier_schemas.qr_metadata (
                             uuid uuid PRIMARY KEY,
                             qr_data text NOT NULL,
                             created_at timestamptz NOT NULL,
                             qr_type qr_type NOT NULL,
                             qr_state qr_state NOT NULL);

CREATE TABLE courier_schemas.shipping_label_metadata (
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

CREATE TABLE courier_schemas.scan_response (
                               uuid varchar(36) PRIMARY KEY,
                               camera_state camera_state NOT NULL,
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

CREATE TABLE courier_schemas.delivery_routes (
                                                 route_id varchar(40) PRIMARY KEY,
                                                 status varchar(30) NOT NULL,
                                                 label_count integer NOT NULL,
                                                 created_at timestamptz NOT NULL,
                                                 updated_at timestamptz,
                                                 notes text,
                                                 assigned_drivers uuid,
                                                 deadline timestamptz,
                                                 route_link varchar(10000)
);

CREATE TABLE courier_schemas.delivery_route_stops (
                                                      id varchar(36) PRIMARY KEY,
                                                      route_id varchar(40) NOT NULL,
                                                      label_uuid varchar(36) NOT NULL,
                                                      stop_order integer NOT NULL,
                                                      created_at timestamptz NOT NULL,
                                                      CONSTRAINT fk_route_stops_route
                                                          FOREIGN KEY (route_id)
                                                              REFERENCES courier_schemas.delivery_routes (route_id)
                                                              ON DELETE CASCADE,
                                                      CONSTRAINT fk_route_stops_label
                                                          FOREIGN KEY (label_uuid)
                                                              REFERENCES courier_schemas.shipping_label_metadata (uuid)
                                                              ON DELETE CASCADE
);

CREATE TABLE courier_schemas.tracking_number_metadata (
                                          tracking_number varchar(64) PRIMARY KEY,
                                          qr_uuid uuid NOT NULL,
                                          delivery_state delivery_state
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

CREATE INDEX idx_delivery_routes_created_at ON courier_schemas.delivery_routes (created_at);
CREATE INDEX idx_delivery_route_stops_route_id ON courier_schemas.delivery_route_stops (route_id);
CREATE INDEX idx_delivery_route_stops_label_uuid ON courier_schemas.delivery_route_stops (label_uuid);

-- User accounts
CREATE TABLE courier_schemas.user_accounts (
                                               id uuid PRIMARY KEY,
                                               external_subject varchar(160) NOT NULL UNIQUE,
                                               username varchar(120) NOT NULL UNIQUE,
                                               enabled boolean NOT NULL,
                                               created_at timestamptz NOT NULL
);

CREATE TABLE courier_schemas.user_account_roles (
                                                   user_id uuid NOT NULL,
                                                   role varchar(40) NOT NULL,
                                                   PRIMARY KEY (user_id, role),
                                                   CONSTRAINT fk_user_account_roles_user
                                                       FOREIGN KEY (user_id)
                                                           REFERENCES courier_schemas.user_accounts (id)
                                                           ON DELETE CASCADE
);

CREATE INDEX idx_user_accounts_username ON courier_schemas.user_accounts (username);