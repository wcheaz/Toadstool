# Toadstool

Team Toadstool's final project for the LEAP period!


# Team Members

- Winston Cheaz
- Upasana Patel
- Triem Le
- Abhinav Mayreddy
- Majoie Ngandi

# Git Branching Strategy

Due to a lack of need for CD for this project, we have decided that a git-flow style branching makes the most sense for this project. We are likely to be working on specific features at some point during the dev lifecycle, and using feature branches would help us stay orgainized. 

# Definition of Done

## Development
- Code is committed to source control.
- Code follows team coding standards.
- No critical or high-severity static code analysis findings.
- At least 2 Peer reviews completed and approved per merge. 

## Testing
- Unit tests created and passing.
- Minimum agreed test coverage achieved (e.g, 80%+).
- Integration tests added applicable.
- All acceptance criteria pass.

## Documentation
- README is updated as features are implemented.
- Architecture diagrams updated if impacted.
- User documentation updated if UI changes. 

## DevOps
- Deployed successfully to test environment. 
- Build pipeline passes successfully. 
- No deployment blockers identified. 

## Security
- No exposed credentials/secrets. 
- Authentication and authorization validated. 
- Security scan passes.
- 0 Critical vulnerabilities, 3 Moderate vulnerabilities, 10 Low vulnerabilities.

## Product
- Product Owner accepts story. 
- Story demonstrates expeted business outcome. 

# Setup/Initialization

## Environment Configuration

Before running the application or tests, you need to configure environment variables for database connectivity.

### Creating the `.env` File

1. Create a `.env` file in the project root (same directory as `pom.xml`) by copying the example:
   ```bash
   cp .env.example .env
   ```

2. Edit the `.env` file with your database connection details:
   ```env
   DB_HOST=<your-db-host>
   DB_PORT=5432
   DB_NAME=<your-db-name>
   DB_USERNAME=<your-db-username>
   DB_PASSWORD=<your-db-password>
   FAUXNANCE_BASE_URL=<your-fauxnance-url>
   FAUXNANCE_API_KEY=<your-api-key>
   ```

**Important:** The `.env` file is git-ignored and contains sensitive credentials. Never commit it to version control.

### How Configuration Works

- Spring Boot automatically loads environment variables from the `.env` file via `spring.config.import` in `application.yml`
- All `application.yml` files are now committed to git (they contain no secrets, only variable references)
- Credentials come **exclusively** from your `.env` file, not from any checked-in configuration

### Application Configuration Files

The following Spring Boot configuration files define the application structure using environment variables:

- **`src/main/resources/application.yml`** - Production/main application configuration
  - Uses environment variables: `${DB_HOST}`, `${DB_PORT}`, `${DB_NAME}`, `${DB_USERNAME}`, `${DB_PASSWORD}`
  - Contains Flyway migration settings and MyBatis mapper configuration
  - Requires all database variables to be set in `.env`

- **`src/test/resources/application.yml`** - Test application configuration
  - Uses environment variables with test-safe defaults
  - Enables Flyway automatic migration and validation for tests

### Variable Reference

| Variable | Purpose | Example |
|----------|---------|---------|
| `DB_HOST` | PostgreSQL server hostname or IP | `localhost` or `<your-db-host>` |
| `DB_PORT` | PostgreSQL server port | `5432` |
| `DB_NAME` | Database name | `toadstool_db` |
| `DB_USERNAME` | Database username | `toadstool_user` |
| `DB_PASSWORD` | Database password | Your secure password |
| `FAUXNANCE_BASE_URL` | Fauxnance API base URL | `https://y4t9nq2bqf.execute-api.eu-west-2.amazonaws.com/v1` |
| `FAUXNANCE_API_KEY` | Fauxnance API key | `fnx_dev_...` |
| `FAUXNANCE_TIMEOUT_SECONDS` | API timeout | `10` |

# Testing

## Running Tests with SSH Tunnel

The test suite includes integration tests that connect to the PostgreSQL database. To run tests against the VM database, you must establish an SSH tunnel.

### Prerequisites

- SSH access to the VM: `ec2-user@10.18.73.92`
- SSH credentials configured (username and password)
- `.env` file configured (see Setup/Initialization section)
- Two terminal windows (one for tunnel, one for tests)

### Step 1: Establish SSH Tunnel (Terminal 1)

Open a PowerShell window and create a local port forward to the VM's PostgreSQL:

```powershell
ssh -L 5432:localhost:5432 ec2-user@10.18.73.92
```

When prompted, enter your SSH password.

**Important:** Keep this terminal window open while running tests. The SSH tunnel must remain active for tests to connect to the database. Do not close this window until tests are complete.

The tunnel output will show:
```
Enter passphrase for key:
The authenticity of host '10.18.73.92' can't be established...
```

Once authenticated, you'll see no further output (this is normal for SSH tunnels).

### Step 2: Run Tests (Terminal 2)

In a separate PowerShell window, run Maven tests:

```powershell
mvn test
```

Or run specific test classes:

```powershell
mvn test -Dtest=AccountMapperTest
```

### What the Tunnel Does

The `-L 5432:localhost:5432` flag creates a **local port forward**:
- Local connections to `127.0.0.1:5432` are forwarded through SSH to the VM
- The VM's PostgreSQL (inside `toadstool-db` container) becomes accessible on your local machine
- Test configuration connects to `localhost:5432` (which the tunnel provides)
- All communication is encrypted through the SSH tunnel

### Connection Details

When tests run through the tunnel:
- **Local:** `127.0.0.1:5432` (where tests connect)
- **Remote:** VM's `10.18.73.92:5432` (PostgreSQL container)
- **Database:** `<your-db-name>`
- **User:** `<your-db-username>`
- **Authentication:** SCRAM-SHA-256

### Test Execution

The full test suite includes:
- **Entity/Model Tests** - Quick validation tests (~0.1s each)
- **Controller Tests** - Mock servlet tests with Spring context (~0.5-2s each)
- **Mapper Tests** - Integration tests requiring database connection (~7s each)
  - These tests require the SSH tunnel to be active
  - They use Flyway to initialize the database schema and test data automatically

Example successful test run output:
```
[INFO] Tests run: 179, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Troubleshooting

**Error: "Connection to localhost:5432 refused"**
- SSH tunnel is not running or has been closed
- Verify Terminal 1 still shows the SSH session is active
- Restart the SSH tunnel in Terminal 1

**Error: "FATAL: password authentication failed"**
- Verify `.env` file has correct DB_USERNAME and DB_PASSWORD
- Verify SSH credentials in Terminal 1
- Confirm database container is running on the VM

**Error: "Cannot obtain connection from database"**
- Ensure SSH tunnel is established before running tests
- Check that `.env` variables are set correctly
- Verify network connectivity to your VM

# Spring Profiles and Market Hours

## Development Mode (Default)

When running the application without a specific Spring profile (default behavior), the MarketStatusService bypasses market hours validation. This allows testing and development to proceed at any time.

**Running in development mode:**
```bash
java -jar target/leap-0.0.1-SNAPSHOT.jar
```

## Production Mode

To enforce real US market hours (9:30 AM - 4:00 PM EST, Monday-Friday), deploy with the `prod` profile:

```bash
java -jar target/leap-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

**Important for Production Deployment:**
- Before going live, ensure the deployment configuration sets `--spring.profiles.active=prod`
- Orders placed outside market hours will be rejected with 422 UNPROCESSABLE_ENTITY and error message "Cannot place order: Market is closed"
- This enforces regulatory compliance with real market hours
- Test the `prod` profile thoroughly before production release
