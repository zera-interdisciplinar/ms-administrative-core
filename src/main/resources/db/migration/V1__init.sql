CREATE TABLE plan (
    id UUID NOT NULL,
    name VARCHAR(75) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    description TEXT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE organization (
    id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    cnpj CHAR(14) NOT NULL,
    status VARCHAR(15) NOT NULL,
    email VARCHAR(254) NOT NULL,
    plan_id UUID NOT NULL,
    created_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT organization_cnpj_unique UNIQUE (cnpj),
    CONSTRAINT organization_plan_id_foreign FOREIGN KEY (plan_id) REFERENCES plan (id)
);

CREATE TABLE unit (
    id UUID NOT NULL,
    name VARCHAR(50) NOT NULL,
    organization_id UUID NOT NULL,
    created_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT unit_organization_id_foreign FOREIGN KEY (organization_id) REFERENCES organization (id)
);

CREATE TABLE user_account (
    id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(15) NOT NULL,
    password CHAR(60) NOT NULL,
    email VARCHAR(254) NOT NULL,
    status VARCHAR(15) NOT NULL,
    unit_id UUID NOT NULL,
    created_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT user_unit_id_foreign FOREIGN KEY (unit_id) REFERENCES unit (id)
);

CREATE TABLE recycling_business (
    id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    cnpj CHAR(14) NOT NULL,
    contact_email VARCHAR(254) NOT NULL,
    created_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT recycling_business_cnpj_unique UNIQUE (cnpj)
);

CREATE TABLE telephone (
    id UUID NOT NULL,
    number VARCHAR(15) NOT NULL,
    user_id UUID NULL,
    organization_id UUID NULL,
    unit_id UUID NULL,
    recycling_business_id UUID NULL,
    created_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT telephone_user_id_foreign FOREIGN KEY (user_id) REFERENCES user_account (id),
    CONSTRAINT telephone_organization_id_foreign FOREIGN KEY (organization_id) REFERENCES organization (id),
    CONSTRAINT telephone_unit_id_foreign FOREIGN KEY (unit_id) REFERENCES unit (id),
    CONSTRAINT telephone_recycling_business_id_foreign FOREIGN KEY (recycling_business_id) REFERENCES recycling_business (id)
);

CREATE TABLE address (
    id UUID NOT NULL,
    city VARCHAR(50) NOT NULL,
    state VARCHAR(50) NOT NULL,
    neighborhood VARCHAR(100) NULL,
    cep CHAR(8) NOT NULL,
    number VARCHAR(10) NOT NULL,
    complement VARCHAR(30) NULL,
    unit_id UUID NULL,
    recycling_business_id UUID NULL,
    created_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT address_unit_id_foreign FOREIGN KEY (unit_id) REFERENCES unit (id),
    CONSTRAINT address_recycling_business_id_foreign FOREIGN KEY (recycling_business_id) REFERENCES recycling_business (id)
);

CREATE TABLE alert_kind (
    id UUID NOT NULL,
    name VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE alert_priority (
    id UUID NOT NULL,
    name VARCHAR(10) NOT NULL,
    description TEXT NOT NULL,
    level INTEGER NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE alert (
    id UUID NOT NULL,
    reference UUID NULL,
    status VARCHAR(15) NOT NULL,
    unit_id UUID NOT NULL,
    priority_id UUID NOT NULL,
    kind_id UUID NOT NULL,
    created_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT alert_unit_id_foreign FOREIGN KEY (unit_id) REFERENCES unit (id),
    CONSTRAINT alert_priority_id_foreign FOREIGN KEY (priority_id) REFERENCES alert_priority (id),
    CONSTRAINT alert_kind_id_foreign FOREIGN KEY (kind_id) REFERENCES alert_kind (id)
);

CREATE TABLE disposal_report (
    id UUID NOT NULL,
    status VARCHAR(15) NOT NULL,
    url VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    unit_id UUID NOT NULL,
    confirmed_by UUID NULL,
    confirmed_at TIMESTAMP(0) WITHOUT TIME ZONE NULL,
    batch_id UUID NULL,
    created_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT disposal_report_user_id_foreign FOREIGN KEY (user_id) REFERENCES user_account (id),
    CONSTRAINT disposal_report_unit_id_foreign FOREIGN KEY (unit_id) REFERENCES unit (id),
    CONSTRAINT disposal_report_confirmed_by_foreign FOREIGN KEY (confirmed_by) REFERENCES user_account (id)
);