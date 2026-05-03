CREATE TABLE operations (
                            id UUID PRIMARY KEY,
                            user_id UUID NOT NULL,
                            operation_id VARCHAR(150) NOT NULL,
                            description TEXT,
                            created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

                            CONSTRAINT operations_user_id_fk
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE CASCADE,

                            CONSTRAINT operations_user_operation_unique
                                UNIQUE (user_id, operation_id)
);