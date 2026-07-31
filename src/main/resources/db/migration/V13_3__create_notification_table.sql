CREATE TABLE notification
(
    id         BIGSERIAL PRIMARY KEY,

    user_id    BIGINT                    NOT NULL,

    tp_type    notification_type_enum    NOT NULL DEFAULT 'INFO',
    tp_status  notification_status_enum  NOT NULL DEFAULT 'UNREAD',

    ds_title   VARCHAR(255)              NOT NULL,
    ds_message TEXT                      NOT NULL,
    dt_read    TIMESTAMP,

    company_id BIGINT,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP                 NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_notification_company_id FOREIGN KEY (company_id) REFERENCES company (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_notification_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_notification_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE INDEX idx_notification_user_id ON notification (user_id);
