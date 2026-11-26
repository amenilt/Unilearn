-- Donnees de demonstration chargees au demarrage (H2)
INSERT INTO claim (student_id, course_id, subject, description, status, created_at, updated_at, course_verified)
VALUES ('etu-001', 1, 'Note du quiz incorrecte', 'Ma note du quiz du chapitre 2 ne correspond pas a mes reponses.', 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE);
INSERT INTO claim (student_id, course_id, subject, description, status, created_at, updated_at, course_verified)
VALUES ('etu-002', 1, 'Lecon inaccessible', 'La lecon 3 affiche une erreur 404.', 'IN_PROGRESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE);
INSERT INTO claim_comment (author_id, comment_text, comment_date, claim_id)
VALUES ('ens-010', 'Nous verifions le lien de la lecon.', CURRENT_TIMESTAMP, 2);
