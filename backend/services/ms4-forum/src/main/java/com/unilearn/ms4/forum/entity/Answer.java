package com.unilearn.ms4.forum.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Reponse a une question : sous-document imbrique dans Question.answers.
 */
public class Answer {

    private String id = UUID.randomUUID().toString();
    private String authorId;
    private String body;
    private int votes;
    /** Utilisateurs ayant deja vote (un seul vote par utilisateur -> 409 sinon). */
    private Set<String> voterIds = new HashSet<>();
    private boolean accepted;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public int getVotes() { return votes; }
    public void setVotes(int votes) { this.votes = votes; }

    public Set<String> getVoterIds() { return voterIds; }
    public void setVoterIds(Set<String> voterIds) { this.voterIds = voterIds != null ? voterIds : new HashSet<>(); }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
