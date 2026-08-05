CREATE TABLE email_otps
(
    id           BIGSERIAL PRIMARY KEY,
    email        VARCHAR(150) NOT NULL,
    otp          VARCHAR(10)  NOT NULL,
    expiry_time  TIMESTAMPTZ  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resend_count INT          NOT NULL DEFAULT 0,
    verified     BOOLEAN      NOT NULL DEFAULT FALSE,
    used         BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_email_otps_email
    ON email_otps (email);

CREATE INDEX idx_email_otps_expiry
    ON email_otps (expiry_time);

CREATE INDEX idx_email_otps_email_used
    ON email_otps (email, used);

CREATE INDEX idx_email_otps_email_valid
    ON email_otps (email, expiry_time DESC) WHERE used = FALSE;