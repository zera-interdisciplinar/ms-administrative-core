-- V8__add_invitee_name_to_invitation.sql

-- nome de quem o gestor esta convidando; nullable porque convites ja existentes nao tem esse dado
ALTER TABLE invitation ADD COLUMN invitee_name VARCHAR(255);
