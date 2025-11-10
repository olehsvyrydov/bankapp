CREATE SCHEMA IF NOT EXISTS notifications;

CREATE TABLE notifications.notifications (
     id BIGSERIAL PRIMARY KEY,
     username VARCHAR(255) NOT NULL,
     message TEXT NOT NULL,
     type VARCHAR(20) NOT NULL DEFAULT 'INFO',
     read BOOLEAN NOT NULL DEFAULT false,
     created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_notifications_username ON notifications.notifications(username);
CREATE INDEX idx_notifications_created_at ON notifications.notifications(created_at);
