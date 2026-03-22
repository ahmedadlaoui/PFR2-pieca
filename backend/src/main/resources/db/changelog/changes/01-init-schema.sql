-- liquibase formatted sql

-- changeset pieca:01-init-schema
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE seller_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    seller_type VARCHAR(50) NOT NULL,
    location geometry(Point, 4326),
    active_radius_km INT NOT NULL DEFAULT 5,
    custom_category_note VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_seller_profile_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE seller_profile_categories (
    seller_profile_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (seller_profile_id, category_id),
    CONSTRAINT fk_spc_seller_profile FOREIGN KEY (seller_profile_id) REFERENCES seller_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_spc_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE
);

CREATE TABLE requests (
    id BIGSERIAL PRIMARY KEY,
    buyer_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    description VARCHAR(1000) NOT NULL,
    image_url VARCHAR(255),
    location geometry(Point, 4326),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_request_user FOREIGN KEY (buyer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_request_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE
);

CREATE TABLE offers (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    price NUMERIC NOT NULL,
    proof_image_url VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_offer_request FOREIGN KEY (request_id) REFERENCES requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_offer_seller FOREIGN KEY (seller_id) REFERENCES users (id) ON DELETE CASCADE
);
