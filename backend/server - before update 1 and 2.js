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
  database: 'securepool_db' // 📁 Ensure this DB exists
});

// Confirm MySQL connection
db.connect(err => {
  if (err) {
    console.error('❌ MySQL connection failed:', err.message);
    process.exit(1);
  }
  console.log('✅ Connected to MySQL');
});

// 🔗 Root health check
app.get('/', (req, res) => {
  res.send('🔗 SecurePool backend is running');
});

// 👤 Register user only if not already present
app.post('/api/register', (req, res) => {
  const { username } = req.body;
  if (!username) {
    return res.status(400).json({ error: 'Missing username' });
  }

  const checkSql = 'SELECT * FROM users WHERE username = ?';
  db.query(checkSql, [username], (err, results) => {
    if (err) {
      console.error('Registration check error:', err.message);
      return res.status(500).json({ error: 'Database error during check' });
    }

    if (results.length === 0) {
      const insertSql = 'INSERT INTO users (username, score) VALUES (?, 100)';
      db.query(insertSql, [username], (insertErr, insertRes) => {
        if (insertErr) {
          console.error('Insert error:', insertErr.message);
          return res.status(500).json({ error: 'User insert failed' });
        }
        res.json({ message: 'User registered', userId: insertRes.insertId });
      });
    } else {
      res.json({ message: 'User already exists', userId: results[0].id });
    }
  });
});

// 📊 Get current score of user
app.get('/api/score', (req, res) => {
  const { username } = req.query;
  if (!username) {
    return res.status(400).json({ error: 'Missing username' });
  }

  const sql = 'SELECT score FROM users WHERE username = ?';
  db.query(sql, [username], (err, results) => {
    if (err) {
      console.error('Score fetch error:', err.message);
      return res.status(500).json({ error: 'Score query failed' });
    }
    if (results.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }
    res.json({ score: results[0].score });
  });
});

// 🏆 Sync match result: winner gets +10, loser loses -10 (min 0)
app.post('/api/matchResult', (req, res) => {
  const { winner, loser, outcome } = req.body;

  if (!winner || !loser || !outcome) {
    return res.status(400).json({ error: 'Missing match data' });
  }

  const increaseScoreSql = 'UPDATE users SET score = score + 10 WHERE username = ?';
  const decreaseScoreSql = 'UPDATE users SET score = GREATEST(score - 10, 0) WHERE username = ?';

  db.query(increaseScoreSql, [winner], (err1) => {
    if (err1) {
      console.error('Winner update error:', err1.message);
      return res.status(500).json({ error: 'Winner score update failed' });
    }

    db.query(decreaseScoreSql, [loser], (err2) => {
      if (err2) {
        console.error('Loser update error:', err2.message);
        return res.status(500).json({ error: 'Loser score update failed' });
      }

      res.json({ message: 'Match result synced successfully' });
    });
  });
});

// 🧠 Leaderboard sorted by score
app.get('/api/leaderboard', (req, res) => {
  const sql = 'SELECT username, score FROM users ORDER BY score DESC';
  db.query(sql, (err, results) => {
    if (err) {
      console.error('Leaderboard fetch error:', err.message);
      return res.status(500).json({ error: 'Leaderboard query failed' });
    }
    res.json(results);
  });
});

// 🚀 Start server on all interfaces for emulator access
app.listen(8080, '0.0.0.0', () => {
  console.log('🚀 Server running on all interfaces at port 8080');
});