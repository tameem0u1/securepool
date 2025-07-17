const express = require('express');
const mysql = require('mysql2');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

const db = mysql.createConnection({
  host: 'localhost',
  user: 'root',
  password: 'abcdef',
  database: 'securepool_db'
});

db.connect(err => {
  if (err) {
    console.error('❌ MySQL connection failed:', err.message);
    process.exit(1);
  }
  console.log('✅ Connected to MySQL');
});

app.get('/', (req, res) => {
  res.send('🔗 SecurePool backend is running');
});

// 🔐 Register with username + password
app.post('/api/register', (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ error: 'Missing credentials' });
  }

  const sql = 'INSERT IGNORE INTO users SET username = ?, password = ?, score = 100';
  db.query(sql, [username, password], err => {
    res.json({ success: !err });
  });
});

// 🔐 Login (plaintext check)
app.post('/api/login', (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ error: 'Missing credentials' });
  }

  const sql = 'SELECT * FROM users WHERE username = ? AND password = ?';
  db.query(sql, [username, password], (err, results) => {
    if (err || results.length === 0) {
      return res.json({ success: false });
    }
    res.json({ success: true });
  });
});

// 📊 Get score + timestamp
app.get('/api/score', (req, res) => {
  const { username } = req.query;
  if (!username) {
    return res.status(400).json({ error: 'Missing username' });
  }

  const sql = 'SELECT username, score, lastZeroTimestamp FROM users WHERE username = ?';
  db.query(sql, [username], (err, results) => {
    if (err || results.length === 0) {
      return res.status(500).json({ error: 'User not found or query failed' });
    }
    res.json(results[0]);
  });
});

// 🏆 Sync match result
app.post('/api/matchResult', (req, res) => {
  const { winner, loser, outcome } = req.body;
  if (!winner || !loser || !outcome) {
    return res.status(400).json({ error: 'Missing match data' });
  }

  const decreaseSql = `
    UPDATE users 
    SET score = GREATEST(score - 10, 0), 
        lastZeroTimestamp = IF(score - 10 <= 0, NOW(), lastZeroTimestamp)
    WHERE username = ?
  `;

  const increaseSql = 'UPDATE users SET score = score + 10 WHERE username = ?';

  db.query(increaseSql, [winner], err1 => {
    if (err1) return res.status(500).json({ error: 'Winner score update failed' });

    db.query(decreaseSql, [loser], err2 => {
      if (err2) return res.status(500).json({ error: 'Loser score update failed' });

      res.json({ message: 'Match result synced' });
    });
  });
});

// 🔁 Restore score after cooldown
app.post('/api/restore-score', (req, res) => {
  const { username } = req.body;
  if (!username) {
    return res.status(400).json({ error: 'Missing username' });
  }

  const sql = 'UPDATE users SET score = 100, lastZeroTimestamp = NULL WHERE username = ?';
  db.query(sql, [username], err => {
    if (err) return res.status(500).json({ error: 'Restore failed' });
    res.status(200).send();
  });
});

// 📊 Leaderboard
app.get('/api/leaderboard', (req, res) => {
  const sql = 'SELECT username, score FROM users ORDER BY score DESC';
  db.query(sql, (err, results) => {
    if (err) return res.status(500).json({ error: 'Leaderboard error' });
    res.json(results);
  });
});

app.listen(8080, '0.0.0.0', () => {
  console.log('🚀 Server running on port 8080');
});