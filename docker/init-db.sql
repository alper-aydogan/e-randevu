-- E-Randevu Database Initialization Script for Docker
-- This script creates the initial database structure and sample data

-- Create extensions if they don't exist
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Insert sample roles (if not handled by JPA)
INSERT INTO roles (id, name) VALUES 
(1, 'ROLE_ADMIN'),
(2, 'ROLE_DOCTOR'),
(3, 'ROLE_PATIENT')
ON CONFLICT DO NOTHING;

-- Create sample admin user
INSERT INTO users (id, username, email, password, first_name, last_name, phone_number, role, enabled, created_at, updated_at) VALUES 
(1, 'admin', 'admin@erandevu.com', '$2a$10$GxFwqS2VlKqGdLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLq', 'Admin', 'User', '+9055551234', 'ROLE_ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'dr_1', 'dr1@erandevu.com', '$2a$10$GxFwqS2VlKqGdLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLq', 'John', 'Smith', '+9055551235', 'ROLE_DOCTOR', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'dr_2', 'dr2@erandevu.com', '$2a$10$GxFwqS2VlKqGdLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLq', 'Jane', 'Johnson', '+9055551236', 'ROLE_DOCTOR', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'dr_3', 'dr3@erandevu.com', '$2a$10$GxFwqS2VlKqGdLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLq', 'Michael', 'Brown', '+9055551237', 'ROLE_DOCTOR', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'patient_1', 'patient1@erandevu.com', '$2a$10$GxFwqS2VlKqGdLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLq', 'Alice', 'Wilson', '+9055552234', 'ROLE_PATIENT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'patient_2', 'patient2@erandevu.com', '$2a$10$GxFwqS2VlKqGdLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLq', 'Bob', 'Taylor', '+9055552235', 'ROLE_PATIENT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'patient_3', 'patient3@erandevu.com', '$2a$10$GxFwqS2VlKqGdLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLq', 'Charlie', 'Davis', '+9055552236', 'ROLE_PATIENT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'patient_4', 'patient4@erandevu.com', '$2a$10$GxFwqS2VlKqGdLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLq', 'Diana', 'Miller', '+9055552237', 'ROLE_PATIENT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 'patient_5', 'patient5@erandevu.com', '$2a$10$GxFwqS2VlKqGdLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLqLq', 'Eva', 'Garcia', '+9055552238', 'ROLE_PATIENT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Create sample appointments (for testing)
INSERT INTO appointments (id, doctor_id, patient_id, appointment_date_time, notes, status, created_at, updated_at) VALUES 
(1, 2, 5, CURRENT_TIMESTAMP + INTERVAL '1 day', 'Regular checkup', 'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 3, 5, CURRENT_TIMESTAMP + INTERVAL '2 days', 'Follow-up appointment', 'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 4, 5, CURRENT_TIMESTAMP + INTERVAL '3 days', 'Initial consultation', 'SCHEDULED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor_id ON appointments(doctor_id);
CREATE INDEX IF NOT EXISTS idx_appointments_patient_id ON appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_date_time ON appointments(appointment_date_time);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments(status);

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO erandevu;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO erandevu;

-- Output success message
DO $$
BEGIN
    RAISE NOTICE 'E-Randevu database initialized successfully';
END;
$$;
