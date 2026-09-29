-- V9__add_image_url_to_user_account.sql

-- url da imagem de perfil do usuario; nullable porque usuarios ja existentes nao tem esse dado
ALTER TABLE user_account ADD COLUMN image_url VARCHAR(2048);
