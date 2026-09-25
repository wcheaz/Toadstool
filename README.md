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

1. Create a `.env` file in the project root (same directory as `pom.xml`):
   ```
   cp .env.example .env
   ```

2. Edit the `.env` file with your database connection details:
   ```env
   DB_HOST=10.18.73.92
   DB_PORT=5432
   DB_NAME=toadstool_db
   DB_USERNAME=toadstool_user
   DB_PASSWORD=toadstool_password
   ```

**Important:** The `.env` file is git-ignored and contains sensitive credentials. Never commit it to version control.

### Configuration Files Modified

The following Spring Boot configuration files use environment variables with fallback defaults:

- **`src/main/resources/application.yml`** - Production/main application configuration
  - Uses `${VAR:default}` pattern for environment variable injection
  - Falls back to `localhost:5432` if `DB_HOST` is not set
  - Contains Flyway migration settings and MyBatis mapper configuration

- **`src/test/resources/application.yml`** - Test application configuration
  - Overrides settings for test environment
  - Points to database at `localhost:5432` (expects SSH tunnel for VM connections)
  - Enables Flyway automatic migration and validation

### Variable Reference

| Variable | Purpose | Example |
|----------|---------|---------|
| `DB_HOST` | PostgreSQL server hostname or IP | `10.18.73.92` or `localhost` |
| `DB_PORT` | PostgreSQL server port | `5432` |
| `DB_NAME` | Database name | `toadstool_db` |
| `DB_USERNAME` | Database username | `toadstool_user` |
| `DB_PASSWORD` | Database password | `toadstool_password` |

# Testing

## Running Tests with SSH Tunnel

The test suite includes integration tests that connect to the PostgreSQL database. To run tests against the VM database, you must establish an SSH tunnel.

### Prerequisites

- SSH access to the VM: `ec2-user@10.18.73.92`
- SSH credentials: password `n3u3da!`
- `.env` file configured (see Setup/Initialization section)
- Two terminal windows (one for tunnel, one for tests)

### Step 1: Establish SSH Tunnel (Terminal 1)

Open a PowerShell window and create a local port forward to the VM's PostgreSQL:

```powershell
ssh -L 5432:localhost:5432 ec2-user@10.18.73.92
```

When prompted, enter the SSH password: `n3u3da!`

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
- **Database:** `toadstool_db`
- **User:** `toadstool_user`
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
- Verify SSH credentials in Terminal 1 (ec2-user password)
- Confirm toadstool_db container is running on the VM

**Error: "Cannot obtain connection from database"**
- Ensure SSH tunnel is established before running tests
- Check that `.env` variables are set correctly
- Verify network connectivity to VM at `10.18.73.92`
