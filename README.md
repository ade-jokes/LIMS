# Sante Diagnostics LIMS

**Laboratory Information Management System**  
A JavaFX desktop application that digitalises laboratory operations for Sante Diagnostics Ltd — covering test ordering, sample lifecycle tracking, result delivery, and administrative governance across three user roles.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Default Login Credentials](#default-login-credentials)
- [User Role Guide](#user-role-guide)
- [Email Setup](#email-setup)
- [Build & Package](#build--package)

---

## Overview

Sante Diagnostics LIMS replaces a manual, paper-based laboratory workflow with a fully digital system built on three core pillars:

| Pillar | Description |
|---|---|
| **Administrative Governance** | Super Admin manages test types, users, payment verification, and an immutable audit trail |
| **Operational Precision** | Lab Attendants track samples through every lifecycle stage and upload validated results |
| **Customer Transparency** | Patients book tests, track progress via live countdown timers, and download reports from a private dashboard |

---

## Features

### Authentication & Onboarding
- Customer self-registration with email verification (6-digit code via SMTP)
- BCrypt-hashed password storage for all roles
- Staff-created accounts require a forced password change on first login
- Role-based screen routing on login (Super Admin → Admin Dashboard, Lab Attendant → Operational Console, Customer → Patient Hub)

### Super Admin Dashboard
- **Custom Test Builder** — define test types with name, price, turnaround time (TAT), and result format (numeric, text, PDF, image)
- **Test Request Queue** — view all patient requests with payment status; manually confirm payments
- **User Provisioning** — create Lab Attendant and Customer accounts
- **Immutable Audit Trail** — live-searchable log of every system action; database triggers prevent any modification or deletion

### Lab Attendant Console
- **Payment Verification** — view unpaid requests and confirm bank transfer payments
- **Sample Lifecycle Tracking** — move samples through: `Pending Collection → Collected → Processing → Validated`
- **Result Entry & Validation** — attach numeric values, text summaries, PDF reports, or medical images; results require manual validation before becoming visible to the patient
- **Customer Provisioning** — create patient accounts directly from the console

### Patient Hub (Customer)
- **Test Booking** — browse the test catalogue with prices and turnaround times; submit a booking with a bank transfer reference
- **Bank Payment Details** — lab account details displayed immediately after booking (populated from the database)
- **Active Tracking** — live countdown timer per test showing time remaining, or a plain-English status label at each stage
- **Result Vault** — view validated results inline (numeric/text values, medical image preview) and download PDF or image attachments
- **Email Notifications** — automated email when a result is validated and released

---

## Technology Stack

| Component | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| GUI Framework | JavaFX + FXML | 21.0.2 |
| Database | PostgreSQL | 14+ |
| Build Tool | Maven | 3.8+ |
| Password Hashing | jBCrypt | 0.4 |
| Email | Jakarta Mail (SMTP) | 2.0.1 |
| Connection Pooling | HikariCP | 5.1.0 |
| PDF Support | Apache PDFBox | 3.0.2 |

---

## Project Structure

```
sante-lims/
├── src/main/
│   ├── java/com/sante/lims/
│   │   ├── App.java                          # JavaFX Application entry point
│   │   ├── MainApp.java                      # Main launcher class
│   │   ├── controller/
│   │   │   ├── LoginController.java
│   │   │   ├── RegisterController.java
│   │   │   ├── VerifyController.java
│   │   │   ├── PasswordChangeController.java
│   │   │   ├── AdminDashboardController.java
│   │   │   ├── AttendantDashboardController.java
│   │   │   └── CustomerDashboardController.java
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   ├── TestType.java
│   │   │   ├── TestRequest.java
│   │   │   ├── TestResult.java
│   │   │   └── AuditLog.java
│   │   └── utils/
│   │       ├── DBConnection.java             # DB init, migrations, connection
│   │       ├── EmailService.java             # Jakarta Mail SMTP sender
│   │       ├── AuditService.java             # Immutable audit log writer
│   │       └── SecurityUtils.java            # BCrypt helpers
│   └── resources/
│       ├── com/sante/lims/
│       │   ├── login.fxml
│       │   ├── register.fxml
│       │   ├── verify.fxml
│       │   ├── password_change.fxml
│       │   ├── admin_dashboard.fxml
│       │   ├── attendant_dashboard.fxml
│       │   ├── customer_dashboard.fxml
│       │   └── style.css
│       ├── config.properties                 # DB credentials & app config
│       └── email.properties                  # SMTP configuration
├── schema.sql                                # Full database schema (manual setup)
├── pom.xml
└── README.md
```

---

## Prerequisites

Before running the application, ensure the following are installed:

- **Java JDK 17** or higher — [Download](https://adoptium.net/)
- **Maven 3.8+** — [Download](https://maven.apache.org/download.cgi)
- **PostgreSQL 14+** — [Download](https://www.postgresql.org/download/)
- A Gmail account (or any SMTP provider) for outgoing email notifications

---

## Database Setup

### 1. Create the database

Open psql or pgAdmin and run:

```sql
CREATE DATABASE sante_lims;
```

### 2. Fix legacy constraints (if running on an existing DB)

If you have previously run an older version of the schema, drop the old uppercase payment status constraint:

```sql
ALTER TABLE test_requests DROP CONSTRAINT IF EXISTS test_requests_payment_status_check;

ALTER TABLE test_requests
ADD CONSTRAINT test_requests_payment_status_check
CHECK (payment_status IN ('Paid', 'Unpaid'));
```

### 3. Let the application auto-initialise

`DBConnection.initializeDatabase()` runs on every startup and creates all tables, seeds default users, sets up the immutable audit trail triggers, and inserts default bank details automatically. You do not need to run `schema.sql` manually unless setting up a completely fresh database from scratch.

### 4. Update bank account details

After first launch, update the lab's payment account in the database:

```sql
UPDATE bank_details
SET bank_name      = 'Stanbic IBTC Bank',
    account_name   = 'Sante Diagnostics Ltd',
    account_number = '0025129570',
    instructions   = 'Please use your Test Request ID as the payment reference. Send proof of payment to aduraayeni5@gmail.com',
    updated_at     = CURRENT_TIMESTAMP
WHERE id = 1;
```

---

## Configuration

All configuration is in `src/main/resources/config.properties`. No recompilation is needed — the app reads this file at startup.

```properties
# Database
db.url=jdbc:postgresql://localhost:5432/sante_lims
db.username=postgres
db.password=your_password_here

# SMTP Email
mail.smtp.host=smtp.gmail.com
mail.smtp.port=587
mail.smtp.username=yourlab@gmail.com
mail.smtp.password=your_app_password_here
mail.smtp.auth=true
mail.smtp.starttls=true
mail.from=yourlab@gmail.com
mail.from.name=Sante Diagnostics
```

> **Gmail users:** Generate an App Password at [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords) (requires 2FA enabled). Use the 16-character app password — not your Gmail login password.

---

## Running the Application

### From the command line

```bash
# Clone or extract the project
cd sante-lims

# Run via Maven
mvn clean javafx:run
```

### From an IDE (NetBeans / IntelliJ / Eclipse)

1. Open the project as a Maven project
2. Set the main class to `com.sante.lims.MainApp`
3. Ensure JavaFX 21 is on the module path
4. Run `MainApp.java`

### Running the packaged JAR

```bash
mvn clean package
java -jar target/sante-lims-1.0.0.jar
```

---

## Default Login Credentials

These accounts are seeded automatically on first launch if the `users` table is empty.

| Role | Email | Password | Notes |
|---|---|---|---|
| Super Admin | `admin@sante.com` | `admin123` | Full system access |
| Lab Attendant | `attendant@sante.com` | `attendant123` | Forced password change on first login |
| Customer | `customer@sante.com` | `customer123` | Sample patient account |

> **Important:** Change all default passwords immediately after first login in a production environment.

---

## User Role Guide

### Super Admin
1. Log in with admin credentials
2. **Custom Test Builder** tab — define the test catalogue (name, price, TAT, format)
3. **Test Request Queue** tab — select a request and click **Mark as Paid** after verifying a patient's bank transfer
4. **User Provisioning** tab — create Lab Attendant or Customer accounts; they will be forced to change their password on first login
5. **Audit Trail** tab — search and review all system actions; click **↻ Refresh** to see the latest entries

### Lab Attendant
1. Log in (change temporary password if prompted)
2. **Payment Verification** tab — confirm patient payments
3. **Sample Lifecycle Tracking** tab — select a paid request and advance its stage: **Mark Collected → Send to Processing → Validate Sample**
4. **Result Entry & Validation** tab — select a processing request, enter the result (numeric value, text, or file attachment), save as draft or **Validate & Release** to publish to the patient
5. **Customer Provisioning** tab — create patient accounts on behalf of walk-in customers

### Customer (Patient)
1. Self-register at the login screen, or use credentials provided by staff
2. **Book Diagnostics Test** tab — select a test, note the bank details shown, enter your transfer reference, and click **Submit Booking Order**
3. **Active Tracking & Timers** tab — monitor your request status and live countdown
4. **Result Vault** tab — view and download validated reports once released by the lab

---

## Email Setup

The application sends emails for:
- **Email verification** — 6-digit code sent on customer self-registration
- **Result notification** — automatic email when a result is validated and released

Email runs asynchronously on a background thread and will not block the UI. If SMTP credentials are missing or incorrect, the app falls back to displaying the verification code in a dialog (development mode).

To use Gmail SMTP:
1. Enable 2-Step Verification on your Google account
2. Go to [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)
3. Generate an app password for "Mail"
4. Paste the 16-character password into `mail.smtp.password` in `config.properties`

---

## Build & Package

```bash
# Compile only
mvn clean compile

# Run tests (if any)
mvn test

# Package into a fat JAR (includes all dependencies)
mvn clean package

# Output: target/sante-lims-1.0.0.jar
```

The `maven-shade-plugin` is configured to produce a single executable JAR with all dependencies bundled.

---

*Sante Diagnostics Ltd — Administrative Governance · Operational Precision · Customer Transparency*
