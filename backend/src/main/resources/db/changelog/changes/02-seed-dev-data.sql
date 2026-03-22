-- liquibase formatted sql

-- changeset pieca:02-seed-dev-data
INSERT INTO categories (name, is_active) VALUES ('Électronique', true) ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (name, is_active) VALUES ('Pièces Auto', true) ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (name, is_active) VALUES ('Vêtements', true) ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (name, is_active) VALUES ('Matériaux de Construction', true) ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (name, is_active) VALUES ('Mobilier', true) ON CONFLICT (name) DO NOTHING;
INSERT INTO categories (name, is_active) VALUES ('Outillage', true) ON CONFLICT (name) DO NOTHING;
