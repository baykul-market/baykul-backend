CREATE TABLE part_sources (
    id uuid PRIMARY KEY,
    name varchar(255) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('ACTIVE', 'HIDDEN', 'ARCHIVED')),
    system_source boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    created_ts timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_ts timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO part_sources (id, name, status, system_source) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Каталог до миграции', 'ACTIVE', true),
    ('00000000-0000-0000-0000-000000000002', 'Ручное добавление', 'ACTIVE', true);
ALTER TABLE parts ADD COLUMN source_id uuid;
ALTER TABLE parts ADD COLUMN catalog_present boolean NOT NULL DEFAULT true;
UPDATE parts SET source_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE parts ALTER COLUMN source_id SET NOT NULL;
ALTER TABLE parts ADD CONSTRAINT parts_source_fk FOREIGN KEY (source_id) REFERENCES part_sources(id);
ALTER TABLE parts ADD CONSTRAINT parts_source_article_brand_key UNIQUE (source_id, article, brand);
ALTER TABLE parts DROP CONSTRAINT parts_article_key;
CREATE INDEX parts_article_idx ON parts(article);
CREATE INDEX parts_source_present_idx ON parts(source_id, catalog_present);

CREATE TABLE part_imports (
    id uuid PRIMARY KEY,
    source_id uuid NOT NULL REFERENCES part_sources(id),
    filename varchar(255) NOT NULL,
    uploaded_by varchar(255) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN
        ('QUEUED', 'PROCESSING', 'READY', 'APPLYING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    source_version bigint NOT NULL,
    total_rows bigint NOT NULL DEFAULT 0,
    valid_rows bigint NOT NULL DEFAULT 0,
    skipped bigint NOT NULL DEFAULT 0,
    duplicates bigint NOT NULL DEFAULT 0,
    added bigint NOT NULL DEFAULT 0,
    updated bigint NOT NULL DEFAULT 0,
    removed bigint NOT NULL DEFAULT 0,
    accept_skipped_rows boolean NOT NULL DEFAULT false,
    error_message varchar(1000),
    created_ts timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_ts timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX part_imports_one_pending ON part_imports(source_id)
    WHERE status IN ('QUEUED', 'PROCESSING', 'READY', 'APPLYING');
CREATE INDEX part_imports_history_idx ON part_imports(source_id, created_ts DESC);
CREATE TABLE part_import_rows (
    import_id uuid NOT NULL REFERENCES part_imports(id),
    article varchar(50) NOT NULL,
    brand varchar(50) NOT NULL,
    row_number bigint NOT NULL,
    name varchar(255),
    weight double precision,
    min_count integer NOT NULL,
    storage_count integer,
    return_part numeric(38,2) NOT NULL,
    price numeric(38,2) NOT NULL,
    PRIMARY KEY (import_id, article, brand)
);
CREATE TABLE part_import_errors (
    import_id uuid NOT NULL REFERENCES part_imports(id),
    row_number bigint NOT NULL,
    error_message varchar(1000) NOT NULL,
    raw_data varchar(2000) NOT NULL,
    PRIMARY KEY (import_id, row_number)
);
