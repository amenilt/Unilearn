from app.schemas.assistant import LessonSource
from app.schemas.recommendation import CourseCandidate

DEMO_STUDENT_ID = "demo-student-001"
DEMO_INTERESTS = ["statistics", "Python", "data analysis"]
DEMO_GOALS = ["data visualization", "interpret study results"]

DEMO_COURSES = [
    CourseCandidate(
        id=101,
        title="Introduction to Statistics",
        description="Descriptive statistics, probability, sampling, and interpreting data.",
        categoryName="Data Science",
        subjectName="Mathematics",
    ),
    CourseCandidate(
        id=102,
        title="Python for Data Analysis",
        description="Use Python and pandas to clean, explore, and analyze datasets.",
        categoryName="Programming",
        subjectName="Computer Science",
    ),
    CourseCandidate(
        id=103,
        title="Data Visualization with Python",
        description="Create clear charts with Python and communicate data insights.",
        categoryName="Data Science",
        subjectName="Computer Science",
    ),
    CourseCandidate(
        id=104,
        title="Research Methods",
        description="Plan a study, collect evidence, and evaluate research findings.",
        categoryName="Research Skills",
        subjectName="Research Methods",
    ),
]

DEMO_LESSONS = {
    101: [
        LessonSource(
            chapter_id=1101,
            chapter_title="Describing Data",
            lesson_id=11011,
            lesson_title="The Arithmetic Mean",
            content=(
                "The arithmetic mean is the sum of all observations divided by the number of observations. "
                "It summarizes the center of a dataset, but extreme values can pull the mean upward or downward. "
                "The median is often more representative when a dataset contains outliers."
            ),
        ),
        LessonSource(
            chapter_id=1102,
            chapter_title="Probability",
            lesson_id=11021,
            lesson_title="Events and Probability",
            content=(
                "Probability measures how likely an event is, from zero for an impossible event to one for a certain event. "
                "For equally likely outcomes, probability is the number of favorable outcomes divided by the total number of outcomes."
            ),
        ),
    ],
    103: [
        LessonSource(
            chapter_id=1301,
            chapter_title="Communicating Results",
            lesson_id=13011,
            lesson_title="Choosing a Chart",
            content=(
                "A bar chart compares values across categories, while a line chart shows change over an ordered sequence such as time. "
                "A chart should make the main comparison visible and label its axes clearly."
            ),
        )
    ],
}


class DemoCourseCatalogClient:
    def list_published_courses(self, _access_token: str) -> list[CourseCandidate]:
        return list(DEMO_COURSES)

    def list_enrolled_course_ids(self, _access_token: str) -> set[int]:
        return {102}


class DemoCourseContentClient:
    def list_lessons(self, course_id: int, _access_token: str) -> list[LessonSource]:
        return list(DEMO_LESSONS.get(course_id, []))
