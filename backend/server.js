import express, { json } from 'express';
import cors from 'cors';
import moment from 'moment'; // ⏱️ Timestamp formatting
import initializeDatabase from './initializeDatabase.js';

const app = express();
app.use(cors());
app.use(json());

// Connect to your local MySQL server

const db = await initializeDatabase();

app.get('/', (req, res) => {
  res.send('🔗 SecurePool backend is running');
});

// 🔐 Register new user (reject duplicates)
app.post('/api/register', (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ error: 'Missing credentials' });
  }

  const checkSql = 'SELECT * FROM users WHERE username = ?';
  db.query(checkSql, [username], (err, results) => {
    if (err) return res.status(500).json({ error: 'Query error' });

    if (results.length > 0) {
      return res.json({ success: false }); // 🚫 Username already exists
    }

    const insertSql = 'INSERT INTO users SET username = ?, password = ?, score = 100';
    db.query(insertSql, [username, password], err2 => {
      if (err2) return res.status(500).json({ success: false });
      res.json({ success: true });
    });
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

// 📊 Get score + formatted timestamp
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

    const player = results[0];
    const formattedTimestamp = player.lastZeroTimestamp
      ? moment(player.lastZeroTimestamp).format('YYYY-MM-DD HH:mm:ss')
      : null;

    res.json({
      username: player.username,
      score: player.score,
      lastZeroTimestamp: formattedTimestamp
    });
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