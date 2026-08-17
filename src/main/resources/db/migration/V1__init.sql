CREATE TABLE app_metadata (
                              id SERIAL PRIMARY KEY,
                              initialized_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO app_metadata (id) VALUES (1);