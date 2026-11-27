package com.unilearn.ms1.courses.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "course_lessons",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_lessons_chapter_position",
                columnNames = {"chapter_id", "position"}
        )
)
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "chapter_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_course_lessons_chapter")
    )
    private Chapter chapter;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 10000)
    private String content;

    protected Lesson() {
    }

    public Lesson(Chapter chapter, String title, int position, String content) {
        this.chapter = chapter;
        this.title = title;
        this.position = position;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public String getTitle() {
        return title;
    }

    public int getPosition() {
        return position;
    }

    public String getContent() {
        return content;
    }
}
