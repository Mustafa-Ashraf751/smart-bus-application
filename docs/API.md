# Company Bus Transportation System — API Documentation

> **Purpose:** This document defines the API contract between the backend and frontend teams.
>
> The frontend should use the endpoints, request bodies, response bodies, status codes, and enum values documented here rather than depending directly on the database structure.
>
> **Status:** Initial API contract based on the current ERD. Update this file whenever an endpoint or DTO changes.

---

# 1. Overview

The Company Bus Transportation System allows employees to:

- Log in to the system.
- View available bus trips.
- View routes and trip stations.
- Reserve a seat on a trip.
- Cancel a reservation.
- View current and previous reservations.
- Manage transportation preferences.
- Track the bus location during an active trip.

Administrators can:

- Manage buses.
- Manage routes.
- Manage stations.
- Manage trips.
- Assign buses and drivers to trips.
- Manage roles and permissions.

Drivers can:

- View assigned trips.
- Start and complete trips.
- Send live bus-location updates.

---

# 2. Base URL

For local development:

```text
http://localhost:8080/api
```

Example:

```http
GET http://localhost:8080/api/trips
```

The production URL will be added when deployment is ready.

---

# 3. Content Type

Unless stated otherwise, requests and responses use JSON.

```http
Content-Type: application/json
```

---

# 4. Authentication

The API uses JWT authentication.

After login, the frontend receives an access token.

Protected endpoints require:

```http
Authorization: Bearer <access_token>
```

Example:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

The backend identifies the currently logged-in user from the JWT token.

Therefore, the frontend should **not send `userId`** for operations such as creating a reservation or retrieving the current user's preferences unless an admin endpoint explicitly requires it.

---

# 5. Common HTTP Status Codes

| Status | Meaning |
|---|---|
| `200 OK` | Request succeeded |
| `201 Created` | Resource created successfully |
| `204 No Content` | Request succeeded with no response body |
| `400 Bad Request` | Invalid request data |
| `401 Unauthorized` | Missing or invalid authentication token |
| `403 Forbidden` | User does not have permission |
| `404 Not Found` | Requested resource does not exist |
| `409 Conflict` | Request conflicts with current data/state |
| `422 Unprocessable Entity` | Business validation failed |
| `500 Internal Server Error` | Unexpected backend error |

---

# 6. Standard Error Response

All API errors should follow one consistent format.

```json
{
  "timestamp": "2026-09-28T14:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Trip not found",
  "path": "/api/trips/15"
}
```

Validation errors may additionally return field errors:

```json
{
  "timestamp": "2026-09-28T14:30:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid request data",
  "fieldErrors": {
    "email": "Email is invalid",
    "preferredTime": "Preferred time is required"
  }
}
```

---

# 7. Main Enums

The frontend should use these exact values unless the backend contract changes.

## User Status

```text
ACTIVE
INACTIVE
```

## Bus Status

```text
ACTIVE
INACTIVE
```

## Route Status

```text
ACTIVE
INACTIVE
```

## Station Status

```text
ACTIVE
INACTIVE
```

## Direction

```text
TO_COMPANY
FROM_COMPANY
```

## Trip Status

```text
SCHEDULED
IN_PROGRESS
COMPLETED
CANCELLED
```

## Reservation Status

```text
RESERVED
CANCELLED
COMPLETED
```

---

# 8. Authentication Endpoints

## 8.1 Login

Authenticate a user and return a JWT access token.

### Endpoint

```http
POST /auth/login
```

### Authentication

Not required.

### Request Body

```json
{
  "email": "employee@company.com",
  "password": "password123"
}
```

### Success Response

**200 OK**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "user": {
    "userId": 10,
    "name": "Toka Mohamed",
    "email": "employee@company.com",
    "phone": "01000000000",
    "status": "ACTIVE",
    "roles": [
      "EMPLOYEE"
    ],
    "permissions": [
      "TRIP_VIEW",
      "RESERVATION_CREATE"
    ]
  }
}
```

### Possible Errors

| Status | Reason |
|---|---|
| `400` | Missing login fields |
| `401` | Invalid email or password |
| `403` | User account is inactive |

---

## 8.2 Get Current User

Return information about the authenticated user.

### Endpoint

```http
GET /auth/me
```

### Authentication

Required.

### Success Response

**200 OK**

```json
{
  "userId": 10,
  "name": "Toka Mohamed",
  "phone": "01000000000",
  "address": "6th of October",
  "email": "employee@company.com",
  "status": "ACTIVE",
  "roles": [
    "EMPLOYEE"
  ],
  "permissions": [
    "TRIP_VIEW",
    "RESERVATION_CREATE"
  ]
}
```

---

# 9. Route Endpoints

## 9.1 Get Active Routes

Return routes available to users.

### Endpoint

```http
GET /routes
```

### Query Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `direction` | string | No | `TO_COMPANY` or `FROM_COMPANY` |
| `status` | string | No | Defaults to active routes for employee-facing screens |

### Example

```http
GET /routes?direction=TO_COMPANY
```

### Success Response

**200 OK**

```json
[
  {
    "routeId": 1,
    "name": "October - Smart Village",
    "direction": "TO_COMPANY",
    "status": "ACTIVE"
  },
  {
    "routeId": 2,
    "name": "Sheikh Zayed - Smart Village",
    "direction": "TO_COMPANY",
    "status": "ACTIVE"
  }
]
```

---

## 9.2 Get Route Details

### Endpoint

```http
GET /routes/{routeId}
```

### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `routeId` | Long | Route identifier |

### Success Response

**200 OK**

```json
{
  "routeId": 1,
  "name": "October - Smart Village",
  "direction": "TO_COMPANY",
  "status": "ACTIVE"
}
```

---

# 10. Station Endpoints

## 10.1 Get Stations

### Endpoint

```http
GET /stations
```

### Query Parameters

| Parameter | Type | Required |
|---|---|---|
| `status` | string | No |

### Success Response

**200 OK**

```json
[
  {
    "stationId": 5,
    "name": "Mall of Arabia",
    "address": "6th of October City",
    "latitude": 30.0074,
    "longitude": 30.9748,
    "status": "ACTIVE"
  }
]
```

---

## 10.2 Get Station Details

### Endpoint

```http
GET /stations/{stationId}
```

### Success Response

**200 OK**

```json
{
  "stationId": 5,
  "name": "Mall of Arabia",
  "address": "6th of October City",
  "latitude": 30.0074,
  "longitude": 30.9748,
  "status": "ACTIVE"
}
```

---

# 11. Trip Endpoints

## 11.1 Get Available Trips

Return trips available for the employee.

### Endpoint

```http
GET /trips
```

### Authentication

Required.

### Query Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `date` | date | No | Example: `2026-09-28` |
| `direction` | string | No | `TO_COMPANY` or `FROM_COMPANY` |
| `routeId` | Long | No | Filter by route |
| `status` | string | No | Filter by trip status |

### Example

```http
GET /trips?date=2026-09-28&direction=TO_COMPANY
```

### Success Response

**200 OK**

```json
[
  {
    "tripId": 15,
    "route": {
      "routeId": 1,
      "name": "October - Smart Village",
      "direction": "TO_COMPANY"
    },
    "bus": {
      "busId": 8,
      "busNumber": "BUS-12",
      "capacity": 40
    },
    "scheduledStartTime": "2026-09-28T07:30:00+03:00",
    "actualStartTime": null,
    "actualEndTime": null,
    "status": "SCHEDULED",
    "availableSeats": 12
  }
]
```

### Notes

`availableSeats` is calculated by the backend:

```text
Bus capacity - active reservations
```

It is not stored directly in the database.

---

## 11.2 Get Trip Details

### Endpoint

```http
GET /trips/{tripId}
```

### Authentication

Required.

### Success Response

**200 OK**

```json
{
  "tripId": 15,
  "route": {
    "routeId": 1,
    "name": "October - Smart Village",
    "direction": "TO_COMPANY"
  },
  "bus": {
    "busId": 8,
    "busNumber": "BUS-12",
    "capacity": 40
  },
  "driver": {
    "userId": 20,
    "name": "Ahmed Ali"
  },
  "scheduledStartTime": "2026-09-28T07:30:00+03:00",
  "actualStartTime": null,
  "actualEndTime": null,
  "status": "SCHEDULED",
  "availableSeats": 12
}
```

---

## 11.3 Get Stations for a Trip

Return the ordered pickup/drop-off stations belonging to a trip.

### Endpoint

```http
GET /trips/{tripId}/stations
```

### Authentication

Required.

### Success Response

**200 OK**

```json
[
  {
    "tripStationId": 41,
    "station": {
      "stationId": 5,
      "name": "Mall of Arabia",
      "address": "6th of October City",
      "latitude": 30.0074,
      "longitude": 30.9748
    },
    "stopOrder": 1
  },
  {
    "tripStationId": 42,
    "station": {
      "stationId": 7,
      "name": "Smart Village Gate",
      "address": "Smart Village",
      "latitude": 30.0717,
      "longitude": 30.9536
    },
    "stopOrder": 2
  }
]
```

### Frontend Important Note

When creating a reservation, send the `tripStationId`, not just the station ID.

This guarantees that the selected pickup station actually belongs to the selected trip.

---

# 12. Reservation Endpoints

## 12.1 Create Reservation

Reserve a seat on a trip for the currently authenticated employee.

### Endpoint

```http
POST /reservations
```

### Authentication

Required.

### Request Body

```json
{
  "tripId": 15,
  "pickupTripStationId": 41
}
```

### Frontend Should NOT Send

```text
userId
reservationId
status
reservedAt
```

These values are controlled by the backend.

### Success Response

**201 Created**

```json
{
  "reservationId": 101,
  "tripId": 15,
  "pickupStation": {
    "tripStationId": 41,
    "stationId": 5,
    "name": "Mall of Arabia"
  },
  "status": "RESERVED",
  "reservedAt": "2026-09-28T16:20:00+03:00"
}
```

### Possible Errors

| Status | Reason |
|---|---|
| `404` | Trip or trip station not found |
| `409` | User already reserved this trip |
| `409` | Bus has no available seats |
| `422` | Trip cannot currently be reserved |
| `422` | Selected pickup station does not belong to trip |

---

## 12.2 Get My Reservations

Return reservations belonging to the logged-in user.

### Endpoint

```http
GET /reservations/me
```

### Authentication

Required.

### Query Parameters

| Parameter | Type | Required |
|---|---|---|
| `status` | string | No |

### Example

```http
GET /reservations/me?status=RESERVED
```

### Success Response

**200 OK**

```json
[
  {
    "reservationId": 101,
    "status": "RESERVED",
    "reservedAt": "2026-09-28T16:20:00+03:00",
    "cancelledAt": null,
    "trip": {
      "tripId": 15,
      "scheduledStartTime": "2026-09-29T07:30:00+03:00",
      "status": "SCHEDULED",
      "route": {
        "routeId": 1,
        "name": "October - Smart Village",
        "direction": "TO_COMPANY"
      },
      "bus": {
        "busId": 8,
        "busNumber": "BUS-12"
      }
    },
    "pickupStation": {
      "tripStationId": 41,
      "stationId": 5,
      "name": "Mall of Arabia"
    }
  }
]
```

---

## 12.3 Get Reservation Details

### Endpoint

```http
GET /reservations/{reservationId}
```

### Authentication

Required.

### Success Response

**200 OK**

```json
{
  "reservationId": 101,
  "status": "RESERVED",
  "reservedAt": "2026-09-28T16:20:00+03:00",
  "cancelledAt": null,
  "trip": {
    "tripId": 15,
    "scheduledStartTime": "2026-09-29T07:30:00+03:00",
    "status": "SCHEDULED"
  },
  "pickupStation": {
    "tripStationId": 41,
    "stationId": 5,
    "name": "Mall of Arabia"
  }
}
```

---

## 12.4 Cancel Reservation

### Endpoint

```http
DELETE /reservations/{reservationId}
```

### Authentication

Required.

### Success Response

**204 No Content**

### Business Rules

The backend must verify:

- The reservation belongs to the logged-in employee unless the requester is an admin.
- The reservation is currently active.
- The trip has not reached a state where cancellation is forbidden.

---

# 13. Transport Preference Endpoints

A transport preference represents a user's normal transportation preference for a specific direction.

Example:

```text
TO_COMPANY
FROM_COMPANY
```

A preference uses a general station and route. It does not point to a specific trip.

---

## 13.1 Get My Transport Preferences

### Endpoint

```http
GET /transport-preferences/me
```

### Authentication

Required.

### Success Response

**200 OK**

```json
[
  {
    "preferenceId": 50,
    "direction": "TO_COMPANY",
    "preferredTime": "07:30:00",
    "route": {
      "routeId": 1,
      "name": "October - Smart Village"
    },
    "station": {
      "stationId": 5,
      "name": "Mall of Arabia"
    }
  }
]
```

---

## 13.2 Create Transport Preference

### Endpoint

```http
POST /transport-preferences
```

### Authentication

Required.

### Request Body

```json
{
  "routeId": 1,
  "stationId": 5,
  "direction": "TO_COMPANY",
  "preferredTime": "07:30:00"
}
```

### Success Response

**201 Created**

```json
{
  "preferenceId": 50,
  "direction": "TO_COMPANY",
  "preferredTime": "07:30:00",
  "route": {
    "routeId": 1,
    "name": "October - Smart Village"
  },
  "station": {
    "stationId": 5,
    "name": "Mall of Arabia"
  }
}
```

### Possible Errors

| Status | Reason |
|---|---|
| `404` | Route or station not found |
| `409` | User already has a preference for this direction |
| `422` | Invalid direction or preference data |

---

## 13.3 Update Transport Preference

### Endpoint

```http
PUT /transport-preferences/{preferenceId}
```

### Authentication

Required.

### Request Body

```json
{
  "routeId": 2,
  "stationId": 9,
  "preferredTime": "07:45:00"
}
```

### Success Response

**200 OK**

```json
{
  "preferenceId": 50,
  "direction": "TO_COMPANY",
  "preferredTime": "07:45:00",
  "route": {
    "routeId": 2,
    "name": "Sheikh Zayed - Smart Village"
  },
  "station": {
    "stationId": 9,
    "name": "Hyper One"
  }
}
```

---

## 13.4 Delete Transport Preference

### Endpoint

```http
DELETE /transport-preferences/{preferenceId}
```

### Authentication

Required.

### Success Response

**204 No Content**

---

# 14. Live Bus Tracking

The trip table stores the latest known bus coordinates:

```text
current_latitude
current_longitude
location_updated_at
```

The frontend may first load the current location through REST and then receive live updates through WebSocket.

---

## 14.1 Get Current Trip Location

### Endpoint

```http
GET /trips/{tripId}/location
```

### Authentication

Required.

### Success Response

**200 OK**

```json
{
  "tripId": 15,
  "latitude": 30.0457,
  "longitude": 31.0123,
  "updatedAt": "2026-09-28T07:48:12+03:00"
}
```

If no location has been sent yet:

```json
{
  "tripId": 15,
  "latitude": null,
  "longitude": null,
  "updatedAt": null
}
```

---

# 15. WebSocket / Real-Time Tracking

> Final WebSocket paths depend on the backend implementation. If Spring WebSocket + STOMP is used, the following contract is recommended.

## Connection Endpoint

```text
/ws
```

The frontend connects once to:

```text
ws://localhost:8080/ws
```

If SockJS is used, the frontend should use the SockJS/STOMP client configuration provided by the backend.

---

## 15.1 Subscribe to Trip Location

Frontend subscribes to:

```text
/topic/trips/{tripId}/location
```

Example:

```text
/topic/trips/15/location
```

### Message Received by Frontend

```json
{
  "tripId": 15,
  "latitude": 30.0457,
  "longitude": 31.0123,
  "updatedAt": "2026-09-28T07:48:12+03:00"
}
```

The frontend should update the bus marker on the map whenever a new message is received.

---

## 15.2 Driver Sends Location

Recommended driver destination:

```text
/app/trips/{tripId}/location
```

Example message:

```json
{
  "latitude": 30.0457,
  "longitude": 31.0123
}
```

Backend responsibilities:

1. Verify that the authenticated user is the driver assigned to the trip.
2. Verify that the trip is allowed to receive location updates.
3. Save the latest location in the `Trip`.
4. Set `location_updated_at`.
5. Broadcast the location to:

```text
/topic/trips/{tripId}/location
```

---

# 16. Driver Endpoints

## 16.1 Get My Assigned Trips

### Endpoint

```http
GET /driver/trips
```

### Authentication

Required.

### Required Role

```text
DRIVER
```

### Query Parameters

| Parameter | Type | Required |
|---|---|---|
| `date` | date | No |
| `status` | string | No |

### Success Response

**200 OK**

```json
[
  {
    "tripId": 15,
    "scheduledStartTime": "2026-09-28T07:30:00+03:00",
    "status": "SCHEDULED",
    "route": {
      "routeId": 1,
      "name": "October - Smart Village",
      "direction": "TO_COMPANY"
    },
    "bus": {
      "busId": 8,
      "busNumber": "BUS-12"
    }
  }
]
```

---

## 16.2 Start Trip

### Endpoint

```http
PATCH /driver/trips/{tripId}/start
```

### Authentication

Required.

### Required Role

```text
DRIVER
```

### Success Response

**200 OK**

```json
{
  "tripId": 15,
  "status": "IN_PROGRESS",
  "actualStartTime": "2026-09-28T07:32:00+03:00"
}
```

---

## 16.3 Complete Trip

### Endpoint

```http
PATCH /driver/trips/{tripId}/complete
```

### Authentication

Required.

### Required Role

```text
DRIVER
```

### Success Response

**200 OK**

```json
{
  "tripId": 15,
  "status": "COMPLETED",
  "actualEndTime": "2026-09-28T08:10:00+03:00"
}
```

---

# 17. Admin — Bus Endpoints

## 17.1 Get Buses

```http
GET /admin/buses
```

## 17.2 Get Bus by ID

```http
GET /admin/buses/{busId}
```

## 17.3 Create Bus

```http
POST /admin/buses
```

### Request Body

```json
{
  "busNumber": "BUS-12",
  "capacity": 40,
  "status": "ACTIVE"
}
```

### Success Response

**201 Created**

```json
{
  "busId": 8,
  "busNumber": "BUS-12",
  "capacity": 40,
  "status": "ACTIVE"
}
```

## 17.4 Update Bus

```http
PUT /admin/buses/{busId}
```

## 17.5 Deactivate Bus

Recommended approach:

```http
PATCH /admin/buses/{busId}/status
```

### Request

```json
{
  "status": "INACTIVE"
}
```

---

# 18. Admin — Route Endpoints

## 18.1 Create Route

```http
POST /admin/routes
```

### Request

```json
{
  "name": "October - Smart Village",
  "direction": "TO_COMPANY",
  "status": "ACTIVE"
}
```

## 18.2 Update Route

```http
PUT /admin/routes/{routeId}
```

## 18.3 Update Route Status

```http
PATCH /admin/routes/{routeId}/status
```

---

# 19. Admin — Station Endpoints

## 19.1 Create Station

```http
POST /admin/stations
```

### Request

```json
{
  "name": "Mall of Arabia",
  "address": "6th of October City",
  "latitude": 30.0074,
  "longitude": 30.9748,
  "status": "ACTIVE"
}
```

## 19.2 Update Station

```http
PUT /admin/stations/{stationId}
```

## 19.3 Update Station Status

```http
PATCH /admin/stations/{stationId}/status
```

---

# 20. Admin — Trip Endpoints

## 20.1 Create Trip

### Endpoint

```http
POST /admin/trips
```

### Request Body

```json
{
  "routeId": 1,
  "busId": 8,
  "driverId": 20,
  "scheduledStartTime": "2026-09-29T07:30:00+03:00",
  "stationIds": [
    5,
    7,
    10
  ]
}
```

The order of `stationIds` represents the stop order.

Alternative request structure if explicit ordering is preferred:

```json
{
  "routeId": 1,
  "busId": 8,
  "driverId": 20,
  "scheduledStartTime": "2026-09-29T07:30:00+03:00",
  "stations": [
    {
      "stationId": 5,
      "stopOrder": 1
    },
    {
      "stationId": 7,
      "stopOrder": 2
    },
    {
      "stationId": 10,
      "stopOrder": 3
    }
  ]
}
```

### Success Response

**201 Created**

```json
{
  "tripId": 16,
  "routeId": 1,
  "busId": 8,
  "driverId": 20,
  "scheduledStartTime": "2026-09-29T07:30:00+03:00",
  "status": "SCHEDULED"
}
```

---

## 20.2 Update Trip

```http
PUT /admin/trips/{tripId}
```

---

## 20.3 Cancel Trip

```http
PATCH /admin/trips/{tripId}/cancel
```

### Success Response

**200 OK**

```json
{
  "tripId": 15,
  "status": "CANCELLED"
}
```

---

# 21. Admin — User Endpoints

## 21.1 Get Users

```http
GET /admin/users
```

### Query Parameters

```text
status
role
search
```

## 21.2 Get User

```http
GET /admin/users/{userId}
```

## 21.3 Update User Status

```http
PATCH /admin/users/{userId}/status
```

### Request

```json
{
  "status": "INACTIVE"
}
```

---

# 22. Roles and Permissions

The ERD supports many-to-many relationships:

```text
User ↔ Role
Role ↔ Permission
```

The frontend should not manually determine permissions based only on the role name.

Instead, use the permissions returned from `/auth/me`.

Example:

```json
{
  "roles": [
    "ADMIN"
  ],
  "permissions": [
    "BUS_CREATE",
    "BUS_UPDATE",
    "TRIP_CREATE",
    "TRIP_UPDATE"
  ]
}
```

Example frontend rule:

```text
Show "Create Trip" button only if the user has TRIP_CREATE.
```

Backend authorization is still required even if the frontend hides unauthorized buttons.

---

# 23. Pagination

Endpoints that may return large collections should support pagination.

Recommended query parameters:

```text
?page=0&size=20
```

Example:

```http
GET /admin/users?page=0&size=20
```

Recommended response:

```json
{
  "content": [
    {
      "userId": 1,
      "name": "Employee One"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 65,
  "totalPages": 4
}
```

---

# 24. Date and Time Format

Use ISO-8601 format.

Example:

```text
2026-09-28T07:30:00+03:00
```

Date only:

```text
2026-09-28
```

Time only:

```text
07:30:00
```

Frontend should not assume a timezone if the API response already contains one.

---

# 25. Frontend Integration Flow

## Employee Reservation Flow

```text
1. POST /auth/login
        ↓
2. Store access token
        ↓
3. GET /trips
        ↓
4. User selects a trip
        ↓
5. GET /trips/{tripId}/stations
        ↓
6. User selects pickup station
        ↓
7. POST /reservations
        ↓
8. GET /reservations/me
```

---

## Live Tracking Flow

```text
1. User opens active-trip screen
        ↓
2. GET /trips/{tripId}/location
        ↓
3. Display latest known bus location
        ↓
4. Connect to /ws
        ↓
5. Subscribe to /topic/trips/{tripId}/location
        ↓
6. Update map whenever new coordinates arrive
```

---

## Transport Preference Flow

```text
1. GET /routes
        ↓
2. GET /stations
        ↓
3. User chooses route + station + direction + time
        ↓
4. POST /transport-preferences
        ↓
5. GET /transport-preferences/me
```

---

# 26. Backend Business Rules Relevant to Frontend

1. A user may have multiple roles.
2. A role may contain multiple permissions.
3. Every trip has exactly one route.
4. Every trip has exactly one bus.
5. Every trip has exactly one driver.
6. A trip contains ordered stations through `TripStation`.
7. A reservation belongs to one user and one trip.
8. A user may have many reservations but only one reservation per trip.
9. A reservation pickup point must reference a valid `TripStation`.
10. The selected pickup station must belong to the selected trip.
11. Seat availability is calculated from bus capacity and active reservations.
12. A transport preference uses a general station and is not tied to a specific trip.
13. The current live location belongs to the trip, not permanently to the bus.
14. The backend must enforce authorization even if frontend UI elements are hidden.

---

# 27. Endpoint Summary

## Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/login` | Login |
| GET | `/auth/me` | Current user |

## Routes

| Method | Endpoint | Description |
|---|---|---|
| GET | `/routes` | Get routes |
| GET | `/routes/{routeId}` | Get route |

## Stations

| Method | Endpoint | Description |
|---|---|---|
| GET | `/stations` | Get stations |
| GET | `/stations/{stationId}` | Get station |

## Trips

| Method | Endpoint | Description |
|---|---|---|
| GET | `/trips` | Get available trips |
| GET | `/trips/{tripId}` | Get trip |
| GET | `/trips/{tripId}/stations` | Get ordered trip stations |
| GET | `/trips/{tripId}/location` | Get latest bus location |

## Reservations

| Method | Endpoint | Description |
|---|---|---|
| POST | `/reservations` | Create reservation |
| GET | `/reservations/me` | Get my reservations |
| GET | `/reservations/{reservationId}` | Get reservation |
| DELETE | `/reservations/{reservationId}` | Cancel reservation |

## Transport Preferences

| Method | Endpoint | Description |
|---|---|---|
| GET | `/transport-preferences/me` | Get my preferences |
| POST | `/transport-preferences` | Create preference |
| PUT | `/transport-preferences/{id}` | Update preference |
| DELETE | `/transport-preferences/{id}` | Delete preference |

## Driver

| Method | Endpoint | Description |
|---|---|---|
| GET | `/driver/trips` | Get assigned trips |
| PATCH | `/driver/trips/{tripId}/start` | Start trip |
| PATCH | `/driver/trips/{tripId}/complete` | Complete trip |

## Admin

| Method | Endpoint | Description |
|---|---|---|
| GET | `/admin/buses` | Get buses |
| POST | `/admin/buses` | Create bus |
| PUT | `/admin/buses/{id}` | Update bus |
| PATCH | `/admin/buses/{id}/status` | Update bus status |
| POST | `/admin/routes` | Create route |
| PUT | `/admin/routes/{id}` | Update route |
| PATCH | `/admin/routes/{id}/status` | Update route status |
| POST | `/admin/stations` | Create station |
| PUT | `/admin/stations/{id}` | Update station |
| PATCH | `/admin/stations/{id}/status` | Update station status |
| POST | `/admin/trips` | Create trip |
| PUT | `/admin/trips/{id}` | Update trip |
| PATCH | `/admin/trips/{id}/cancel` | Cancel trip |
| GET | `/admin/users` | Get users |
| GET | `/admin/users/{id}` | Get user |
| PATCH | `/admin/users/{id}/status` | Update user status |

---

# 28. Important Team Rule

This document is the API contract between frontend and backend.

When an endpoint changes, such as:

- URL
- HTTP method
- Request body
- Response body
- Field name
- Enum
- Validation rule
- Authentication requirement

the backend team must update this document and inform the frontend team.

Example:

```text
Bad workflow:
Backend changes pickupStationId → pickupTripStationId
Frontend is not informed.

Good workflow:
Backend changes DTO
→ update API documentation
→ update Swagger/OpenAPI
→ notify frontend
→ frontend updates integration
```

---

# 29. Swagger / OpenAPI

The backend should also expose Swagger/OpenAPI documentation.

Typical Spring Boot Swagger path:

```text
/swagger-ui/index.html
```

The exact URL depends on project configuration.

Use:

- **Swagger/OpenAPI** for interactive technical testing.
- **This Markdown file** for the human-readable frontend/backend contract and application flow.

---

# 30. API Contract Status

Before frontend treats an endpoint as ready, the backend team should confirm all of the following:

```text
[ ] Endpoint implemented
[ ] DTO finalized
[ ] Validation implemented
[ ] Authentication/authorization implemented
[ ] Endpoint tested
[ ] Swagger updated
[ ] API.md updated
[ ] Frontend notified
```

---

**Project:** Company Bus Transportation System  
**Document:** API Documentation  
**Format:** Markdown (`.md`)
