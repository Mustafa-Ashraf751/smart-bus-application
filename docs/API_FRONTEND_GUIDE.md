# VerySmartBus frontend API guide

This guide describes the API currently implemented on `main`. Swagger remains the
source of truth for every field and response schema while developing:

`http://localhost:8080/swagger-ui/index.html`

## 1. Conventions

- Base URL in local development: `http://localhost:8080`.
- Send JSON and use `Content-Type: application/json`.
- Except for login, send `Authorization: Bearer <accessToken>` on every request.
- `401` means the token is missing/expired. `403` means the logged-in user is not
  allowed to do that action. `400` means invalid request data. `404` means the ID
  does not exist (or is not accessible).
- Times use one string, never the Java time object form: `"13:34:44"` (`HH:mm:ss`).
- Coordinates are JSON objects in this order: `{ "longitude": 31.0, "latitude": 30.0 }`.
- IDs with different names are **not interchangeable**. In particular:
  - `stationId`: a fixed physical station.
  - `routeStationId`: a station in the route template.
  - `tripStationId`: a station in one specific day's trip. Reservations use this ID.

## 2. Login and session

### `POST /api/auth/login` — public

```json
{ "email": "passenger@example.com", "password": "password" }
```

The response contains `accessToken`, `tokenType`, `expiresInSeconds`, and `user`.
Keep the token according to the frontend security approach and attach it as the
Bearer token. `user.roles` and `user.permissions` decide which screens/actions to show.

### `POST /api/auth/register` — public

Creates a normal passenger account with `PENDING` status. The frontend must never send
a role: public registration always assigns only the `EMPLOYEE` role, which has
`BOOK_TRIP`. Admin, Driver, and Bus Admin roles can be assigned only through admin APIs.

```json
{
  "name": "Sara Ahmed",
  "phone": "01000000000",
  "address": "Sheikh Zayed",
  "email": "sara@example.com",
  "password": "secure-password"
}
```

Returns `201 Created` with its ID, email, `PENDING` status, and approval message—**not a
JWT**. Show a “waiting for admin approval” screen. A pending or rejected account cannot
log in. A duplicate email returns `409 Conflict`; password must be 8–72 characters.

### `GET /api/auth/me` — logged-in user

Returns current user identity, status, roles, and permissions. Call this after app
reload if the frontend needs the latest role/permission state.

## 3. Passenger app

### Today and live trip

| Method | Endpoint | What the frontend uses it for |
|---|---|---|
| GET | `/api/trips/today` | Gets today's operational trips. It also lazily creates missing daily trips first. |
| GET | `/api/trips/{tripId}/location` | Gets current location, freshness flag, and station-presence information. |
| PATCH | `/api/trips/{tripId}/start` | Starts a trip; only assigned driver or a passenger with a `RESERVED` reservation may do it. |
| PATCH | `/api/trips/{tripId}/location` | Sends the shared bus location; assigned Bus Admin, assigned driver, or a passenger with a `RESERVED` reservation may send it. |

Location request body:

```json
{ "latitude": 30.012345, "longitude": 31.123456 }
```

`GET /api/trips/{tripId}/location` and the location WebSocket topic return:

```json
{
  "tripId": 14,
  "currentLocation": { "longitude": 31.123456, "latitude": 30.012345 },
  "locationUpdatedAt": "2026-10-13T08:04:00+03:00",
  "locationStale": false,
  "stationsContainingBus": [
    { "stationId": 5, "stationName": "Zayed Gate", "consecutiveInsideCount": 2, "arrivalConfirmed": true }
  ]
}
```

`arrivalConfirmed: true` means two consecutive location readings were inside the
station pickup area. That triggers the pickup-arrival notification for confirmed
passengers at that station.

### Transport preferences

A preference describes a passenger's normal route pickup/drop-off. When today's
Trip is created, active preferences become that day's reservations.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/transport-preferences/routes` | List active fixed routes the passenger may choose for a preference. |
| GET | `/api/transport-preferences/routes/{routeId}/stations` | List active fixed RouteStations, ordered by stop order, for one chosen route. |
| POST | `/api/transport-preferences` | Create a preference (`BOOK_TRIP` permission required). |
| GET | `/api/transport-preferences/me` | List the logged-in passenger's preferences. |
| PUT | `/api/transport-preferences/{preferenceId}` | Update own preference. |
| PATCH | `/api/transport-preferences/{preferenceId}/toggle-active` | Pause/resume it. |
| DELETE | `/api/transport-preferences/{preferenceId}` | Delete it. |

```json
{
  "routeId": 3,
  "pickupRouteStationId": 12,
  "dropoffRouteStationId": 18,
  "direction": "TO_COMPANY",
  "active": true
}
```

`pickupRouteStationId` and `dropoffRouteStationId` must be route-template station
IDs, not physical `stationId`s. Valid directions: `TO_COMPANY`, `FROM_COMPANY`.

Preference-form flow:

```text
GET /api/transport-preferences/routes
→ passenger chooses a fixed route
→ GET /api/transport-preferences/routes/{routeId}/stations
→ passenger chooses pickup and dropoff RouteStations
→ POST /api/transport-preferences
```

These endpoints return fixed RouteStation IDs. Do not use `tripStationId` from
`GET /api/reservations/available-trips` here; those are for a one-day manual
reservation.

### Reservations

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/reservations/available-trips` | Show bookable daily trips, stops, capacity, availability, and the passenger's current reservation. Optional `?routeId={id}` filters one route. |
| POST | `/api/reservations` | Create a manual reservation for current user. |
| GET | `/api/reservations/me` | List own reservations; also ensures today's preference reservations exist. |
| PATCH | `/api/reservations/{reservationId}/cancel` | Cancel own reservation. |
| GET | `/api/reservations/trip/{tripId}` | List trip passengers; only authorized operational users. |
| GET | `/api/reservations/trip/{tripId}/bus` | Get assigned bus; only authorized operational users. |

```json
{
  "tripId": 14,
  "pickupTripStationId": 51,
  "dropoffTripStationId": 57
}
```

Manual reservations use **TripStation IDs**, because actual daily trip stations can
differ from the original RouteStations. Statuses: `RESERVED`, `WAITLISTED`,
`CANCELLED`, `COMPLETED`. A cancelled confirmed reservation may promote the oldest
waitlisted passenger.

Booking-screen flow: call `GET /api/reservations/available-trips`, choose its
`tripId`, `pickupTripStationId`, and `dropoffTripStationId`, then call
`POST /api/reservations`. The GET call creates any missing trips for today before
returning the booking choices.

### Notifications

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/notifications/me` | List own notifications. |
| GET | `/api/notifications/me/unread-count` | Returns `{ "unreadCount": 3 }`. |
| PATCH | `/api/notifications/{notificationId}/read` | Mark one as read. |
| PATCH | `/api/notifications/read-all` | Mark all own notifications as read; returns `204`. |

Notification types currently used: `RESERVATION_CONFIRMED`, `RESERVATION_WAITLISTED`,
`WAITLIST_PROMOTED`, `RESERVATION_CANCELLED`, `TRIP_STARTED`,
`TRIP_STATION_TIME_UPDATED`, and `BUS_ARRIVED_AT_PICKUP`.

## 4. Bus Admin app

All endpoints below require the `BUS_ADMIN` role. The server additionally verifies
that the trip belongs to that Bus Admin.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/bus-admin/trips/me` | List assigned trips. |
| GET | `/api/bus-admin/trips/{tripId}` | Get one assigned trip. |
| PATCH | `/api/bus-admin/trips/{tripId}/bus` | Change assigned bus. |
| PATCH | `/api/bus-admin/trips/{tripId}/driver` | Change assigned driver. |
| PATCH | `/api/bus-admin/trips/{tripId}/start` | Start the trip. |
| PATCH | `/api/bus-admin/trips/{tripId}/complete` | Complete the trip. |
| PATCH | `/api/bus-admin/trips/{tripId}/location` | Send the live location for the assigned trip. |
| GET | `/api/bus-admin/trips/{tripId}/stations` | List actual TripStations. |
| POST | `/api/bus-admin/trips/{tripId}/stations` | Add a physical station to actual trip. |
| PATCH | `/api/bus-admin/trips/{tripId}/stations/{tripStationId}/arrival-time` | Change only that actual station's time. |
| DELETE | `/api/bus-admin/trips/{tripId}/stations/{tripStationId}` | Remove a station from actual trip. |

Change bus/driver bodies:

```json
{ "busId": 4 }
```

```json
{ "driverId": 7 }
```

Add actual TripStation:

```json
{ "stationId": 9, "stopOrder": 3, "expectedArrivalTime": "13:34:44" }
```

Update its time:

```json
{ "expectedArrivalTime": "13:34:44" }
```

Location update body:

```json
{ "latitude": 30.012345, "longitude": 31.123456 }
```

RouteStations are the fixed template. TripStations are the actual copy for that
one daily Trip; changing them does not change the route template.

## 5. Driver app (legacy endpoints)

These existing endpoints require `DRIVER` and only accept the assigned driver:

| Method | Endpoint |
|---|---|
| PATCH | `/api/driver/trips/{tripId}/start` |
| PATCH | `/api/driver/trips/{tripId}/end` |
| PATCH | `/api/driver/trips/{tripId}/location` |

For new screens, prefer the generic `/api/trips/{tripId}/start` and
`/api/trips/{tripId}/location` endpoints where appropriate, because they support
the intended shared participant model.

## 6. Admin app

Every `/api/admin/**` endpoint requires `ADMIN`.

| Resource | Endpoints |
|---|---|
| Users | `POST/GET /api/admin/users`, `GET/PUT /api/admin/users/{userId}`, `PATCH /{userId}/approve`, `PATCH /{userId}/reject`, `PATCH /{userId}/activate`, `PATCH /{userId}/deactivate`, `PUT/DELETE /{userId}/roles/{roleId}` |
| Roles | `POST/GET /api/admin/roles`, `GET/PUT/DELETE /api/admin/roles/{roleId}`, `PUT/DELETE /{roleId}/permissions/{permissionId}` |
| Permissions | `POST/GET /api/admin/permissions`, `GET/PUT/DELETE /api/admin/permissions/{permissionId}` |
| Buses | `POST/GET /api/admin/buses`, `GET/PUT /api/admin/buses/{id}`, `PATCH /{id}/activate`, `PATCH /{id}/deactivate` |
| Stations | `POST/GET /api/admin/stations`, `GET/PUT/DELETE /api/admin/stations/{id}` |
| Routes | `POST/GET /api/admin/routes`, `GET/PUT/DELETE /api/admin/routes/{id}`, `PUT /{id}/path` |
| RouteStations | `POST/GET /api/admin/route-stations`, `GET/DELETE /api/admin/route-stations/{id}` |
| Trips | `POST/GET /api/admin/trips`, `GET/DELETE /api/admin/trips/{id}`, `PATCH /{id}/start`, `PATCH /{id}/complete` |

Important admin request examples:

```json
// Route
{
  "name": "Zayed to Company",
  "status": "ACTIVE",
  "direction": "TO_COMPANY",
  "defaultDepartureTime": "07:30:00",
  "defaultBusId": 1,
  "defaultDriverId": 7,
  "defaultBusAdminId": 11
}
```

```json
// Route station (fixed template stop)
{ "routeId": 3, "stationId": 9, "stopOrder": 1, "expectedArrivalTime": "07:30:00" }
```

```json
// Physical station; these coordinates represent the pickup-area center/boundary managed by backend
{
  "name": "Zayed Gate",
  "address": "Zayed",
  "pickupLatitude": 30.012345,
  "pickupLongitude": 31.123456,
  "status": "ACTIVE"
}
```

```json
// Manual Trip creation (normally GET /api/trips/today creates today's trips automatically)
{ "routeId": 3, "serviceDate": "2026-10-13" }
```

## 7. WebSocket / STOMP

Use a STOMP client, connect to:

`ws://localhost:8080/ws`

On the STOMP `CONNECT` frame send native header:

```text
Authorization: Bearer <accessToken>
```

Subscribe to:

| Destination | Who can subscribe | Payload |
|---|---|---|
| `/user/queue/notifications` | Any authenticated user; receives only own messages | `NotificationResponseDto` |
| `/topic/trips/{tripId}/location` | Authorized participant/operational user for that trip | `TripLocationResponseDto` |

Clients must not publish to `/topic/**`. The server alone publishes live locations
after the database transaction commits. Keep a REST fallback: on reconnect, call
`GET /api/notifications/me` and `GET /api/trips/{tripId}/location`.

## 8. Recommended screen flows

1. **App launch:** restore token, call `/api/auth/me`, render screens based on roles.
   A newly registered user has no token until an admin approves them.
2. **Passenger home:** call `/api/trips/today`, `/api/reservations/me`, and
   `/api/notifications/me`; subscribe to notification queue and selected trip location.
3. **Passenger preference form:** load routes and route stations, submit IDs to
   `/api/transport-preferences`.
4. **Bus Admin home:** call `/api/bus-admin/trips/me`; open a trip and load its
   TripStations. Edit only its actual TripStations.
5. **Live map:** subscribe to trip location topic and render the latest coordinates;
   use the REST location endpoint to populate the map before the first socket event.

## 9. Frontend implementation notes

- Treat `accessToken` as sensitive. Never log it or include it in URLs.
- A `TripResponseDto` identifies a daily execution. Its `routeId` identifies the
  reusable route template behind it.
- `locationStale` should show a clear UI message such as “Location has not been
  updated recently”; it is not the same as a missing trip.
- After a preference or reservation mutation, refetch the relevant list rather
  than guessing capacity/waitlist state on the client.
- When receiving a WebSocket notification, append it locally and increment unread
  count; on any reconnection, refetch from REST to recover messages delivered while offline.
