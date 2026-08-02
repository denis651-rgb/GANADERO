create table core.idempotency_records (
    subject varchar(160) not null,
    idempotency_key varchar(200) not null,
    http_method varchar(10) not null,
    request_path varchar(500) not null,
    state varchar(20) not null default 'PROCESSING',
    response_status integer,
    response_content_type varchar(200),
    response_body bytea,
    created_at timestamptz not null default now(),
    completed_at timestamptz,
    primary key (subject, idempotency_key, http_method, request_path),
    constraint ck_idempotency_state check (state in ('PROCESSING', 'COMPLETED'))
);

create index idx_idempotency_created_at on core.idempotency_records(created_at);
