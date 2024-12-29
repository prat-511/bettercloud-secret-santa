CREATE TABLE IF NOT EXISTS members (
    id BIGSERIAL PRIMARY KEY,
    family_id INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS edges (
    edge_id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    member_id BIGINT NOT NULL REFERENCES members(id)
);

CREATE TABLE IF NOT EXISTS assignments (
    id BIGSERIAL PRIMARY KEY,
    assignment_year INTEGER NOT NULL,
    giver_id BIGINT NOT NULL REFERENCES members(id),
    receiver_id BIGINT NOT NULL REFERENCES members(id)
);

CREATE INDEX IF NOT EXISTS idx_assignments_year ON assignments(assignment_year);
CREATE INDEX IF NOT EXISTS idx_edges_member ON edges(member_id);

-- Seed data
-- First clear any existing data
TRUNCATE TABLE assignments CASCADE;
TRUNCATE TABLE edges CASCADE;
TRUNCATE TABLE members CASCADE;


-- First insert the members
INSERT INTO members (family_id, name) VALUES
    (1, 'Adam'),    -- Will get id 1
    (1, 'Brian'),   -- Will get id 2
    (2, 'Cathy'),   -- Will get id 3
    (2, 'Dave'),    -- Will get id 4
    (3, 'Enid'),    -- Will get id 5
    (3, 'Fred');    -- Will get id 6

-- Then insert the edges, referencing the members
INSERT INTO edges (type, member_id)
SELECT 'IMMEDIATE_FAMILY', id FROM members WHERE family_id = 1
UNION ALL
SELECT 'IMMEDIATE_FAMILY', id FROM members WHERE family_id = 2
UNION ALL
SELECT 'IMMEDIATE_FAMILY', id FROM members WHERE family_id = 3;

COMMIT;