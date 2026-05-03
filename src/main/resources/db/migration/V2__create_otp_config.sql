CREATE TABLE otp_config (
                            id SMALLINT PRIMARY KEY,
                            code_length INTEGER NOT NULL,
                            ttl_seconds INTEGER NOT NULL,
                            updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

                            CONSTRAINT otp_config_single_row_check CHECK (id = 1),
                            CONSTRAINT otp_config_code_length_check CHECK (code_length BETWEEN 4 AND 10),
                            CONSTRAINT otp_config_ttl_seconds_check CHECK (ttl_seconds BETWEEN 30 AND 86400)
);