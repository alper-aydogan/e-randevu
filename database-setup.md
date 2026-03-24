# PostgreSQL Kurulum ve Veritabanı Doldurma Rehberi

## 🍺 macOS için PostgreSQL Kurulum

### 1. Homebrew ile Kurulum
```bash
brew install postgresql@15
brew services start postgresql@15
```

### 2. PostgreSQL Başlatma
```bash
brew services start postgresql@15
brew services list | grep postgres
```

### 3. Database ve User Oluşturma
```bash
# PostgreSQL'e bağlanma
psql postgres

# Database oluşturma
CREATE DATABASE e_randevu;

# User oluşturma
CREATE USER e_randevu_user WITH PASSWORD 'password123';

# Yetki verme
GRANT ALL PRIVILEGES ON DATABASE e_randevu TO e_randevu_user;

# Çıkış
\q
```

### 4. Application.properties Güncelleme
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/e_randevu
spring.datasource.username=e_randevu_user
spring.datasource.password=password123
spring.datasource.driver-class-name=org.postgresql.Driver
```

## 🐳 Docker ile PostgreSQL (Alternatif)

### Docker ile PostgreSQL Başlatma
```bash
docker run --name postgres-e-randevu \
  -e POSTGRES_DB=e_randevu \
  -e POSTGRES_USER=e_randevu_user \
  -e POSTGRES_PASSWORD=password123 \
  -p 5432:5432 \
  -d postgres:15
```

## 📊 Manuel Veri Ekleme SQL Scriptleri

### Users Tablosu için örnek veriler
```sql
-- Admin User
INSERT INTO users (username, password, email, first_name, last_name, phone_number, role, enabled, created_at, updated_at)
VALUES (
  'admin', 
  '$2a$10$...', -- şifrelenmiş admin123
  'admin@erandevu.com', 
  'Admin', 
  'User', 
  '+9055550101', 
  'ADMIN', 
  true, 
  NOW(), 
  NOW()
);

-- Doctor Users
INSERT INTO users (username, password, email, first_name, last_name, phone_number, role, enabled, created_at, updated_at)
VALUES 
  ('dr_1', '$2a$10$...', 'dr1@erandevu.com', 'Doctor', '1', '+9055550102', 'DOCTOR', true, NOW(), NOW()),
  ('dr_2', '$2a$10$...', 'dr2@erandevu.com', 'Doctor', '2', '+9055550103', 'DOCTOR', true, NOW(), NOW()),
  ('dr_3', '$2a$10$...', 'dr3@erandevu.com', 'Doctor', '3', '+9055550104', 'DOCTOR', true, NOW(), NOW());

-- Patient Users
INSERT INTO users (username, password, email, first_name, last_name, phone_number, role, enabled, created_at, updated_at)
VALUES 
  ('patient_1', '$2a$10$...', 'patient1@erandevu.com', 'Patient', '1', '+9055550201', 'PATIENT', true, NOW(), NOW()),
  ('patient_2', '$2a$10$...', 'patient2@erandevu.com', 'Patient', '2', '+9055550202', 'PATIENT', true, NOW(), NOW());
```

### Schedules Tablosu için örnek veriler
```sql
INSERT INTO schedules (doctor_id, day_of_week, start_time, end_time, appointment_duration_minutes, active, created_at, updated_at)
VALUES 
  (1, 'MONDAY', '09:00:00', '17:00:00', 30, true, NOW(), NOW()),
  (1, 'TUESDAY', '09:00:00', '17:00:00', 30, true, NOW(), NOW()),
  (2, 'MONDAY', '10:00:00', '18:00:00', 30, true, NOW(), NOW());
```

## 🔧 Bağlantı Test Etme
```bash
# Uygulama çalıştıktan sonra
curl http://localhost:8080/api/users

# Swagger UI
http://localhost:8080/swagger-ui.html
```

## 📝 Notlar
- PostgreSQL port: 5432
- Default user: postgres
- Yeni user: e_randevu_user
- Database: e_randevu
- Password: password123
