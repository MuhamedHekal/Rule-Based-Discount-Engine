# create this table in postgres db 
CREATE TABLE order_discounts (
    order_id SERIAL PRIMARY KEY,
    order_timestamp TIMESTAMP NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    expiry_date DATE NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    average_discount DECIMAL(5,2) NOT NULL,
    TotalPrice DECIMAL(10,2) NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

);

