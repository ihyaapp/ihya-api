-- V2__create_identity_and_profile_tables.sql

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id UUID NOT NULL REFERENCES users(id),
                                token_hash VARCHAR(255) NOT NULL UNIQUE,
                                issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                expires_at TIMESTAMPTZ NOT NULL,
                                revoked_at TIMESTAMPTZ
);

CREATE TABLE profiles (
                          user_id UUID PRIMARY KEY REFERENCES users(id),
                          name VARCHAR(255)
);