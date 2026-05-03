CREATE TABLE otp_codes (
                           id UUID PRIMARY KEY,
                           user_id UUID NOT NULL,
                           operation_id UUID NOT NULL,
                           code_hash VARCHAR(255) NOT NULL,
                           status VARCHAR(20) NOT NULL,
                           expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                           created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
                           used_at TIMESTAMP WITH TIME ZONE,

                           CONSTRAINT otp_codes_user_id_fk
                               FOREIGN KEY (user_id)
                                   REFERENCES users(id)
                                   ON DELETE CASCADE,

                           CONSTRAINT otp_codes_operation_id_fk
                               FOREIGN KEY (operation_id)
                                   REFERENCES operations(id)
                                   ON DELETE CASCADE,

                           CONSTRAINT otp_codes_status_check
                               CHECK (status IN ('ACTIVE', 'EXPIRED', 'USED'))
);