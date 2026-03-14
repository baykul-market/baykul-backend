# Baykul Backend Application

## Requirements
- **Docker** and **Docker Compose** (any OS)

## Quick Start

### 1. Clone the repository
```bash
git clone <your-repository-url>
cd baykul-backend
```

### 2. Create `.env` file
```env
POSTGRES_DB=baykul
POSTGRES_USER=baykul_user
DB_PASSWORD=your_secure_password
JWT_ACCESS_SECRET=your_access_secret_key_32_bytes_minimum
JWT_REFRESH_SECRET=your_refresh_secret_key_64_bytes_minimum_for_refresh
SECURITY_USER_PASSWORD=your_admin_password
```

### 3. Start the application
```bash
docker-compose up -d --build
```

### 4. Check container status
```bash
# View running containers
docker-compose ps

# View application logs
docker-compose logs -f app
```

## For Windows (PowerShell)

```powershell
# Create required directories
mkdir logs, uploads, uploads\tmp -Force

# Start the application
docker-compose up -d --build
```

## Essential Commands

| Command                      | Description                   |
|------------------------------|-------------------------------|
| `docker-compose up -d`       | Start in background           |
| `docker-compose down`        | Stop all containers           |
| `docker-compose logs -f app` | Follow app logs               |
| `docker-compose logs -f db`  | Follow database logs          |
| `docker-compose restart app` | Restart application only      |
| `docker-compose down -v`     | Stop and remove database data |

## Application Access
- **API**: `http://localhost`
- **Database** (local access only): `localhost:54321` (user: `baykul_user`)

## Production Notes
- Change all passwords in `.env`
- Swagger is disabled by default in prod profile
- Configure HTTPS using a reverse proxy

## Troubleshooting

### Check container status
```bash
docker-compose ps
# Both containers should be "Up" and "healthy"
```

### View error logs
```bash
docker-compose logs app --tail 50
```

### Full restart
```bash
docker-compose down
docker-compose up -d --build
```