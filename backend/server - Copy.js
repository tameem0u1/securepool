// server.js

const express = require('express');
const mysql = require('mysql2');
const cors = require('cors');

// Initialize the Express app
const app = express();
app.use(cors());
app.use(express.json());

// Connect to your local MySQL server
const db = mysql.createConnection({
  host: 'localhost',
  user: 'root',
  password: 'abcdef', // 🔒 Replace with your actual password
  database: 'securepool_db'        // 📁 Make sure this database has been created
});

// Confirm MySQL connection
db.connect(err => {
  if (err) {
    console.error('❌ MySQL connection failed:', err.message);
    process.exit(1);
  }
  console.log('✅ Connected to MySQL');
});

// Test route
app.get('/', (req, res) => {
  res.send('🔗 SecurePool backend is running');
});

// Register a new user
app.post('/api/register', (req, res) => {
  const { username } = req.body;
  if (!username) {
    return res.status(400).json({ error: 'Missing username' });
  }

  const sql = 'INSERT INTO users (username) VALUES (?)';
  db.query(sql, [username], (err, result) => {
    if (err) {
      console.error('Registration error:', err.message);
      return res.status(500).json({ error: 'Database error' });
    }
    res.json({ message: 'User registered', userId: result.insertId });
  });
});

// Get a user's current score
app.get('/api/score', (req, res) => {
  const { username } = req.query;
  if (!username) {
    return res.status(400).json({ error: 'Missing username' });
  }

  const sql = 'SELECT score FROM users WHERE username = ?';
  db.query(sql, [username], (err, results) => {
    if (err) {
      console.error('Score fetch error:', err.message);
      return res.status(500).json({ error: 'Database error' });
    }
    if (results.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }
    res.json({ score: results[0].score });
  });
});

// Start server
app.listen(8080, () => {
  console.log('🚀 Server running at http://localhost:8080');
});