INSERT IGNORE INTO member (name, login_id, password, role, created_at)
VALUES ('admin', 'admin@gmail.com', '$2a$10$oPEmMyyJ9HrOmpiz85uaUuFYmYkiZEdo20xYJp5.EHIEu9MaVbKxS', 'ADMIN', NOW());