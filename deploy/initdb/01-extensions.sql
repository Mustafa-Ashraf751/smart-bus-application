-- Runs once, when the database volume is first created.
-- Replaces the postgis image's default init script, which installs PostGIS into "public".
-- The app expects PostGIS in the "extensions" schema (as on Supabase, see V13__enable_postgis.sql).
CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA extensions;
