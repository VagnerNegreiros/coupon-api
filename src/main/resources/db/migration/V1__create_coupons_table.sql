CREATE TABLE coupons (
    id              UUID                     NOT NULL PRIMARY KEY,
    code            VARCHAR(6)               NOT NULL,
    description     VARCHAR                  NOT NULL,
    discount_value  DECFLOAT                 NOT NULL,
    expiration_date TIMESTAMP WITH TIME ZONE NOT NULL,
    status          VARCHAR(20)              NOT NULL,
    published       BOOLEAN                  NOT NULL,
    redeemed        BOOLEAN                  NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    version         BIGINT                   NOT NULL,
    CONSTRAINT ck_coupons_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    CONSTRAINT ck_coupons_discount CHECK (discount_value >= 0.5)
);
