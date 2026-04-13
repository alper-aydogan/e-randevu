-- Initial Schema for E-Randevu Database Migration

-- Users table to store user data with their roles
CREATE TABLE Users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Appointments table to store appointment details between doctors and patients
CREATE TABLE Appointments (
    id SERIAL PRIMARY KEY,
    doctor_id INT NOT NULL,
    patient_id INT NOT NULL,
    datetime TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (doctor_id) REFERENCES Users(id),
    FOREIGN KEY (patient_id) REFERENCES Users(id)
);

-- Schedules table to store each doctor's available times
CREATE TABLE Schedules (
    id SERIAL PRIMARY KEY,
    doctor_id INT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (doctor_id) REFERENCES Users(id)
);

-- Holidays table to store holidays for each doctor
CREATE TABLE Holidays (
    id SERIAL PRIMARY KEY,
    doctor_id INT NOT NULL,
    date DATE NOT NULL,
    type VARCHAR(20) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (doctor_id) REFERENCES Users(id)
);

-- Indexes Creation
CREATE INDEX idx_appointments_doctor ON Appointments(doctor_id);
CREATE INDEX idx_appointments_patient ON Appointments(patient_id);
CREATE INDEX idx_schedules_doctor ON Schedules(doctor_id);
CREATE INDEX idx_holidays_doctor ON Holidays(doctor_id);