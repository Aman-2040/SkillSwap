CREATE DATABASE IF NOT EXISTS skillswap;
USE skillswap;

-- USERS (password required for login in the Java app)
CREATE TABLE IF NOT EXISTS USERS (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    city VARCHAR(100) NOT NULL
);

-- SKILLS
CREATE TABLE IF NOT EXISTS SKILLS (
    skill_id INT PRIMARY KEY AUTO_INCREMENT,
    skill_name VARCHAR(100) NOT NULL UNIQUE
);

-- USER SKILLS
CREATE TABLE IF NOT EXISTS USER_SKILLS (
    user_id INT NOT NULL,
    skill_id INT NOT NULL,
    PRIMARY KEY (user_id, skill_id),
    FOREIGN KEY (user_id) REFERENCES USERS(user_id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES SKILLS(skill_id) ON DELETE CASCADE
);

-- USER WANTS
CREATE TABLE IF NOT EXISTS USER_WANTS (
    user_id INT NOT NULL,
    skill_id INT NOT NULL,
    PRIMARY KEY (user_id, skill_id),
    FOREIGN KEY (user_id) REFERENCES USERS(user_id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES SKILLS(skill_id) ON DELETE CASCADE
);

-- REQUESTS
CREATE TABLE IF NOT EXISTS REQUESTS (
    request_id INT PRIMARY KEY AUTO_INCREMENT,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    skill_offered INT NOT NULL,
    skill_requested INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Pending',

    FOREIGN KEY (sender_id) REFERENCES USERS(user_id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES USERS(user_id) ON DELETE CASCADE,
    FOREIGN KEY (skill_offered) REFERENCES SKILLS(skill_id) ON DELETE CASCADE,
    FOREIGN KEY (skill_requested) REFERENCES SKILLS(skill_id) ON DELETE CASCADE,

    CHECK (sender_id <> receiver_id),
    CHECK (status IN ('Pending','Accepted','Rejected'))
);

-- SAMPLE USERS (password is plain text for college demo only)
INSERT IGNORE INTO USERS (name, email, password, city) VALUES
('Arjun Sharma', 'arjun@email.com', 'arjun123', 'Bangalore'),
('Priya Mehta', 'priya@email.com', 'priya123', 'Mumbai'),
('Rohan Verma', 'rohan@email.com', 'rohan123', 'Delhi'),
('Sneha Patil', 'sneha@email.com', 'sneha123', 'Pune'),
('Karan Singh', 'karan@email.com', 'karan123', 'Hyderabad');

-- SAMPLE SKILLS
INSERT IGNORE INTO SKILLS (skill_name) VALUES
('Java Programming'),
('Python Programming'),
('Web Development'),
('Graphic Design'),
('Data Analysis'),
('Machine Learning'),
('Photography'),
('Video Editing'),
('Public Speaking'),
('Digital Marketing');

-- USER SKILLS
INSERT IGNORE INTO USER_SKILLS (user_id, skill_id) VALUES
(1,1),(1,3),
(2,4),(2,7),
(3,2),(3,5),
(4,8),(4,9),
(5,6),(5,10);

-- USER WANTS
INSERT IGNORE INTO USER_WANTS (user_id, skill_id) VALUES
(1,4),(1,6),
(2,1),(2,5),
(3,3),(3,9),
(4,2),(4,10),
(5,7),(5,8);

-- REQUESTS
INSERT IGNORE INTO REQUESTS
(sender_id, receiver_id, skill_offered, skill_requested, status)
VALUES
(1,2,1,4,'Pending'),
(2,3,4,5,'Accepted'),
(3,4,2,9,'Pending');

-- VERIFY DATA
SELECT 'USERS' AS TableName, COUNT(*) AS TotalRows FROM USERS
UNION ALL
SELECT 'SKILLS', COUNT(*) FROM SKILLS
UNION ALL
SELECT 'USER_SKILLS', COUNT(*) FROM USER_SKILLS
UNION ALL
SELECT 'USER_WANTS', COUNT(*) FROM USER_WANTS
UNION ALL
SELECT 'REQUESTS', COUNT(*) FROM REQUESTS;
