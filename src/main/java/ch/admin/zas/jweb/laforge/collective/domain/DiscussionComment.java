package ch.admin.zas.jweb.laforge.collective.domain;

import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Commentaire du débrief collectif d'un défi. Texte brut uniquement, jamais interprété comme
 * HTML/Markdown côté serveur.
 */
@Entity
@Table(name = "challenge_comment")
public class DiscussionComment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Account author;

    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    protected DiscussionComment() {
        // Requis par JPA.
    }

    public DiscussionComment(Challenge challenge, Account author, String body) {
        this.challenge = challenge;
        this.author = author;
        this.body = body;
    }

    public Challenge getChallenge() {
        return challenge;
    }

    public Account getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }
}
