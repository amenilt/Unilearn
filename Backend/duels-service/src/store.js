// Donnees en memoire de MS5 (entites et associations de la conception)
//   Duel 1..* DuelRound        (les manches / reponses d'un duel)
//   Badge 1..* UserBadge *..1 utilisateur (identifiant Keycloak)
//   Points : total par utilisateur
//   Notification *..1 utilisateur

const store = {
  duels: [],
  badges: [
    { id: 1, code: 'FIRST_DUEL', name: 'Premier duel', description: 'Terminer un premier duel' },
    { id: 2, code: 'FIRST_WIN', name: 'Vainqueur', description: 'Gagner un premier duel' },
    { id: 3, code: 'CHAMPION', name: 'Champion', description: 'Gagner 3 duels' },
  ],
  userBadges: [], // { id, userId, badgeId, date }
  points: {}, // userId -> total
  notifications: [], // { id, userId, message, read, date }
  seq: { duel: 1, round: 1, badge: 4, userBadge: 1, notification: 1 },
};

const nextId = (name) => store.seq[name]++;

function notify(userId, message) {
  const n = { id: nextId('notification'), userId, message, read: false, date: new Date().toISOString() };
  store.notifications.push(n);
  return n;
}

function addPoints(userId, pts) {
  store.points[userId] = (store.points[userId] || 0) + pts;
}

function hasBadge(userId, code) {
  const badge = store.badges.find((b) => b.code === code);
  return badge && store.userBadges.some((ub) => ub.userId === userId && ub.badgeId === badge.id);
}

function award(userId, code) {
  const badge = store.badges.find((b) => b.code === code);
  if (!badge || hasBadge(userId, code)) return null;
  const ub = { id: nextId('userBadge'), userId, badgeId: badge.id, date: new Date().toISOString() };
  store.userBadges.push(ub);
  notify(userId, `Nouveau badge : ${badge.name}`);
  return ub;
}

function badgesOf(userId) {
  return store.userBadges
    .filter((ub) => ub.userId === userId)
    .map((ub) => ({ ...store.badges.find((b) => b.id === ub.badgeId), obtainedAt: ub.date }));
}

module.exports = { store, nextId, notify, addPoints, award, badgesOf };
