-- V4__add_manager_id_to_user_account.sql

ALTER TABLE user_account ADD COLUMN manager_id UUID NULL;
ALTER TABLE user_account ADD CONSTRAINT user_manager_id_foreign FOREIGN KEY (manager_id) REFERENCES user_account (id);

CREATE INDEX idx_user_account_manager_id ON user_account (manager_id);
