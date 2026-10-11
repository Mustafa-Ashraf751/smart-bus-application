-- =====================================================================
-- VerySmartBus — admin account + demo data for frontend testing
--
-- Run on the VPS:
--   docker exec -i verysmartbus-db psql -U postgres -d smart_bus_db -v ON_ERROR_STOP=1 < seed/seed-demo-data.sql
--
-- * One transaction: either everything is inserted or nothing is.
-- * Safe to re-run: existing rows are kept; trips are (re)generated around TODAY (Africa/Cairo),
--   so re-running on a later day adds trips for the new dates.
-- * Demo records are tagged: users @demo.verysmartbus.com, buses DEMO-*, routes/stations "Demo: ...".
--   Remove them with seed/remove-demo-data.sql.
--
-- Accounts (login is by email):
--   admin@verysmartbus.com              / admin123   ADMIN
--   busadmin1..2@demo.verysmartbus.com  / demo123    BUS_ADMIN
--   driver1..5@demo.verysmartbus.com    / demo123    DRIVER
--   employee1..11@demo.verysmartbus.com / demo123    EMPLOYEE (employee11 is INACTIVE)
-- =====================================================================

BEGIN;

SET LOCAL search_path = public, extensions;
SET LOCAL client_encoding = 'UTF8';

-- ---------------------------------------------------------------------
-- 1. Roles & permissions (same as ReferenceDataSeeder + V27 BUS_ADMIN)
-- ---------------------------------------------------------------------
INSERT INTO permission (name, description) VALUES
    ('MANAGE_USERS',          'Create, update, deactivate users'),
    ('MANAGE_ROLES',          'Create, update roles and their permissions'),
    ('MANAGE_TRIPS',          'Create and schedule trips'),
    ('MANAGE_BUSES',          'Create, update, deactivate buses'),
    ('VIEW_ALL_RESERVATIONS', 'View reservations for any trip, not just your own'),
    ('BOOK_TRIP',             'Reserve a seat on a trip'),
    ('UPDATE_LIVE_LOCATION',  'Push live GPS location for an in-progress trip')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role (name, description) VALUES
    ('ADMIN',     'Full access to manage the system'),
    ('DRIVER',    'Can operate trips and update live location'),
    ('EMPLOYEE',  'Regular employee who books bus trips'),
    ('BUS_ADMIN', 'Operates assigned routes and daily trips')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
JOIN permission p
  ON r.name = 'ADMIN'
  OR (r.name = 'DRIVER'   AND p.name = 'UPDATE_LIVE_LOCATION')
  OR (r.name = 'EMPLOYEE' AND p.name = 'BOOK_TRIP')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- 2. Users
-- ---------------------------------------------------------------------
-- Admin: created, or password reset to admin123 if it already exists.
INSERT INTO users (name, phone, address, email, password_hash, status)
VALUES ('System Admin', '01000000000', 'HQ - Smart Village', 'admin@verysmartbus.com',
        '$2a$10$vwVIJ.U9Ox004iWs/iid.efyLy3fnIwcaV/JqVzV3.WwcYPvqgr62', 'ACTIVE')
ON CONFLICT (email) DO UPDATE
    SET password_hash = EXCLUDED.password_hash,
        status        = 'ACTIVE',
        updated_at    = CURRENT_TIMESTAMP;

CREATE TEMP TABLE demo_users (name text, phone text, address text, email text, role_name text, status text) ON COMMIT DROP;
INSERT INTO demo_users VALUES
    ('Amr Galal',          '01011110001', 'Smart Village',      'busadmin1@demo.verysmartbus.com', 'BUS_ADMIN', 'ACTIVE'),
    ('Dina Sherif',        '01011110002', 'Smart Village',      'busadmin2@demo.verysmartbus.com', 'BUS_ADMIN', 'ACTIVE'),
    ('Hassan Abdelrahman', '01022220001', 'Shubra, Cairo',      'driver1@demo.verysmartbus.com',   'DRIVER',    'ACTIVE'),
    ('Mohamed Saad',       '01022220002', 'Imbaba, Giza',       'driver2@demo.verysmartbus.com',   'DRIVER',    'ACTIVE'),
    ('Tamer Fawzy',        '01022220003', '6th of October',     'driver3@demo.verysmartbus.com',   'DRIVER',    'ACTIVE'),
    ('Sayed Ramadan',      '01022220004', 'Ain Shams, Cairo',   'driver4@demo.verysmartbus.com',   'DRIVER',    'ACTIVE'),
    ('Walid Nabil',        '01022220005', 'Helwan, Cairo',      'driver5@demo.verysmartbus.com',   'DRIVER',    'ACTIVE'),
    ('Ahmed Hassan',       '01133330001', 'Downtown, Cairo',    'employee1@demo.verysmartbus.com', 'EMPLOYEE',  'ACTIVE'),
    ('Mona Ali',           '01133330002', 'Dokki, Giza',        'employee2@demo.verysmartbus.com', 'EMPLOYEE',  'ACTIVE'),
    ('Omar Khaled',        '01133330003', 'Faisal, Giza',       'employee3@demo.verysmartbus.com', 'EMPLOYEE',  'ACTIVE'),
    ('Sara Mahmoud',       '01133330004', 'Haram, Giza',        'employee4@demo.verysmartbus.com', 'EMPLOYEE',  'ACTIVE'),
    ('Youssef Ibrahim',    '01133330005', '6th of October',     'employee5@demo.verysmartbus.com', 'EMPLOYEE',  'ACTIVE'),
    ('Nour Adel',          '01133330006', 'Sheikh Zayed',       'employee6@demo.verysmartbus.com', 'EMPLOYEE',  'ACTIVE'),
    ('Laila Mostafa',      '01133330007', 'Heliopolis, Cairo',  'employee7@demo.verysmartbus.com', 'EMPLOYEE',  'ACTIVE'),
    ('Karim Samir',        '01133330008', 'Nasr City, Cairo',   'employee8@demo.verysmartbus.com', 'EMPLOYEE',  'ACTIVE'),
    ('Hana Tarek',         '01133330009', 'Ramses, Cairo',      'employee9@demo.verysmartbus.com', 'EMPLOYEE',  'ACTIVE'),
    ('Mahmoud Fathy',      '01133330010', 'Mohandessin, Giza',  'employee10@demo.verysmartbus.com','EMPLOYEE',  'ACTIVE'),
    ('Reem Saeed',         '01133330011', 'Maadi, Cairo',       'employee11@demo.verysmartbus.com','EMPLOYEE',  'INACTIVE');

INSERT INTO users (name, phone, address, email, password_hash, status)
SELECT name, phone, address, email, '$2a$10$tTCII59YMuXbvvlEZYU2zOEdjLrg/Q6voWTjHs6NpyvFnwkWsIDPO', status
FROM demo_users
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM (SELECT email, role_name FROM demo_users
      UNION ALL SELECT 'admin@verysmartbus.com', 'ADMIN') d
JOIN users u ON u.email = d.email
JOIN role  r ON r.name  = d.role_name
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- 3. Buses (DEMO-05 is inactive)
-- ---------------------------------------------------------------------
INSERT INTO buses (bus_number, capacity, status) VALUES
    ('DEMO-01', 40, 'ACTIVE'),
    ('DEMO-02', 28, 'ACTIVE'),
    ('DEMO-03', 14, 'ACTIVE'),
    ('DEMO-04', 50, 'ACTIVE'),
    ('DEMO-05', 30, 'INACTIVE')
ON CONFLICT (bus_number) DO NOTHING;

-- ---------------------------------------------------------------------
-- 4. Stations — 50 m pickup area around each point, like StationRepositoryImpl
-- ---------------------------------------------------------------------
CREATE TEMP TABLE demo_stations (code text, name text, address text, lat numeric, lng numeric, status text) ON COMMIT DROP;
INSERT INTO demo_stations VALUES
    ('SV',  'Demo: Smart Village (Company HQ)',       'Smart Village, Cairo-Alex Desert Rd, Giza', 30.071700, 31.017600, 'ACTIVE'),
    ('RAM', 'Demo: Ramses Square',                    'Ramses Sq, Downtown Cairo',                 30.062600, 31.246900, 'ACTIVE'),
    ('TAH', 'Demo: Tahrir Square',                    'Tahrir Sq, Downtown Cairo',                 30.044400, 31.235700, 'ACTIVE'),
    ('DOK', 'Demo: Dokki Metro',                      'Tahrir St, Dokki, Giza',                    30.038400, 31.212100, 'ACTIVE'),
    ('MOH', 'Demo: Mohandessin - Gameat El Dowal',    'Gameat El Dowal St, Mohandessin, Giza',     30.056100, 31.200100, 'ACTIVE'),
    ('FAI', 'Demo: Faisal - Talbia',                  'Faisal St, Giza',                           30.017200, 31.180600, 'ACTIVE'),
    ('HAR', 'Demo: Haram - Mariouteya',               'Al Haram St, Giza',                         29.994800, 31.155700, 'ACTIVE'),
    ('HOS', 'Demo: 6th October - El Hosary',          'El Hosary Sq, 6th of October City',         29.972900, 30.944700, 'ACTIVE'),
    ('ARK', 'Demo: Sheikh Zayed - Arkan',             'Arkan Plaza, Sheikh Zayed',                 30.019500, 30.980000, 'ACTIVE'),
    ('KOR', 'Demo: Heliopolis - Korba',               'Baghdad St, Korba, Heliopolis',             30.090500, 31.324000, 'ACTIVE'),
    ('AKK', 'Demo: Nasr City - Abbas El Akkad',       'Abbas El Akkad St, Nasr City',              30.060500, 31.336900, 'ACTIVE'),
    ('MAA', 'Demo: Maadi - Degla',                    'Road 9, Maadi, Cairo',                      29.960200, 31.256900, 'ACTIVE'),
    ('NEW', 'Demo: New Cairo - 90th Street',          '90th St, Fifth Settlement, New Cairo',      30.020600, 31.475300, 'INACTIVE');

INSERT INTO stations (name, address, status, area)
SELECT d.name, d.address, d.status,
       extensions.ST_Buffer(
           extensions.ST_SetSRID(extensions.ST_MakePoint(d.lng, d.lat), 4326)::extensions.geography,
           50
       )::extensions.geometry
FROM demo_stations d
WHERE NOT EXISTS (SELECT 1 FROM stations s WHERE s.name = d.name);

-- ---------------------------------------------------------------------
-- 5. Routes + ordered stops (R6 is an incomplete, inactive draft: no defaults, so no trips)
-- ---------------------------------------------------------------------
CREATE TEMP TABLE demo_routes (code text, name text, status text, direction text, departure time,
                               bus_number text, driver_email text, bus_admin_email text) ON COMMIT DROP;
INSERT INTO demo_routes VALUES
    ('R1', 'Demo: Downtown → Smart Village',                'ACTIVE',   'TO_COMPANY',   '07:00', 'DEMO-01', 'driver1@demo.verysmartbus.com', 'busadmin1@demo.verysmartbus.com'),
    ('R2', 'Demo: Smart Village → Downtown',                'ACTIVE',   'FROM_COMPANY', '17:00', 'DEMO-01', 'driver1@demo.verysmartbus.com', 'busadmin1@demo.verysmartbus.com'),
    ('R3', 'Demo: Giza → Smart Village',                    'ACTIVE',   'TO_COMPANY',   '07:15', 'DEMO-02', 'driver2@demo.verysmartbus.com', 'busadmin1@demo.verysmartbus.com'),
    ('R4', 'Demo: October & Zayed → Smart Village',         'ACTIVE',   'TO_COMPANY',   '07:30', 'DEMO-03', 'driver3@demo.verysmartbus.com', 'busadmin2@demo.verysmartbus.com'),
    ('R5', 'Demo: Heliopolis & Nasr City → Smart Village',  'ACTIVE',   'TO_COMPANY',   '06:30', 'DEMO-04', 'driver4@demo.verysmartbus.com', 'busadmin2@demo.verysmartbus.com'),
    ('R6', 'Demo: Maadi → Smart Village (draft)',           'INACTIVE', NULL,           NULL,    NULL,      NULL,                            NULL);

CREATE TEMP TABLE demo_route_stops (route_code text, station_code text, stop_order int, arrival time) ON COMMIT DROP;
INSERT INTO demo_route_stops VALUES
    ('R1', 'RAM', 1, '07:00'), ('R1', 'TAH', 2, '07:10'), ('R1', 'DOK', 3, '07:20'), ('R1', 'MOH', 4, '07:30'), ('R1', 'SV', 5, '08:00'),
    ('R2', 'SV',  1, '17:00'), ('R2', 'MOH', 2, '17:30'), ('R2', 'DOK', 3, '17:40'), ('R2', 'TAH', 4, '17:50'), ('R2', 'RAM', 5, '18:00'),
    ('R3', 'FAI', 1, '07:15'), ('R3', 'HAR', 2, '07:30'), ('R3', 'SV',  3, '08:00'),
    ('R4', 'HOS', 1, '07:30'), ('R4', 'ARK', 2, '07:45'), ('R4', 'SV',  3, '08:05'),
    ('R5', 'KOR', 1, '06:30'), ('R5', 'AKK', 2, '06:45'), ('R5', 'SV',  3, '07:45'),
    ('R6', 'MAA', 1, '07:00'), ('R6', 'SV',  2, '08:00');

INSERT INTO routes (name, status, direction, default_departure_time, default_bus_id, default_driver_id, default_bus_admin_id)
SELECT d.name, d.status, d.direction, d.departure, b.id, drv.user_id, adm.user_id
FROM demo_routes d
LEFT JOIN buses b   ON b.bus_number = d.bus_number
LEFT JOIN users drv ON drv.email    = d.driver_email
LEFT JOIN users adm ON adm.email    = d.bus_admin_email
ON CONFLICT (name) DO NOTHING;

INSERT INTO route_stations (route_id, station_id, stop_order, expected_arrival_time)
SELECT r.id, s.id, rs.stop_order, rs.arrival
FROM demo_route_stops rs
JOIN demo_routes dr   ON dr.code = rs.route_code
JOIN routes r         ON r.name  = dr.name
JOIN demo_stations ds ON ds.code = rs.station_code
JOIN stations s       ON s.name  = ds.name
ON CONFLICT DO NOTHING;

-- Route path = line through the stop centres, in stop order
UPDATE routes r
SET path = p.line, updated_at = CURRENT_TIMESTAMP
FROM (
    SELECT rs.route_id,
           extensions.ST_MakeLine(extensions.ST_Centroid(s.area) ORDER BY rs.stop_order) AS line
    FROM route_stations rs
    JOIN stations s ON s.id = rs.station_id
    GROUP BY rs.route_id
    HAVING COUNT(*) >= 2
) p
WHERE p.route_id = r.id
  AND r.name IN (SELECT name FROM demo_routes)
  AND r.path IS NULL;

-- ---------------------------------------------------------------------
-- 6. Daily trips around today (Africa/Cairo), like TripService.create:
--    yesterday = COMPLETED, today = R1 IN_PROGRESS / others SCHEDULED,
--    next 3 days = SCHEDULED, except R3 in 3 days = CANCELLED
-- ---------------------------------------------------------------------
CREATE TEMP TABLE demo_today ON COMMIT DROP AS
SELECT (now() AT TIME ZONE 'Africa/Cairo')::date AS today;

INSERT INTO trips (route_id, bus_id, driver_id, bus_admin_id, service_date, scheduled_start_time,
                   actual_start_time, actual_end_time, status)
SELECT r.id, r.default_bus_id, r.default_driver_id, r.default_bus_admin_id,
       t.today + o.day_offset,
       ((t.today + o.day_offset) + r.default_departure_time) AT TIME ZONE 'Africa/Cairo',
       CASE
           WHEN o.day_offset < 0 THEN (((t.today + o.day_offset) + r.default_departure_time) AT TIME ZONE 'Africa/Cairo') + INTERVAL '3 minutes'
           WHEN o.day_offset = 0 AND dr.code = 'R1' THEN now() - INTERVAL '15 minutes'
       END,
       CASE
           WHEN o.day_offset < 0 THEN (((t.today + o.day_offset) + r.default_departure_time) AT TIME ZONE 'Africa/Cairo') + INTERVAL '70 minutes'
       END,
       CASE
           WHEN o.day_offset < 0 THEN 'COMPLETED'
           WHEN o.day_offset = 0 AND dr.code = 'R1' THEN 'IN_PROGRESS'
           WHEN o.day_offset = 3 AND dr.code = 'R3' THEN 'CANCELLED'
           ELSE 'SCHEDULED'
       END
FROM demo_routes dr
JOIN routes r ON r.name = dr.name
CROSS JOIN demo_today t
CROSS JOIN generate_series(-1, 3) AS o(day_offset)
WHERE r.direction IS NOT NULL
  AND r.default_departure_time IS NOT NULL
  AND r.default_bus_id IS NOT NULL
  AND r.default_driver_id IS NOT NULL
  AND r.default_bus_admin_id IS NOT NULL
ON CONFLICT (route_id, service_date) DO NOTHING;

-- Trip stations copied from the route (only for trips that have none yet).
-- Tomorrow's R5 trip skips its 2nd stop, to show the SKIPPED state.
INSERT INTO trip_stations (trip_id, station_id, stop_order, expected_arrival_time, status)
SELECT tr.id, rs.station_id, rs.stop_order, rs.expected_arrival_time,
       CASE WHEN dr.code = 'R5' AND tr.service_date = t.today + 1 AND rs.stop_order = 2
            THEN 'SKIPPED' ELSE 'ACTIVE' END
FROM trips tr
JOIN routes r          ON r.id = tr.route_id
JOIN demo_routes dr    ON dr.name = r.name
JOIN route_stations rs ON rs.route_id = r.id
CROSS JOIN demo_today t
WHERE NOT EXISTS (SELECT 1 FROM trip_stations x WHERE x.trip_id = tr.id);

-- Today's in-progress R1 trip: bus is inside the Tahrir pickup area right now,
-- Ramses arrival already confirmed (2 consecutive updates), Tahrir seen once.
UPDATE trips tr
SET current_location    = extensions.ST_Centroid(s.area),
    location_updated_at = now(),
    updated_at          = CURRENT_TIMESTAMP
FROM routes r, stations s, demo_today t
WHERE r.id = tr.route_id
  AND r.name = 'Demo: Downtown → Smart Village'
  AND s.name = 'Demo: Tahrir Square'
  AND tr.service_date = t.today
  AND tr.status = 'IN_PROGRESS'
  AND tr.current_location IS NULL;

INSERT INTO trip_station_presence (trip_id, station_id, consecutive_inside_count, arrival_confirmed, last_observed_at)
SELECT tr.id, s.id, v.cnt, v.confirmed, now() - v.ago
FROM (VALUES ('Demo: Ramses Square', 2, TRUE,  INTERVAL '10 minutes'),
             ('Demo: Tahrir Square', 1, FALSE, INTERVAL '0 minutes')) AS v(station_name, cnt, confirmed, ago)
JOIN stations s ON s.name = v.station_name
JOIN trips tr   ON tr.status = 'IN_PROGRESS'
JOIN routes r   ON r.id = tr.route_id AND r.name = 'Demo: Downtown → Smart Village'
JOIN demo_today t ON tr.service_date = t.today
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- 7. Transport preferences (one per user per direction)
-- ---------------------------------------------------------------------
CREATE TEMP TABLE demo_prefs (email text, direction text, route_code text, station_code text) ON COMMIT DROP;
INSERT INTO demo_prefs VALUES
    ('employee1@demo.verysmartbus.com',  'TO_COMPANY',   'R1', 'TAH'),
    ('employee1@demo.verysmartbus.com',  'FROM_COMPANY', 'R2', 'TAH'),
    ('employee2@demo.verysmartbus.com',  'TO_COMPANY',   'R1', 'DOK'),
    ('employee2@demo.verysmartbus.com',  'FROM_COMPANY', 'R2', 'DOK'),
    ('employee3@demo.verysmartbus.com',  'TO_COMPANY',   'R3', 'FAI'),
    ('employee4@demo.verysmartbus.com',  'TO_COMPANY',   'R3', 'HAR'),
    ('employee4@demo.verysmartbus.com',  'FROM_COMPANY', 'R2', 'MOH'),
    ('employee5@demo.verysmartbus.com',  'TO_COMPANY',   'R4', 'HOS'),
    ('employee6@demo.verysmartbus.com',  'TO_COMPANY',   'R4', 'ARK'),
    ('employee7@demo.verysmartbus.com',  'TO_COMPANY',   'R5', 'KOR'),
    ('employee7@demo.verysmartbus.com',  'FROM_COMPANY', 'R2', 'RAM'),
    ('employee8@demo.verysmartbus.com',  'TO_COMPANY',   'R5', 'AKK'),
    ('employee9@demo.verysmartbus.com',  'TO_COMPANY',   'R1', 'RAM'),
    ('employee10@demo.verysmartbus.com', 'TO_COMPANY',   'R1', 'MOH');

INSERT INTO transport_preference (user_id, route_id, station_id, direction, active)
SELECT u.user_id, r.id, s.id, p.direction, TRUE
FROM demo_prefs p
JOIN users u          ON u.email = p.email
JOIN demo_routes dr   ON dr.code = p.route_code
JOIN routes r         ON r.name  = dr.name
JOIN demo_stations ds ON ds.code = p.station_code
JOIN stations s       ON s.name  = ds.name
ON CONFLICT (user_id, direction) DO NOTHING;

-- ---------------------------------------------------------------------
-- 8. Reservations — the app keeps ONE reservation row per user (it is reused on re-booking).
--    employee8..10 are left without a reservation so they can book from the frontend.
-- ---------------------------------------------------------------------
CREATE TEMP TABLE demo_reservations (email text, route_code text, day_offset int, station_code text, status text) ON COMMIT DROP;
INSERT INTO demo_reservations VALUES
    ('employee1@demo.verysmartbus.com', 'R1', 0, 'TAH', 'RESERVED'),
    ('employee2@demo.verysmartbus.com', 'R1', 0, 'DOK', 'RESERVED'),
    ('employee3@demo.verysmartbus.com', 'R3', 1, 'FAI', 'RESERVED'),
    ('employee4@demo.verysmartbus.com', 'R3', 1, 'HAR', 'RESERVED'),
    ('employee5@demo.verysmartbus.com', 'R4', 1, 'HOS', 'RESERVED'),
    ('employee6@demo.verysmartbus.com', 'R4', 1, 'ARK', 'RESERVED'),
    ('employee7@demo.verysmartbus.com', 'R5', 1, 'KOR', 'CANCELLED');

INSERT INTO reservation (user_id, trip_id, pickup_trip_station_id, status, reserved_at, cancelled_at)
SELECT u.user_id, tr.id, ts.id, d.status,
       now() - INTERVAL '1 day',
       CASE WHEN d.status = 'CANCELLED' THEN now() - INTERVAL '2 hours' END
FROM demo_reservations d
JOIN users u          ON u.email = d.email
JOIN demo_routes dr   ON dr.code = d.route_code
JOIN routes r         ON r.name  = dr.name
CROSS JOIN demo_today t
JOIN trips tr         ON tr.route_id = r.id AND tr.service_date = t.today + d.day_offset
JOIN demo_stations ds ON ds.code = d.station_code
JOIN stations s       ON s.name  = ds.name
JOIN trip_stations ts ON ts.trip_id = tr.id AND ts.station_id = s.id
WHERE NOT EXISTS (SELECT 1 FROM reservation x WHERE x.user_id = u.user_id)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- 9. Service zone — Greater Cairo + Giza + 6th of October
-- ---------------------------------------------------------------------
INSERT INTO service_zones (name, area)
SELECT 'Demo: Greater Cairo',
       extensions.ST_GeomFromText('POLYGON((30.85 29.90, 31.55 29.90, 31.55 30.20, 30.85 30.20, 30.85 29.90))', 4326)
WHERE NOT EXISTS (SELECT 1 FROM service_zones WHERE name = 'Demo: Greater Cairo');

COMMIT;

-- ---------------------------------------------------------------------
-- Summary
-- ---------------------------------------------------------------------
SELECT 'users (demo + admin)' AS item, COUNT(*) AS total FROM users
 WHERE email LIKE '%@demo.verysmartbus.com' OR email = 'admin@verysmartbus.com'
UNION ALL SELECT 'buses',          COUNT(*) FROM buses    WHERE bus_number LIKE 'DEMO-%'
UNION ALL SELECT 'stations',       COUNT(*) FROM stations WHERE name LIKE 'Demo: %'
UNION ALL SELECT 'routes',         COUNT(*) FROM routes   WHERE name LIKE 'Demo: %'
UNION ALL SELECT 'route stops',    COUNT(*) FROM route_stations rs JOIN routes r ON r.id = rs.route_id WHERE r.name LIKE 'Demo: %'
UNION ALL SELECT 'trips',          COUNT(*) FROM trips tr JOIN routes r ON r.id = tr.route_id WHERE r.name LIKE 'Demo: %'
UNION ALL SELECT 'trip stops',     COUNT(*) FROM trip_stations ts JOIN trips tr ON tr.id = ts.trip_id JOIN routes r ON r.id = tr.route_id WHERE r.name LIKE 'Demo: %'
UNION ALL SELECT 'preferences',    COUNT(*) FROM transport_preference tp JOIN users u ON u.user_id = tp.user_id WHERE u.email LIKE '%@demo.verysmartbus.com'
UNION ALL SELECT 'reservations',   COUNT(*) FROM reservation rv JOIN users u ON u.user_id = rv.user_id WHERE u.email LIKE '%@demo.verysmartbus.com';
