-- V8__create_password_reset_tokens.sql

CREATE TABLE password_reset_tokens (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        user_id UUID NOT NULL REFERENCES users(id),
                                        token_hash VARCHAR(255) NOT NULL UNIQUE,
                                        issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                        expires_at TIMESTAMPTZ NOT NULL,
                                        used_at TIMESTAMPTZ
);
