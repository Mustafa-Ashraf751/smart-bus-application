# VerySmartBus — current implementation audit

## Scope and technology

This report describes code currently on `main`, not intended requirements. The application is Spring Boot **3.3.4**, Java **17**, Maven, Spring MVC, Spring Data JPA, Spring Security, Hibernate Spatial/JTS, PostgreSQL/Supabase, Flyway, JJWT, and Spring STOMP WebSocket. Production JPA uses `ddl-auto: none`; Flyway runs migrations `V1` through `V31`, validates them, and permits out-of-order migrations. Tests use JUnit/Mockito/Spring MockMvc and H2; H2 does not faithfully prove PostGIS SQL.

Normal request flow:

```text
frontend → Controller (HTTP/DTO validation) → Service (rules/transaction)
→ Repository/JPA/native PostGIS query → PostgreSQL → response DTO
```

`JwtAuthenticationFilter` runs before controllers for bearer requests. It validates JWT, reloads current user/roles/permissions from the database, and sets `UserPrincipal` only when the account is ACTIVE. Controllers are thin adapters; services own business rules; mappers build DTOs. The only scheduled worker is the notification outbox worker; there is **no daily-trip scheduler**.

## Database/domain model

| Entity/table | Current implementation |
|---|---|
| `users` / `AppUser` | PK `user_id`; unique email; name, phone, address, password_hash, status, created/updated timestamps. Status: `PENDING`, `ACTIVE`, `INACTIVE`. Public register creates PENDING; normal admin creation defaults ACTIVE. `user_role` is the many-to-many role join. |
| `role`, `permission` | PKs `role_id`,`permission_id`; unique names. `role_permission` joins roles to permissions. Seeded roles are ADMIN, DRIVER, EMPLOYEE. **BUS_ADMIN is used by code/security but is not seeded by ReferenceDataSeeder.** |
| `buses` | PK id, unique bus_number, capacity >= 1, ACTIVE/INACTIVE, timestamps. |
| `stations` | PK id, name/address/status, `area extensions.geometry(Polygon,4326)`. Native SQL creates a 50m PostGIS geography buffer from submitted lat/lon. |
| `service_zones` | Polygon geometry table/entity exists, but no API/service currently uses it. |
| `routes` | PK id, unique name, direction, planned `path geometry(LineString,4326)`, ACTIVE/INACTIVE, default departure time/bus/driver/busAdmin. A reusable template. |
| `route_stations` | FK Route + Station, expected time/order; unique route+station and route+order. Fixed template stops. |
| `trips` | FK Route/Bus/driver/busAdmin, serviceDate, schedule/actual times, status, `current_location geometry(Point,4326)`, locationUpdatedAt. Unique `(route_id,service_date)`. One actual run. |
| `trip_stations` | FK Trip + Station, actual expected time/order/status. Copied from RouteStations, then Bus Admin can edit while SCHEDULED. |
| `transport_preferences` | FK user/route/pickup RouteStation/dropoff RouteStation, direction, active, timestamps. One preference per user+direction by service/repository check, even when inactive. |
| `reservation` | FK user/trip/pickup TripStation/dropoff TripStation; status RESERVED/WAITLISTED/CANCELLED/COMPLETED; source MANUAL/PREFERENCE; timestamps. Partial unique DB index permits one non-cancelled reservation per user/trip. |
| `trip_station_presence` | FK **Trip + physical Station** (not TripStation), unique pair; count, confirmed flag/at, last observed. |
| `notifications`, `notification_outbox` | Durable inbox plus delivery event. Outbox has unique eventKey, status/attempt/retry/lock/error/sent fields. |

```text
Station = physical pickup place/polygon.
RouteStation = station in reusable Route template.
TripStation = station in one daily Trip.
Route = reusable plan. Trip = one daily execution.
Preference = recurring desire. Reservation = actual daily seat/waitlist record.
```

## Security and roles

Public: login/register, Swagger/OpenAPI, health, OPTIONS and WebSocket HTTP handshake. `/api/admin/**` requires ADMIN, `/api/bus-admin/**` requires BUS_ADMIN, `/api/driver/**` requires DRIVER, all other APIs require authentication. Creating preference additionally requires `BOOK_TRIP` authority. JWT has user ID/email/name/contact/roles/permissions/iat/exp and lasts 24h by default.

Important ownership rules:

- Bus Admin must also equal `Trip.busAdmin.id`; the role alone cannot manage all trips.
- Legacy Driver APIs require `Trip.driver.id` to equal caller.
- Generic start/location permits the assigned driver **or any RESERVED passenger**.
- Cancel reservation requires owner or ADMIN. Reservation passenger/bus lookup allows owner, assigned driver, assigned Bus Admin, or ADMIN.
- Preference and notification updates require ownership.

## All HTTP controllers/endpoints

All request DTOs are `@Valid`; bad field values are 400; missing resources normally 404; rule/state conflicts normally 409; ownership failures 403. No controller declares query parameters.

| Controller | Endpoint(s), caller, body, current effect |
|---|---|
| AuthController | `POST /api/auth/login` public, `{email,password}` → 200 JWT/user; invalid/disabled is 401. `POST /api/auth/register` public, `{name,phone?,address?,email,password}` → 201 `{userId,email,status:PENDING,message}`, hashes password and assigns EMPLOYEE, no token. `GET /api/auth/me` authenticated → current AuthUser. |
| UserController | ADMIN: `POST/GET /api/admin/users`, `GET/PUT /{id}`, `PATCH /{id}/activate|deactivate|approve|reject`, `PUT/DELETE /{id}/roles/{roleId}`. User body has name/email required, phone/address optional, password required only create. GET returns all users including pending. Approve/reject only PENDING. |
| RoleController / PermissionController | ADMIN CRUD `/api/admin/roles`, `/api/admin/permissions`; role also PUT/DELETE `/{roleId}/permissions/{permissionId}`. Name required; hard delete. |
| BusController | ADMIN `POST/GET /api/admin/buses`, `GET/PUT /{id}`, `PATCH /{id}/activate|deactivate`; body `{busNumber,capacity 1..200}`. |
| StationController | ADMIN CRUD `/api/admin/stations`; `{name,address?,pickupLatitude,pickupLongitude,status}`. Writes 50m polygon. |
| RouteController | ADMIN CRUD `/api/admin/routes`, `PUT /{id}/path`. Route body requires name,status,direction, HH:mm:ss default departure, default bus/driver/busAdmin IDs. Path requires >=2 valid latitude/longitude points. |
| RouteStationController | ADMIN `POST/GET /api/admin/route-stations`, `GET/DELETE /{id}`; body `{routeId,stationId,stopOrder>=1,expectedArrivalTime}`. Validates template association/unique order. No update/reorder endpoint. |
| TripController | ADMIN `POST/GET /api/admin/trips`, `GET /{id}`, `PATCH /{id}/start|complete`, `DELETE /{id}`; body `{routeId,serviceDate}`. Manual create copies route stations. Delete is hard delete. |
| TodayTripController | authenticated `GET /api/trips/today`: lazy-generates missing today trips then returns them. |
| TripLocationController | authenticated `GET /api/trips/{tripId}/location`: returns latest point/updatedAt/stale/presences. **No REST reservation authorization.** |
| TripParticipantController | authenticated `PATCH /api/trips/{id}/start`, `/location`; location body `{latitude,longitude}` within world ranges. Assigned driver OR RESERVED passenger only; location requires IN_PROGRESS. |
| BusAdminTripController | BUS_ADMIN plus assignment: `GET /me`, `GET /{trip}`, PATCH bus `{busId}`, driver `{driverId}`, start, complete. Bus/driver edits scheduled-only; driver must have DRIVER role; bus active. |
| TripStationController | assigned BUS_ADMIN: `GET/POST /api/bus-admin/trips/{trip}/stations`, PATCH `/{station}/arrival-time`, DELETE `/{station}`. Scheduled-only; add shifts orders; time update notifies affected reservations. Delete prevents pickup-use/last stop but does **not** check dropoff-use. |
| ReservationController | authenticated `POST /api/reservations` `{tripId,pickupTripStationId,dropoffTripStationId}`; locks Trip, checks SCHEDULED/today, stops belong/order, capacity, duplicate, then MANUAL RESERVED/WAITLISTED and notification. `PATCH /{id}/cancel`; `GET /me`; `GET /trip/{id}`, `/trip/{id}/bus` with operational ownership checks. |
| TransportPreferenceController | authenticated; create needs BOOK_TRIP. `POST`, `GET /me`, `PUT /{id}`, `PATCH /{id}/toggle-active`, `DELETE /{id}`. Body has routeId, pickup/dropoff **RouteStation** IDs, direction, optional active. Validates route membership/order and one direction/user. |
| DriverTripController / DriverTripLocationController | DRIVER + assigned driver: PATCH `/api/driver/trips/{id}/start`, `/end`, `/location`. These are legacy wrappers. **No GET /api/driver/trips/me.** |
| NotificationController | authenticated: GET `/api/notifications/me`, `/me/unread-count`, PATCH `/{id}/read`, `/read-all`; own inbox only. |

Responses: `TripResponseDto` contains trip IDs/default assignments/service date/times/current location/stale/status/timestamps. `ReservationResponseDto` contains IDs, pickup/dropoff names/TripStation IDs, status/source/times. Swagger gives exact JSON schemas for all response DTOs.

## Daily Trip and automatic preference flow

No job creates trips in advance. `GET /api/trips/today` and `GET /api/reservations/me` call `DailyTripService.ensureTodayTrips()` for Cairo's current date. It finds active routes without a trip; each call invokes `DailyTripCreator.createIfMissing` in `REQUIRES_NEW` transaction.

```text
active ready Route (default departure/bus/driver required; Bus Admin not required here)
→ save Trip with route/default bus/default driver/default busAdmin/date/Cairo OffsetDateTime
→ copy ordered RouteStations to TripStations
→ load active route preferences oldest first
→ match their physical stations to new TripStations
→ allocate RESERVED until bus capacity, then WAITLISTED, source=PREFERENCE
```

Unique `(route_id,service_date)` plus exists check protects duplicate trips; outer service catches race DataIntegrityViolationException. Admin can also manually create Trip. Preference updates do not alter existing reservations. Automatic preference reservations **do not currently create reservation notifications**, because DailyTripCreator bypasses ReservationService/TripNotificationService.

## Cancellation, GPS, notifications, WebSocket

Cancellation locks the Trip, changes reservation to CANCELLED/cancelledAt, and promotes oldest WAITLISTED by `reservedAt,reservationId` when a RESERVED seat was freed. It creates cancellation and promotion notifications. It has no trip-state rule: current code permits cancellation even after start/completion.

GPS is written as Point SRID 4326 to `Trip.currentLocation`; Station SQL uses `ST_Covers(station.area, point)` for active stations attached to that Trip. First inside hit makes count=1; second consecutive hit makes confirmed=true and sends pickup notification to RESERVED passengers whose pickup physical station matches. Leaving resets count/confirmed; re-entering twice can notify again. Location event is published after commit to `/topic/trips/{id}/location`.

STOMP connects at `/ws` with native `Authorization: Bearer <token>`. Subscribe to `/user/queue/notifications` (own user queue) or authorized `/topic/trips/{id}/location`. Clients may not send to `/topic`; allowed send prefix is `/app`, but no application message mappings exist. NotificationService writes notification + outbox in same transaction. Worker runs every 5 seconds by default, claims native `FOR UPDATE SKIP LOCKED` batch, publishes, marks sent or retries after 10s; it has no maximum retry handling.

Notification creators: manual reservation confirmed/waitlisted; cancellation; waitlist promotion; any trip start; Bus Admin station time update; second pickup arrival reading. `TRIP_CANCELLED` enum exists but no code currently creates it.

## Current rule audit/gaps

| Rule | Actual status |
|---|---|
| one trip per route/date; copy route stations | implemented (unique index, DailyTripCreator/TripService) |
| reservation stops/order/capacity/waitlist promotion | implemented; Trip row pessimistically locked |
| only RESERVED consumes seat | implemented |
| Bus Admin only assigned trip | implemented |
| driver only assigned trip | legacy APIs yes; generic APIs deliberately also allow RESERVED passenger |
| location only in progress | implemented |
| passenger REST location only for own reservation | **not implemented**; every authenticated user can read it |
| cancellation forbidden after start | **not implemented** |
| bus change capacity safeguard | **not implemented**; smaller active bus can be assigned |
| completed/cancelled history | completed persists, but admin can hard-delete; no cancellation endpoint |
| RouteStation change never changes old TripStation | implemented by copy/no propagation |
| two readings required | implemented, but reset on leave allows later repeated notification |

Frontend gaps: no public `GET /api/routes`, `GET /api/routes/{id}/stations`, or `GET /api/trips/{id}/stations`, so passenger preference/trip selection cannot be built safely without adding public read APIs. No Driver “my trips” API. No TripStation skip/reorder or Bus Admin manual passenger management. No dashboard aggregate API.

Duplicate flows: admin, Bus Admin, Driver legacy, and generic participant start paths coexist with different authorization/today checks. Legacy driver and generic participant location endpoints call same location service; generic intentionally adds RESERVED passengers. Admin manual and lazy generation are separate Trip creation paths.

## File verification list

Security: `SecurityConfig`, `AuthService`, `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService`, `UserPrincipal`, all access authorizers, `WebSocketJwtChannelInterceptor`.

Controllers: every file under `src/main/java/com/verysmartbus/controller`.

Core services: `TripService`, `DailyTripService`, `DailyTripCreator`, `ReservationService`, `TransportPreferenceService`, `BusAdminTripService`, `TripStationService`, `TripNotificationService`, `NotificationService`, `NotificationOutboxService`, `NotificationOutboxWorker`.

Persistence: all `entity` and `repository` files; especially `StationRepositoryImpl`, `TripStationPresence`, `Reservation`, and all migrations `V1`–`V31`.

Realtime/setup: `WebSocketConfig`, `websocket/*`, `TripLocationUpdatedEvent`, `pom.xml`, `application.yml`, `seeders/*`.
