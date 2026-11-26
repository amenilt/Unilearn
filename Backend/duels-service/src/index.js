// MS5 - Duels et Badges (UniLearn) - microservice commun, realise par toute l'equipe
// Node.js / Express + client Eureka + WebSocket (duels en temps reel)
const http = require('http');
const express = require('express');
const { WebSocketServer } = require('ws');
const pkg = require('../package.json');
const { store, nextId, notify, addPoints, award, badgesOf } = require('./store');
const { createEurekaClient, resolveService } = require('./eureka');

const PORT = parseInt(process.env.PORT || '8085', 10); // 2e instance : PORT=8095
const HOST = process.env.HOST || 'localhost';
const APP_NAME = 'MS5-DUELS';
const EUREKA_URLS = (process.env.EUREKA_URLS || 'http://localhost:8761/eureka,http://localhost:8762/eureka')
  .split(',').map((u) => u.trim());

const app = express();
app.use(express.json());
const server = http.createServer(app);
const eureka = createEurekaClient({ appName: APP_NAME, host: HOST, port: PORT, eurekaUrls: EUREKA_URLS });

// ------------------------------------------------------------------
// WebSocket : ws://localhost:8085/ws  -> recoit les evenements des duels en direct
// ------------------------------------------------------------------
const wss = new WebSocketServer({ server, path: '/ws' });
const broadcast = (type, payload) => {
  const msg = JSON.stringify({ type, payload });
  wss.clients.forEach((c) => c.readyState === 1 && c.send(msg));
};

// ------------------------------------------------------------------
// Appel synchrone MS5 -> MS2 (questions du quiz) via Eureka
// ------------------------------------------------------------------
async function fetchQuizQuestions(quizId) {
  const base = resolveService(eureka, 'MS2-QUIZ');
  if (!base) {
    const err = new Error('MS2-QUIZ indisponible (aucune instance UP dans Eureka)');
    err.status = 503;
    throw err;
  }
  let res;
  try {
    res = await fetch(`${base}/api/quizzes/${quizId}/questions`);
  } catch (e) {
    const err = new Error(`MS2-QUIZ injoignable (${base})`);
    err.status = 503;
    throw err;
  }
  if (res.status === 404) {
    const err = new Error(`Quiz ${quizId} introuvable`);
    err.status = 400;
    throw err;
  }
  if (!res.ok) {
    const err = new Error(`MS2-QUIZ a repondu ${res.status}`);
    err.status = 503;
    throw err;
  }
  return res.json();
}

// Corrige des duels, garde hors des objets renvoyes au client (pas de triche)
const answerKeys = new Map(); // duelId -> { questionId: correctOptionId }

const findDuel = (id) => store.duels.find((d) => d.id === Number(id));
const scoreOf = (duel, userId) =>
  duel.rounds.filter((r) => r.userId === userId && r.correct).reduce((s, r) => s + r.points, 0);

function finishIfComplete(duel) {
  const total = duel.questions.length;
  const answered = (u) => duel.rounds.filter((r) => r.userId === u).length;
  if (answered(duel.challengerId) < total || answered(duel.opponentId) < total) return;

  const a = scoreOf(duel, duel.challengerId);
  const b = scoreOf(duel, duel.opponentId);
  duel.status = 'FINISHED';
  duel.finishedAt = new Date().toISOString();
  duel.scores = { [duel.challengerId]: a, [duel.opponentId]: b };
  duel.winnerId = a === b ? null : a > b ? duel.challengerId : duel.opponentId;

  for (const u of [duel.challengerId, duel.opponentId]) {
    addPoints(u, u === duel.winnerId ? 10 : 3);
    award(u, 'FIRST_DUEL');
    notify(u, duel.winnerId ? `Duel #${duel.id} termine, vainqueur : ${duel.winnerId}` : `Duel #${duel.id} : egalite`);
  }
  if (duel.winnerId) {
    award(duel.winnerId, 'FIRST_WIN');
    const wins = store.duels.filter((d) => d.winnerId === duel.winnerId).length;
    if (wins >= 3) award(duel.winnerId, 'CHAMPION');
  }
  // Evolution prevue (conception) : publier duel.finished via RabbitMQ
  broadcast('duel.finished', duel);
}

// ------------------------------------------------------------------
// Duels (CRUD + deroulement)
// ------------------------------------------------------------------
app.get('/api/duels/hello', (req, res) => res.json({ service: APP_NAME, port: PORT }));

app.get('/api/duels', (req, res) => {
  const { userId, status } = req.query;
  let list = store.duels;
  if (userId) list = list.filter((d) => d.challengerId === userId || d.opponentId === userId);
  if (status) list = list.filter((d) => d.status === status);
  res.json(list);
});

app.get('/api/duels/:id', (req, res) => {
  const d = findDuel(req.params.id);
  return d ? res.json(d) : res.status(404).json({ message: 'Duel introuvable' });
});

// POST /api/duels {"challengerId":"etu-001","opponentId":"etu-002","quizId":1} -> 201, 400, 503
app.post('/api/duels', async (req, res) => {
  const { challengerId, opponentId, quizId } = req.body || {};
  if (!challengerId || !opponentId || !quizId || challengerId === opponentId) {
    return res.status(400).json({ message: 'challengerId, opponentId (differents) et quizId obligatoires' });
  }
  try {
    // Mode demo (rendu individuel, MS2 absent) : les questions peuvent etre fournies dans le corps.
    // Sinon : appel normal a MS2-QUIZ via Eureka (503 si MS2 est indisponible).
    const questions = Array.isArray(req.body.questions) && req.body.questions.length
      ? req.body.questions.map((q, i) => ({
          id: q.id ?? i + 1,
          text: q.text,
          points: q.points || 1,
          options: (q.options || []).map((o, j) => ({ id: o.id ?? (i + 1) * 10 + j + 1, text: o.text, correct: !!o.correct })),
        }))
      : await fetchQuizQuestions(quizId);
    const duel = {
      id: nextId('duel'),
      challengerId,
      opponentId,
      quizId,
      status: 'PENDING',
      winnerId: null,
      createdAt: new Date().toISOString(),
      questions: questions.map((q) => ({
        id: q.id,
        text: q.text,
        points: q.points || 1,
        options: (q.options || []).map((o) => ({ id: o.id, text: o.text })), // sans la bonne reponse
      })),
      rounds: [],
    };
    answerKeys.set(duel.id, Object.fromEntries(questions.map((q) => [q.id, (q.options || []).find((o) => o.correct)?.id])));
    store.duels.push(duel);
    notify(opponentId, `${challengerId} vous defie en duel (#${duel.id})`);
    broadcast('duel.created', duel);
    res.status(201).json(duel);
  } catch (e) {
    res.status(e.status || 500).json({ message: e.message });
  }
});

// -> 200, 404, 409
app.post('/api/duels/:id/accept', (req, res) => {
  const d = findDuel(req.params.id);
  if (!d) return res.status(404).json({ message: 'Duel introuvable' });
  if (d.status !== 'PENDING') return res.status(409).json({ message: `Duel deja ${d.status}` });
  d.status = 'IN_PROGRESS';
  notify(d.challengerId, `${d.opponentId} a accepte le duel #${d.id}`);
  broadcast('duel.accepted', d);
  res.json(d);
});

// {"userId":"etu-001","questionId":1,"optionId":2} -> 200, 400, 404, 409
app.post('/api/duels/:id/answer', (req, res) => {
  const d = findDuel(req.params.id);
  if (!d) return res.status(404).json({ message: 'Duel introuvable' });
  if (d.status !== 'IN_PROGRESS') return res.status(409).json({ message: `Duel ${d.status}` });
  const { userId, questionId, optionId } = req.body || {};
  if (![d.challengerId, d.opponentId].includes(userId)) {
    return res.status(400).json({ message: "Cet utilisateur ne participe pas au duel" });
  }
  const q = d.questions.find((x) => x.id === Number(questionId));
  if (!q) return res.status(400).json({ message: 'Question hors du duel' });
  if (d.rounds.some((r) => r.userId === userId && r.questionId === q.id)) {
    return res.status(409).json({ message: 'Question deja repondue' });
  }
  const round = {
    id: nextId('round'),
    duelId: d.id,
    userId,
    questionId: q.id,
    optionId: Number(optionId),
    correct: Number(optionId) === answerKeys.get(d.id)[q.id],
    points: q.points,
  };
  d.rounds.push(round);
  broadcast('duel.answer', { duelId: d.id, userId, questionId: q.id });
  finishIfComplete(d);
  res.json({ round, duelStatus: d.status, winnerId: d.winnerId });
});

app.delete('/api/duels/:id', (req, res) => {
  const before = store.duels.length;
  store.duels = store.duels.filter((d) => d.id !== Number(req.params.id));
  return store.duels.length < before ? res.status(204).end() : res.status(404).json({ message: 'Duel introuvable' });
});

// ------------------------------------------------------------------
// Badges, classement, notifications
// ------------------------------------------------------------------
app.get('/api/badges', (req, res) => res.json(store.badges));

app.post('/api/badges', (req, res) => {
  const { code, name, description } = req.body || {};
  if (!code || !name) return res.status(400).json({ message: 'code et name obligatoires' });
  const b = { id: nextId('badge'), code, name, description };
  store.badges.push(b);
  res.status(201).json(b);
});

app.put('/api/badges/:id', (req, res) => {
  const b = store.badges.find((x) => x.id === Number(req.params.id));
  if (!b) return res.status(404).json({ message: 'Badge introuvable' });
  Object.assign(b, { name: req.body.name ?? b.name, description: req.body.description ?? b.description });
  res.json(b);
});

app.delete('/api/badges/:id', (req, res) => {
  const id = Number(req.params.id);
  const before = store.badges.length;
  store.badges = store.badges.filter((b) => b.id !== id);
  store.userBadges = store.userBadges.filter((ub) => ub.badgeId !== id);
  return store.badges.length < before ? res.status(204).end() : res.status(404).json({ message: 'Badge introuvable' });
});

app.get('/api/badges/:userId', (req, res) => res.json(badgesOf(req.params.userId)));

app.get('/api/leaderboard', (req, res) => {
  const board = Object.entries(store.points)
    .map(([userId, points]) => ({ userId, points, badges: badgesOf(userId).length }))
    .sort((a, b) => b.points - a.points);
  res.json(board);
});

app.get('/api/notifications/:userId', (req, res) =>
  res.json(store.notifications.filter((n) => n.userId === req.params.userId))
);

// ------------------------------------------------------------------
// Observabilite : /health et /info (equivalents d'Actuator pour Node.js)
// ------------------------------------------------------------------
app.get('/health', (req, res) => {
  const ms2 = resolveService(eureka, 'MS2-QUIZ');
  res.json({
    status: 'UP',
    components: {
      memory: { status: 'UP', details: { heapUsedMb: Math.round(process.memoryUsage().heapUsed / 1048576) } },
      websocket: { status: 'UP', details: { clients: wss.clients.size } },
      ms2Quiz: { status: ms2 ? 'UP' : 'UNKNOWN', details: { instance: ms2 } },
    },
  });
});

app.get('/info', (req, res) =>
  res.json({
    app: {
      name: pkg.name,
      version: pkg.version,
      description: pkg.description,
      team: 'Ayoub Jleli, Ameni Ltaifi, Mohamed Rhouma, Akrem Jouini',
    },
  })
);

// ------------------------------------------------------------------
server.listen(PORT, () => {
  console.log(`${APP_NAME} demarre sur http://${HOST}:${PORT} (WebSocket : ws://${HOST}:${PORT}/ws)`);
  eureka.start((err) => {
    if (err) console.error('Enregistrement Eureka echoue :', err.message || err);
    else console.log(`${APP_NAME} enregistre dans Eureka`);
  });
});

const shutdown = () => {
  console.log('Arret : desinscription de Eureka...');
  eureka.stop(() => server.close(() => process.exit(0)));
  setTimeout(() => process.exit(0), 3000).unref();
};
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
