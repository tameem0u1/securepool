import express, { json } from 'express';
import cors from 'cors';
import moment from 'moment'; // ⏱️ Timestamp formatting
import initializeDatabase from './initializeDatabase.js';

const app = express();
app.use(cors());
app.use(json());

app.use((req, res, next) => {
  console.log(`request received: ${req.method} ${req.url} ${JSON.stringify(req.body)}`);
  next();
});

// Initialize the database connection
const db = await initializeDatabase();

app.get('/', (req, res) => {
  res.send('🔗 SecurePool backend is running');
});

app.post('/api/register', async (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ error: 'Missing credentials' });
  }

  try {
    const checkSql = 'SELECT * FROM users WHERE username = ?';
    const [existingUsers] = await db.query(checkSql, [username]);

    if (existingUsers.length > 0) {
      return res.json({ success: false, message: 'Username already exists' }); // 🚫 Username already exists
    }

    const insertSql = 'INSERT INTO users SET username = ?, password = ?, score = 100';
    await db.query(insertSql, [username, password]);

    res.json({ success: true });

  } catch (error) {
    console.error('Error during registration:', error);
    return res.status(500).json({ success: false, error: 'Database error during registration' });
  }
});

// 🔐 Login (plaintext check)
app.post('/api/login', async (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ error: 'Missing credentials' });
  }

  const sql = 'SELECT * FROM users WHERE username = ? AND password = ?';

  try {
    const [results] = await db.query(sql, [username, password]);
    if (results.length === 0) {
      return res.json({ success: false });
    }
    const user = results[0];
    res.json({
      success: true,
      username: user.username,
      score: user.score,
      lastZeroTimestamp: user.lastZeroTimestamp
    });
  } catch (error) {
    console.error('Error executing login query:', error);
    return res.status(500).json({ error: 'Query error' });
  }
});

// 📊 Get score + formatted timestamp
app.get('/api/score', async (req, res) => {
  const { username } = req.query;
  if (!username) {
    return res.status(400).json({ error: 'Missing username' });
  }

  const sql = 'SELECT username, score, lastZeroTimestamp FROM users WHERE username = ?';
  try {
    const [results] = await db.query(sql, [username]);
    if (results.length === 0) {
      return res.status(404).json({ success: false, message: 'User not found' });
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

  } catch (error) {
    console.error('Error executing score query:', error);
    return res.status(500).json({ success: false, message: 'Query error' });
  }
});

app.post('/api/matchResult', async (req, res) => {
  const { winner, loser, outcome } = req.body;
  if (!winner || !loser || !outcome) {
    return res.status(400).json({ error: 'Missing match data' });
  }

  const increaseSql = 'UPDATE users SET score = score + 10 WHERE username = ?';
  const decreaseSql = `
    UPDATE users 
    SET score = GREATEST(score - 10, 0), 
        lastZeroTimestamp = IF(score - 10 <= 0, NOW(), lastZeroTimestamp)
    WHERE username = ?
  `;

  try {
    await db.query(increaseSql, [winner]);
    await db.query(decreaseSql, [loser]);

    res.json({ message: 'Match result synced successfully' });

  } catch (error) {
    console.error('Error syncing match result:', error);
    return res.status(500).json({ error: 'Failed to sync match result' });
  }
});

app.post('/api/restore-score', async (req, res) => {
  const { username } = req.body;
  if (!username) {
    return res.status(400).json({ error: 'Missing username' });
  }

  const sql = 'UPDATE users SET score = 100, lastZeroTimestamp = NULL WHERE username = ?';

  try {
    await db.query(sql, [username]);
    res.status(200).json({ success: true, message: 'Score restored' });

  } catch (error) {
    console.error('Error restoring score:', error);
    return res.status(500).json({ error: 'Restore failed' });
  }
});

app.get('/api/leaderboard', async (req, res) => {
  const sql = 'SELECT username, score FROM users ORDER BY score DESC LIMIT 10';
  try {
    const [rows] = await db.query(sql);
    if (rows.length === 0) {
      return res.status(404).json({ success: false, message: 'No users found' });
    }
    return res.json(rows);
  }
  catch (err) {
    console.error('Error fetching leaderboard:', err);
    return res.status(500).json({ error: 'Leaderboard error' });
  }
});

app.listen(8080, '0.0.0.0', () => {
  console.log('🚀 Server running on port 8080');
});