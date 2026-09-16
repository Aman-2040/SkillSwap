-- Run this ONLY if you already created the database WITHOUT password column.
-- If starting fresh, use skillswap_database.sql instead.

USE skillswap;

ALTER TABLE USERS ADD COLUMN password VARCHAR(50) NOT NULL DEFAULT 'changeme';

UPDATE USERS SET password = 'arjun123' WHERE email = 'arjun@email.com';
UPDATE USERS SET password = 'priya123' WHERE email = 'priya@email.com';
UPDATE USERS SET password = 'rohan123' WHERE email = 'rohan@email.com';
UPDATE USERS SET password = 'sneha123' WHERE email = 'sneha@email.com';
UPDATE USERS SET password = 'karan123' WHERE email = 'karan@email.com';
