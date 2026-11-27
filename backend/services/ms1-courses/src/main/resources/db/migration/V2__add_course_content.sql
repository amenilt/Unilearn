CREATE TABLE course_chapters (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    position INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_course_chapters_course_position UNIQUE (course_id, position),
    CONSTRAINT fk_course_chapters_course
        FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

CREATE TABLE course_lessons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    chapter_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    position INT NOT NULL,
    content VARCHAR(10000) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_course_lessons_chapter_position UNIQUE (chapter_id, position),
    CONSTRAINT fk_course_lessons_chapter
        FOREIGN KEY (chapter_id) REFERENCES course_chapters (id) ON DELETE CASCADE
);
