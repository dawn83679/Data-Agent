-- Remove device_info column from sys_sessions table
-- This field is redundant as device information can be parsed from user_agent field
ALTER TABLE sys_sessions DROP COLUMN device_info;
