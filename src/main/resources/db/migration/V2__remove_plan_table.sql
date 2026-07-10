ALTER TABLE organization DROP CONSTRAINT organization_plan_id_foreign;
ALTER TABLE organization DROP COLUMN plan_id;
ALTER TABLE organization ADD COLUMN plan VARCHAR(20) NOT NULL;
DROP TABLE plan;