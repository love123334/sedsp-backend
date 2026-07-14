CREATE TABLE email_otps
(
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(150) NOT NULL,
    otp         VARCHAR(10)  NOT NULL,
    expiry_time TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_email_otps_email
    ON email_otps (email);

ALTER TABLE email_otps
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE email_otps
    ADD COLUMN resend_count INT NOT NULL DEFAULT 0;