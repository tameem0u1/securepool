import { createConnection } from 'mysql2/promise';
import bcrypt from 'bcryptjs';

const database_name = "securepool_db";

const connectionProperties = {
            host: 'localhost',
            port: 3306,
            user: 'root',
            password: 'abcdef', // 🔒 Replace with your actual password
        }

const createTableQuery = `
    CREATE TABLE users (
        username VARCHAR(100) PRIMARY KEY,
        password VARCHAR(100),
        score INT DEFAULT 100,
        lastZeroTimestamp DATETIME
    );
`;

let salt = await bcrypt.genSalt(10);
const gamer1pw = await bcrypt.hash('a123', salt);
const gamer2pw = await bcrypt.hash('b123', salt);
const gamer3pw = await bcrypt.hash('c123', salt);
const gamer4pw = await bcrypt.hash('d123', salt);
const gamer5pw = await bcrypt.hash('e123', salt);

const seedUsersQuery = `
    INSERT INTO users (username, password, score, lastZeroTimestamp) VALUES
    ('gamerA', '${gamer1pw}', 100, NULL),
    ('gamerB', '${gamer2pw}', 100, NULL),
    ('gamerC', '${gamer3pw}', 100, NULL),
    ('gamerD', '${gamer4pw}', 100, NULL),
    ('gamerE', '${gamer5pw}', 100, NULL);
`;

/**
 * Initializes the database by checking for the 'users' table and seeding it if empty.
 */
export default async function initializeDatabase() {
    let connection;
    try {
        // Establish a connection to the database
        connection = await createConnection(connectionProperties);
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