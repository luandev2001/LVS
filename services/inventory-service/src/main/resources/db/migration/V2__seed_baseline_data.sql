INSERT INTO product (sku, name, category_code)
SELECT
    'SKU-' || LPAD(g::text, 8, '0'),
    'Demo Product ' || g,
    'CAT-' || ((g % 20) + 1)
FROM generate_series(1, 10000) AS g;

INSERT INTO inventory (product_id, warehouse_code, quantity, reserved_quantity)
SELECT
    p.id,
    'WH-' || ((p.id % 10) + 1),
    100 + (p.id % 900),
    p.id % 20
FROM product p
CROSS JOIN generate_series(1, 5) AS x;
