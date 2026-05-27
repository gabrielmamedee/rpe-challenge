CREATE TABLE tb_users (
    id UUID PRIMARY KEY,
    login VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

CREATE TABLE tb_payment_methods (
    id UUID PRIMARY KEY,
    description VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE tb_orders (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    buyer_name VARCHAR(255) NOT NULL,
    buyer_cpf VARCHAR(14) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    payment_date TIMESTAMP
);

INSERT INTO tb_payment_methods (id, description) VALUES
    (gen_random_uuid(), 'PIX'),
    (gen_random_uuid(), 'CREDITO'),
    (gen_random_uuid(), 'DEBITO');