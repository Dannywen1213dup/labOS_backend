# ============================================================================
# ⚠️  WARNING: DEVELOPMENT ONLY - DO NOT USE IN PRODUCTION ⚠️
# ============================================================================
# This file creates a test user with default credentials for development
# purposes only. This should NEVER be included in production deployments.
#
# Test User Credentials:
#   Email: test@gmail.com
#   Password: test
#   Username: test
#
# This file is automatically executed during database initialization in
# development environments. Ensure this file is NOT included in production
# Docker Compose configurations or deployment scripts.
# ============================================================================

USE my_db;

-- Insert test user (only if it doesn't already exist)
INSERT INTO `user` (
    `userAccount`,
    `email`,
    `userPassword`,
    `firstName`,
    `lastName`,
    `userName`,
    `legalAccepted`,
    `status`,
    `userRole`,
    `isDelete`
) VALUES (
    'test@gmail.com',  -- userAccount (set to email for compatibility)
    'test@gmail.com',  -- email
    '06cbc63e39a5743db1c10db6ed3703f3',  -- MD5 hash of "Yifantest" (salt + password)
    'Test',            -- firstName
    'User',            -- lastName
    'Test User',       -- userName
    1,                 -- legalAccepted (true)
    'ACTIVE',          -- status (ACTIVE so user can login immediately)
    'user',            -- userRole
    0                  -- isDelete (false)
) ON DUPLICATE KEY UPDATE
    -- If user already exists, update to ensure correct status
    `status` = 'ACTIVE',
    `userPassword` = '06cbc63e39a5743db1c10db6ed3703f3';

