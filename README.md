# Finance Pro - Personal Finance Management System

Finance Pro is a modern web application designed to help users track their income, expenses, and manage multiple wallets effectively.

## 🚀 Features

- **Wallet Management**: Create and track multiple bank accounts or cash wallets.
- **Transaction Tracking**: Log daily income and expenses with categories and descriptions.
- **Budgeting**: Set daily spending limits and monthly category budgets with real-time alerts.
- **Spending Diary**: View historical transaction reports and budget status for any date.
- **Dynamic Dashboard**: Visual overview of financial health and recent activities.

## 🛠 Technology Stack

- **Backend**: Java 17, Spring Boot 3.x
- **Persistence**: Spring Data JPA, Hibernate, MySQL
- **Security**: Spring Security (Role-based access control)
- **Frontend**: Thymeleaf, Vanilla CSS, JavaScript
- **Build Tool**: Gradle

## ⚙️ Setup & Installation

### Prerequisites
- Java 17 or higher
- MySQL 8.0 or higher
- Gradle (optional, use `gradlew` wrapper)

### Database Setup
1. Create a MySQL database named `finance_tracker`.
2. Configure your credentials in `src/main/resources/application.properties`. You can refer to `application.properties.example`.

### Running the Application
1. Clone the repository.
2. Open the terminal in the project root.
3. Run the following command:
   ```bash
   ./gradlew bootRun
   ```
4. Access the application at `http://localhost:8080`.

## 🧪 Testing
To run the automated tests:
```bash
./gradlew test
```

## 🤝 Contribution
1. Create a feature branch (`git checkout -b feature/amazing-feature`).
2. Commit your changes (`git commit -m 'feat: add amazing feature'`).
3. Push to the branch (`git push origin feature/amazing-feature`).
4. Open a Pull Request.
