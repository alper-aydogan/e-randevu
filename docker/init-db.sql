-- PostgreSQL Initialization Script for E-Randevu
-- This script runs when the PostgreSQL container starts for the first time

-- Set timezone to UTC
SET timezone = 'UTC';

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant permissions to the user
GRANT ALL PRIVILEGES ON DATABASE erandevu TO alper;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO alper;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO alper;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO alper;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO alper;

-- Output success message
DO $$
BEGIN
    RAISE NOTICE 'E-Randevu database initialized successfully';
END;
$$;
