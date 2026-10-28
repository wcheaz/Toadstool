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
- Credentials come from environment variables or your local `.env`, not from checked-in configuration.
  EC2 deployments can retrieve them directly from Parameter Store at application startup,
  as described below.

### Application Configuration Files

The following Spring Boot configuration files define the application structure using environment variables:

- **`src/main/resources/application.yml`** - Production/main application configuration
  - Uses environment variables: `${DB_HOST}`, `${DB_PORT}`, `${DB_NAME}`, `${DB_USERNAME}`, `${DB_PASSWORD}`
  - Contains Flyway migration settings and MyBatis mapper configuration
  - Requires database credentials in the environment or `.env`

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

## Backend container on the existing VM

The tracked [Compose example](./docker-compose.yml.example) builds the backend
using the multi-stage [Dockerfile](./Dockerfile); no local Maven installation or
prebuilt JAR is required. Jenkins is outside the scope of this setup.

Compose injects the database credentials, reporting credentials, external API
settings, and optional JWT keys from the VM's existing `.env`. It overrides both
database hosts to `postgres` and ports to `5432`, so the backend connects directly
over the Docker network without an SSH tunnel. Host-side scripts can keep using
`localhost` in `.env`. Environment variables are injected explicitly; the
credentials file is not copied into the image.

Spring Boot listens on container port `8080` through `SERVER_PORT`; the VM exposes
it on port `8081`. Running Spring Boot outside Docker still defaults to `8081`.
The existing Spring profile behavior is unchanged.

On the VM, after transferring the updated files:

1. Keep the existing `.env` and PostgreSQL data. Fill in the required database,
   Fauxnance, and Finnhub settings using real values, not example placeholders.
   Configure both JWT keys for tokens to survive backend restarts; omitting both
   retains the existing temporary-key development behavior.
2. Compare the updated example with the VM's `docker-compose.yml`. Copy the example
   to `docker-compose.yml` only after confirming the PostgreSQL service, volume,
   and networks match the existing deployment. The actual Compose file is ignored
   by Git and will not be updated by a pull.
3. Run the following from the existing VM Compose directory, retaining the same
   Compose project name (`toadstool`) and any existing project-name settings:

   ```bash
   docker-compose -p toadstool config --quiet
   docker-compose -p toadstool ps postgres
   docker-compose -p toadstool up -d --build --no-deps api
   docker-compose -p toadstool logs --tail=100 api
   curl --fail http://localhost:8081/api/health
   ```

`--no-deps` updates only the backend; PostgreSQL must already be running and ready.
Do not run `docker compose down -v`. Back up the database before starting a new
backend version: Flyway automatically applies pending application migrations on
startup, even though the PostgreSQL container itself is unchanged.

The existing PostgreSQL initialization mount is unchanged in this step. Flyway's
authoritative migrations are packaged from `src/main/resources/db/migrations`.

The health endpoint confirms HTTP availability only. Also verify login, a
database-backed application request, and an admin reporting request before
considering the deployment validated. Do not share `.env`, rendered Compose
configuration, or logs containing credentials.

## Password-only RDS with Parameter Store (Docker run on EC2)

The application has separate trading and reporting data sources. Neither needs
IAM database authentication. Parameter Store access uses the EC2 instance role;
PostgreSQL connections use the database username and password. With the `aws`
Spring profile enabled, the application inside the container retrieves parameters
directly through Spring Cloud AWS. No AWS CLI, host env file, or credentials
loader is required. Without this profile, local `.env` and environment-variable
configuration continue to work without Parameter Store access.

### Store the configuration

In AWS Systems Manager -> Parameter Store, select region `us-east-1` and create
one parameter per environment variable under `/toadstool/prod/`. For example,
the parameter `/toadstool/prod/DB_HOST` becomes Spring property `DB_HOST`.
Do not append a slash to the variable name. These properties are not exported
as operating-system environment variables.
Enter raw values, without `.env` quotes or an `export` prefix.

Use `SecureString` for passwords, API keys, and private JWT keys. `String` is
sufficient for non-secret configuration.

| Parameter suffix | Value |
|---|---|
| `DB_HOST` | `toadstool-rds.c8xuasgw2vz5.us-east-1.rds.amazonaws.com` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `toadstool_db` |
| `DB_USERNAME` | `toadstool_user` |
| `DB_PASSWORD` | New standalone RDS password |
| `REPORTING_DB_HOST` | Same new RDS endpoint |
| `REPORTING_DB_PORT` | `5432` |
| `REPORTING_DB_NAME` | `toadstool_reporting` |
| `REPORTING_DB_USERNAME` | `toadstool_user` |
| `REPORTING_DB_PASSWORD` | New standalone RDS password |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://toadstool-rds.c8xuasgw2vz5.us-east-1.rds.amazonaws.com:5432/toadstool_db?sslmode=verify-full&sslrootcert=/app/rds-global-bundle.pem` |
| `REPORTING_DB_URL` | `jdbc:postgresql://toadstool-rds.c8xuasgw2vz5.us-east-1.rds.amazonaws.com:5432/toadstool_reporting?sslmode=verify-full&sslrootcert=/app/rds-global-bundle.pem` |
| `SERVER_PORT` | `8080`, if retaining host mapping `8081:8080` |

`SPRING_DATASOURCE_URL` overrides the main Spring datasource URL.
`REPORTING_DB_URL` overrides the reporting host/name settings. Remove stale URL
overrides that point to the old database. The certificate path above is inside
the container, not the EC2 host.

Also migrate the existing values for `FAUXNANCE_BASE_URL`, `FAUXNANCE_API_KEY`,
`FAUXNANCE_TIMEOUT_SECONDS`, `FINNHUB_API_KEY`, and any configured
`APP_CORS_ALLOWED_ORIGINS` and authentication settings. Set active profiles on
the Docker command, not in Parameter Store: `aws` must be active before import.
If previously using the `prod` profile, enable both with `aws,prod`.
Preserve both `AUTH_JWT_PUBLIC_KEY` and `AUTH_JWT_PRIVATE_KEY` if configured so a
restart does not unexpectedly rotate signing keys. These must be single-line,
Base64-encoded DER values, as in `.env.example`. Preserve the existing
`AUTH_JWT_REQUIRE_HTTPS` setting.

Do not upload the `.env` file or paste decrypted parameter values into logs or
chat. Enter each value explicitly rather than references such as `${DB_PASSWORD}`.
The import is required, not optional: an inaccessible or missing parameter path
fails startup. Supply all required database and API settings before starting.

### Allow the EC2 role to read this path

Attach this permission to the EC2 instance role, not the database:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "ssm:GetParametersByPath",
      "Resource": "arn:aws:ssm:us-east-1:314662805698:parameter/toadstool/prod/*"
    }
  ]
}
```

For SecureString parameters encrypted with a customer-managed KMS key, also grant
`kms:Decrypt` for that key and ensure its key policy permits the role. The default
AWS-managed `aws/ssm` key normally requires no additional custom key policy.
Keep unrelated secrets outside this dedicated path.

### Recreate the API container

Commit/push the application changes, pull them in the separate EC2 clone, and
build a new image there with `docker build -t toadstool-api:ssm .`. Local edits
do not update EC2 automatically. The image includes the Java integration, not
AWS credentials or parameter values.

The AWS SDK uses the default credentials chain, including the EC2 instance role.
Do not pass AWS access keys or mount your AWS credentials directory. The container
must reach EC2 instance metadata to obtain role credentials. Keep IMDSv2 required;
Docker bridge networking may require the instance's metadata response hop limit
to be 2 rather than 1. Review that instance-level change before applying it.
Also allow outbound HTTPS to the regional SSM endpoint (via internet/NAT or an
SSM interface VPC endpoint with private DNS). RDS still needs inbound 5432 from
the EC2 security group.

Before cutover, stop source writes and refresh the restored data if the source
has changed since the dumps. Snapshot the new RDS database before starting the
backend: Flyway automatically applies pending migrations at application startup.
Avoid unrelated application upgrades during cutover.

Record the current image, ports, network names, aliases, volumes, restart policy,
and command overrides without dumping container environment variables. For
example:

```bash
docker inspect --format 'Image={{.Config.Image}} Ports={{json .HostConfig.PortBindings}} Networks={{json .NetworkSettings.Networks}}' toadstool-api
```

Use a spare, loopback-only port and a temporary name to test the replacement
before stopping the existing API. Substitute the actual Docker
network; this is an example, not a replacement for existing deployment options:

```bash
docker run \
  --detach \
  --name toadstool-api-rds-check \
  --network YOUR_EXISTING_DOCKER_NETWORK \
  --publish 127.0.0.1:18081:8080 \
  --env SPRING_PROFILES_ACTIVE=aws \
  --env AWS_REGION=us-east-1 \
  --env SERVER_PORT=8080 \
  --mount type=bind,src=/home/ubuntu/rds-migration/global-bundle.pem,dst=/app/rds-global-bundle.pem,readonly \
  toadstool-api:ssm

docker logs --tail 100 toadstool-api-rds-check
curl --fail --show-error http://127.0.0.1:18081/api/health
```

The check container is a real running application. Keep users and other writers
stopped during testing; avoid running two backends concurrently if scheduled
application work can change data. The HTTP health endpoint alone does not verify
database connectivity. Check startup/Flyway logs, login, a trading-data request,
and an admin reporting request. Compare against expected restored data.

After testing, stop/remove only the temporary container, then stop and retain the
old API container under a different name. Recreate the production API using
`docker run` with its original name, network aliases, host ports, restart policy, and
other required options. Preserve the frontend's upstream hostname and container
port. Do not retain `--env-file ../toadstool.env` or old database environment
overrides: environment variables take precedence over Parameter Store properties.
If the frontend uses Nginx, reload/restart it after the backend swap
so it resolves the replacement container.

After deploying the new integration, `docker restart` restarts Java and fetches
current Parameter Store values. There is no live refresh. Initial migration still
requires recreating the container to remove its old environment and use the new
image. Secrets remain in application memory; host/container administrators can
still access them. Never log resolved configuration, bake `.env` into the image,
or mount the old file into the new container.

### Update other database consumers and finish cutover

If running the reporting sync or admin outbox processor, update their launch
environment as well: `SOURCE_DB_*` must reference `toadstool_db` on the new RDS
endpoint and `TARGET_DB_*` must reference `toadstool_reporting` there. This Spring
integration only configures the API, not Python workers, cron jobs, or services. Also
update separately deployed Superset database connections if present.

Once login, trading, reporting, and workers are verified, remove the old host
`.env` only after confirming nothing still depends on it. Keep recoverable backups
and the old database through the rollback window. Rolling back to a source that
has become stale after new RDS writes requires data reconciliation.

These instructions use `docker run`. The local Compose example still pins both
hosts to its `postgres` service; changing Parameter Store does not change that
Compose configuration.

## Frontend container on the existing VM

The frontend serves production Angular assets using Nginx and forwards `/api/*`
to the backend on the internal Compose network. Browsers use the frontend origin,
not `localhost:8081` or a Docker service hostname, so no new CORS origins are needed.
The default host port is `8083` to avoid assuming Superset's ports are available.
Check occupied ports with `docker ps` and `ss -ltn` first; set `FRONTEND_PORT` in
the VM's `.env` to a free port if needed.

After transferring the changes, compare and copy `docker-compose.yml.example`
to the ignored `docker-compose.yml`, keeping the existing VM `.env` and project
name. With the backend and PostgreSQL already running:

```bash
docker-compose -p toadstool config --quiet
docker-compose -p toadstool up -d --build --no-deps frontend
docker-compose -p toadstool ps
docker-compose -p toadstool logs --tail=60 frontend
curl --fail --show-error http://localhost:8083/healthz
curl --fail --show-error http://localhost:8083/login
curl --fail --show-error http://localhost:8083/api/health
```

Substitute the selected port if different. The first endpoint checks Nginx only;
the last checks the reverse proxy and backend. Test browser login, news, admin
reporting, and refreshing a deep link such as `/login`. API failures must remain
API errors rather than returning the Angular HTML shell.

Access `http://10.18.73.92:8083` if the network/firewall permits it, or keep the
deployment private and forward the frontend port from your workstation:

```bash
ssh -N -L 4201:localhost:8083 ec2-user@10.18.73.92
```

Then browse `http://localhost:4201`. No separate browser-facing backend tunnel is
needed. HTTPS and production hardening are separate follow-up work.

The VM uses the standalone `docker-compose` command. On installations with the
Compose plugin, use `docker compose` with the same arguments instead.

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
