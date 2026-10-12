-- =====================================================================
-- Removes everything created by seed-demo-data.sql (plus anything testers created on top of it:
-- trips on demo routes, reservations of demo users, ...). Real data is not touched.
-- The admin account (admin@verysmartbus.com) and roles/permissions are kept.
--
--   docker exec -i verysmartbus-db psql -U postgres -d smart_bus_db -v ON_ERROR_STOP=1 < seed/remove-demo-data.sql
-- =====================================================================

BEGIN;

CREATE TEMP TABLE d_users    ON COMMIT DROP AS SELECT user_id FROM users    WHERE email LIKE '%@demo.verysmartbus.com';
CREATE TEMP TABLE d_buses    ON COMMIT DROP AS SELECT id      FROM buses    WHERE bus_number LIKE 'DEMO-%';
CREATE TEMP TABLE d_stations ON COMMIT DROP AS SELECT id      FROM stations WHERE name LIKE 'Demo: %';
CREATE TEMP TABLE d_routes   ON COMMIT DROP AS SELECT id      FROM routes   WHERE name LIKE 'Demo: %';
CREATE TEMP TABLE d_trips    ON COMMIT DROP AS
SELECT id FROM trips
WHERE route_id     IN (SELECT id FROM d_routes)
   OR bus_id       IN (SELECT id FROM d_buses)
   OR driver_id    IN (SELECT user_id FROM d_users)
   OR bus_admin_id IN (SELECT user_id FROM d_users);

DELETE FROM reservation
WHERE user_id IN (SELECT user_id FROM d_users)
   OR trip_id IN (SELECT id FROM d_trips)
   OR pickup_trip_station_id IN (SELECT id FROM trip_stations
                                 WHERE trip_id IN (SELECT id FROM d_trips)
                                    OR station_id IN (SELECT id FROM d_stations));

DELETE FROM trip_station_presence
WHERE trip_id IN (SELECT id FROM d_trips) OR station_id IN (SELECT id FROM d_stations);

DELETE FROM trip_stations
WHERE trip_id IN (SELECT id FROM d_trips) OR station_id IN (SELECT id FROM d_stations);

DELETE FROM trips WHERE id IN (SELECT id FROM d_trips);

DELETE FROM transport_preference
WHERE user_id    IN (SELECT user_id FROM d_users)
   OR route_id   IN (SELECT id FROM d_routes)
   OR station_id IN (SELECT id FROM d_stations);

DELETE FROM route_stations
WHERE route_id IN (SELECT id FROM d_routes) OR station_id IN (SELECT id FROM d_stations);

-- Real routes that point at demo bus/driver/bus admin as defaults lose those defaults
UPDATE routes SET default_bus_id = NULL       WHERE default_bus_id       IN (SELECT id FROM d_buses)       AND id NOT IN (SELECT id FROM d_routes);
UPDATE routes SET default_driver_id = NULL    WHERE default_driver_id    IN (SELECT user_id FROM d_users)  AND id NOT IN (SELECT id FROM d_routes);
UPDATE routes SET default_bus_admin_id = NULL WHERE default_bus_admin_id IN (SELECT user_id FROM d_users)  AND id NOT IN (SELECT id FROM d_routes);

DELETE FROM routes    WHERE id IN (SELECT id FROM d_routes);
DELETE FROM user_role WHERE user_id IN (SELECT user_id FROM d_users);
DELETE FROM users     WHERE user_id IN (SELECT user_id FROM d_users);
DELETE FROM buses     WHERE id IN (SELECT id FROM d_buses);
DELETE FROM stations  WHERE id IN (SELECT id FROM d_stations);
DELETE FROM service_zones WHERE name LIKE 'Demo: %';

COMMIT;

SELECT 'demo data removed' AS result;
