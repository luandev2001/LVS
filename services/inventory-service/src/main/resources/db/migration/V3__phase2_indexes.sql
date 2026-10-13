-- PHASE 2 OPTIMIZATION: run this migration only when comparing the optimized variant.
-- The baseline endpoint remains intentionally available for before/after comparison.
CREATE INDEX idx_inventory_warehouse_id ON inventory (warehouse_code, id DESC);
CREATE INDEX idx_inventory_product_id ON inventory (product_id);
