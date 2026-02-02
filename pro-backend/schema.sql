-- Licenses table to store active subscriptions
CREATE TABLE IF NOT EXISTS licenses (
    user_id TEXT PRIMARY KEY,
    serial_key TEXT,
    expires_at INTEGER,
    status TEXT DEFAULT 'active'
);

-- Serial keys table for validation
CREATE TABLE IF NOT EXISTS serial_keys (
    key TEXT PRIMARY KEY,
    status TEXT DEFAULT 'available', -- 'available', 'used'
    used_by TEXT
);

-- Table to map hashed emails to user IDs for subscription gating
CREATE TABLE IF NOT EXISTS email_mapping (
    email_hash TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    webhook_secret TEXT,
    updated_at INTEGER
);
