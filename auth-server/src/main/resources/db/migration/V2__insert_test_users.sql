-- Insert test users (password is 'password' encrypted with BCrypt)
-- BCrypt hash for 'password': $2a$10$spsMKAjD9VLrGsKYRTU4COa1GL68JGI4VqcoeScVFbJ8Ywn9KqDDW

INSERT INTO auth.users (username, email, password, enabled, created_at)
SELECT 'admin', 'admin@bank.com', '$2a$10$spsMKAjD9VLrGsKYRTU4COa1GL68JGI4VqcoeScVFbJ8Ywn9KqDDW', true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM auth.users WHERE username = 'admin');

INSERT INTO auth.users (username, email, password, enabled, created_at)
SELECT 'tester', 'tester@bank.com', '$2a$10$spsMKAjD9VLrGsKYRTU4COa1GL68JGI4VqcoeScVFbJ8Ywn9KqDDW', true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM auth.users WHERE username = 'tester');

-- Add role
INSERT INTO auth.user_roles (user_id, role)
SELECT u.id, 'ADMIN'
FROM auth.users u
WHERE u.username = 'admin'
  AND NOT EXISTS (
        SELECT 1 FROM auth.user_roles r WHERE r.user_id = u.id AND r.role = 'ADMIN'
    );

INSERT INTO auth.user_roles (user_id, role)
SELECT u.id, 'USER'
FROM auth.users u
WHERE u.username = 'tester'
  AND NOT EXISTS (
        SELECT 1 FROM auth.user_roles r WHERE r.user_id = u.id AND r.role = 'USER'
    );
