CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       login VARCHAR(100) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL,
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

                       CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'USER'))
);