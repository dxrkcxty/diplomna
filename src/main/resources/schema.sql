CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(50),
    password_reset_code VARCHAR(20),
    password_reset_expires VARCHAR(50),
    bonus_balance DECIMAL(10, 2) NOT NULL DEFAULT 0,
    bonus_rate INTEGER,
    spin_credits INTEGER NOT NULL DEFAULT 0
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_code VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_expires VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS bonus_balance DECIMAL(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS bonus_rate INTEGER;
ALTER TABLE users ADD COLUMN IF NOT EXISTS spin_credits INTEGER NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    category_id BIGINT REFERENCES categories(id),
    type VARCHAR(100),
    size VARCHAR(50),
    gender VARCHAR(50),
    image_url TEXT,
    discount_percent DECIMAL(5, 2),
    discount_amount DECIMAL(10, 2)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    date VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'New',
    full_name VARCHAR(255),
    phone VARCHAR(50),
    delivery_address TEXT,
    delivery_carrier VARCHAR(100),
    delivery_branch VARCHAR(255),
    payment_method VARCHAR(50),
    payment_status VARCHAR(50),
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    bonus_used DECIMAL(10, 2) NOT NULL DEFAULT 0,
    bonus_earned DECIMAL(10, 2) NOT NULL DEFAULT 0,
    bonus_rate INTEGER
);

ALTER TABLE orders ADD COLUMN IF NOT EXISTS full_name VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS phone VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_address TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_carrier VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_branch VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_status VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS bonus_used DECIMAL(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS bonus_earned DECIMAL(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS bonus_rate INTEGER;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS items_snapshot TEXT;

CREATE TABLE IF NOT EXISTS order_products (
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY (order_id, product_id)
);

ALTER TABLE order_products ADD COLUMN IF NOT EXISTS quantity INTEGER NOT NULL DEFAULT 1;

CREATE TABLE IF NOT EXISTS order_messages (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    sender_email VARCHAR(255) NOT NULL,
    sender_role VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    created_at VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    product_id BIGINT REFERENCES products(id),
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT
);

CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_orders_user ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_order_products_order ON order_products(order_id);
CREATE INDEX IF NOT EXISTS idx_order_products_product ON order_products(product_id);
CREATE INDEX IF NOT EXISTS idx_order_messages_order ON order_messages(order_id);
CREATE INDEX IF NOT EXISTS idx_reviews_user ON reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_product ON reviews(product_id);

