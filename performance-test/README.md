# Gatling Performance Test Setup

## Requirements
- Java 21
- Running app (`http://localhost:8000` by default)
- A valid existing user for login scenario

## Quick Run (Recommended)
Use script:

```bash
./performance-test/runPerformance.sh
```

Credential priority:
- `PERF_USERNAME` / `PERF_PASSWORD` environment variables (first)
- Script args (`$1` username, `$2` password) (fallback)
- Defaults (`performanceUser` / `performanceUser`) (last fallback)

Simulation selection:
- Default: `com.worktrack.performance.AuthAndProjectFlowSimulation`
- Override with env var `SIMULATION_CLASS` or script arg `$3`

Example:

```bash
PERF_USERNAME=manager PERF_PASSWORD=manager123 \
SIMULATION_CLASS=com.worktrack.performance.AuthAndProjectFlowSimulation \
PROJECT_PAGE_SIZE=20 \
./performance-test/runPerformance.sh
```

## RPS Capacity Discovery
Use `AuthAndProjectFlowSimulation` for staircase load (users/sec increases by levels):

```bash
PERF_USERNAME=performanceUser PERF_PASSWORD=performanceUser \
SIMULATION_CLASS=com.worktrack.performance.AuthAndProjectFlowSimulation \
START_USERS_PER_SEC=5 \
USERS_PER_SEC_INCREMENT=5 \
LEVELS=8 \
LEVEL_DURATION_SEC=30 \
RAMP_BETWEEN_LEVELS_SEC=10 \
P95_LIMIT_MS=1000 \
MAX_FAILED_PERCENT=1.0 \
PROJECT_PAGE_SIZE=20 \
BUSINESS_CALLS_PER_LOGIN=5 \
MIN_THINK_TIME_MS=50 \
MAX_THINK_TIME_MS=250 \
./performance-test/runPerformance.sh
```

How to read "supported RPS":
- Find the highest load level where both conditions still hold:
- `failedRequests.percent < MAX_FAILED_PERCENT`
- `responseTime.p95 < P95_LIMIT_MS`
- Approx request rate at a level is `usersPerSec * (BUSINESS_CALLS_PER_LOGIN + 3)`.
- Flow is: `1x login` + `1x auth/me` + `1x projects/me` + `N x projects?page`.
- Random think-time is applied between business calls with `MIN_THINK_TIME_MS` / `MAX_THINK_TIME_MS`.
- Assertions are checked both globally and per request (`login`, `auth-me`, `projects-me`, `projects-list`).

## Direct Gradle Run
```bash
./gradlew :performance-test:gatlingRun \
  -PsimulationClass=com.worktrack.performance.AuthAndProjectFlowSimulation \
  -DbaseUrl=http://localhost:8000 \
  -Dperf.username=performanceUser \
  -Dperf.password=performanceUser \
  -DstartUsersPerSec=5 \
  -DusersPerSecIncrement=5 \
  -Dlevels=8 \
  -DlevelDurationSec=30 \
  -DrampBetweenLevelsSec=10 \
  -Dp95LimitMs=1000 \
  -DmaxFailedPercent=1.0 \
  -DprojectPageSize=20 \
  -DbusinessCallsPerLogin=5 \
  -DminThinkTimeMs=50 \
  -DmaxThinkTimeMs=250
```

## Single vs Multiple Simulations
- One run usually targets one `Simulation` class for clean, comparable reports.
- You can keep multiple simulation classes under `src/gatling/java`.
- Choose which one to run with `-PsimulationClass=...`.

## Report
Generated under:
`performance-test/build/reports/gatling`

## Notes
- If credentials are invalid, simulation fails by design.
- `exitHereIfFailed()` stops scenario after failed login.
