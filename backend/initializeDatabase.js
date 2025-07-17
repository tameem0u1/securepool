import { createConnection } from 'mysql2/promise';

const database_name = "securepool_db";

const createTableQuery = `
    CREATE TABLE users (
        username VARCHAR(100) PRIMARY KEY,
        password VARCHAR(100),
        score INT DEFAULT 100,
        lastZeroTimestamp DATETIME
    );
`;

const seedUsersQuery = `
    INSERT INTO users (username, password, score, lastZeroTimestamp) VALUES
    ('gamerA', 'a123', 100, NULL),
    ('gamerB', 'b123', 100, NULL),
    ('gamerC', 'c123', 100, NULL),
    ('gamerD', 'd123', 100, NULL),
    ('gamerE', 'e123', 100, NULL);
`;

/**
 * Initializes the database by checking for the 'users' table and seeding it if empty.
 */
export default async function initializeDatabase() {
    let connection;
    try {
        // Establish a connection to the database
        connection = await createConnection({
            host: 'localhost',
            user: 'root',
            password: 'abcdef', // 🔒 Replace with your actual password
        });
        console.log('Connected to MySQL database.');

        await connection.execute(`CREATE DATABASE IF NOT EXISTS ${database_name}`);
        
        await connection.query(`USE \`${database_name}\`;`);

        await connection.connect();

        // 1. Check if the 'users' table exists
        const [rows] = await connection.execute(
            `SHOW TABLES LIKE 'users';`
        );

        console.log(rows);

        if (rows.length === 0) {
            // Table does not exist, so create it
            console.log('Table "users" does not exist. Creating table...');
            connection.execute(createTableQuery);
            console.log('Table "users" created successfully.');

            // After creating, it's definitely empty, so seed it
            console.log('Seeding "users" table with initial data...');
            connection.execute(seedUsersQuery);
            console.log('Users seeded successfully.');
        } else {
            console.log('Table "users" already exists.');

            // 2. Check if the table has any users (rows)
            const [userCountRows] = await connection.execute(
                `SELECT COUNT(*) AS count FROM users;`
            );
            const userCount = userCountRows[0].count;

            if (userCount === 0) {
                // Table exists but is empty, so seed it
                console.log('Table "users" is empty. Seeding with initial data...');
                connection.execute(seedUsersQuery);
                console.log('Users seeded successfully.');
            } else {
                console.log(`Table "users" already contains ${userCount} users. No seeding required.`);
            }
        }
        return connection;
    } catch (error) {
        console.error('Database initialization failed:', error);
        if (connection) {
            connection.end();
            console.log('Database connection closed.');
        }
    }
}