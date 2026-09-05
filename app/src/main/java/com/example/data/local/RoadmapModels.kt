package com.example.data.local

import com.example.R

enum class RoadmapStepType {
    PRONUNCIATION,
    ROLEPLAY,
    ROLEPLAY_REVIEW,
    GRAMMAR,
    VOCABULARY,
    UNIT_REVIEW
}

data class RoadmapStep(
    val id: String,
    val unitId: String,
    val stepNumber: Int,
    val title: String,
    val type: RoadmapStepType,
    val subtitle: String = "",
    val imageRes: Int? = null,
    val targetSentence: String = "",
    val phoneticTip: String = "",
    val grammarQuestion: String = "",
    val grammarOptions: List<String> = emptyList(),
    val correctOptionIndex: Int = 0,
    val grammarExplanation: String = "",
    val roleplayScenarioId: String? = null,
    val defaultCompleted: Boolean = false
)

data class RoadmapUnit(
    val id: String,
    val unitNumber: Int,
    val title: String,
    val subtitle: String,
    val steps: List<RoadmapStep>
)

object RoadmapCurriculum {
    val units: List<RoadmapUnit> = listOf(
        RoadmapUnit(
            id = "unit_1",
            unitNumber = 1,
            title = "Unit 1 | Foundations & Introductions",
            subtitle = "Master greeting nuances, self-introduction, and confident speech",
            steps = listOf(
                RoadmapStep(
                    id = "u1_s1",
                    unitId = "unit_1",
                    stepNumber = 1,
                    title = "Pronunciation exercise",
                    type = RoadmapStepType.PRONUNCIATION,
                    subtitle = "Vowel clarity & soft linking",
                    targetSentence = "Pleased to meet you! I'm genuinely thrilled to join this conversation.",
                    phoneticTip = "/pliːzd tuː miːt juː/ — ensure the 'th' in 'thrilled' /θrɪld/ has tongue between teeth.",
                    defaultCompleted = true
                ),
                RoadmapStep(
                    id = "u1_s2",
                    unitId = "unit_1",
                    stepNumber = 2,
                    title = "Making a new friend",
                    type = RoadmapStepType.ROLEPLAY,
                    subtitle = "Casual social dialogue at an art workshop",
                    imageRes = R.drawable.img_chapter_making_friends_1788579459754,
                    roleplayScenarioId = "scenario_casual",
                    defaultCompleted = true
                ),
                RoadmapStep(
                    id = "u1_s3",
                    unitId = "unit_1",
                    stepNumber = 3,
                    title = "Roleplay review",
                    type = RoadmapStepType.ROLEPLAY_REVIEW,
                    subtitle = "Active listening cues and reciprocal questions",
                    targetSentence = "Use phrase: 'That's fascinating! How did you get started with that?'",
                    defaultCompleted = true
                ),
                RoadmapStep(
                    id = "u1_s4",
                    unitId = "unit_1",
                    stepNumber = 4,
                    title = "Pronunciation exercise",
                    type = RoadmapStepType.PRONUNCIATION,
                    subtitle = "Question intonation & rhythm",
                    targetSentence = "Could you tell me a little bit about what you do for fun?",
                    phoneticTip = "Rise slightly at the end of the sentence to sound curious and welcoming.",
                    defaultCompleted = true
                ),
                RoadmapStep(
                    id = "u1_s5",
                    unitId = "unit_1",
                    stepNumber = 5,
                    title = "Unit Review",
                    type = RoadmapStepType.UNIT_REVIEW,
                    subtitle = "Comprehensive mastery evaluation",
                    targetSentence = "I usually spend my downtime exploring local cafes and reading literature.",
                    defaultCompleted = true
                )
            )
        ),
        RoadmapUnit(
            id = "unit_2",
            unitNumber = 2,
            title = "Unit 2 | Your interests & Hobbies",
            subtitle = "Express passions, sports, music, and weekend adventures fluently",
            steps = listOf(
                RoadmapStep(
                    id = "u2_s1",
                    unitId = "unit_2",
                    stepNumber = 1,
                    title = "Common Hobbies",
                    type = RoadmapStepType.ROLEPLAY,
                    subtitle = "Talk about personal creative pastimes",
                    imageRes = R.drawable.img_tutor_emma_1788578908516,
                    roleplayScenarioId = "scenario_casual",
                    defaultCompleted = false
                ),
                RoadmapStep(
                    id = "u2_s2",
                    unitId = "unit_2",
                    stepNumber = 2,
                    title = "Pronunciation exercise",
                    type = RoadmapStepType.PRONUNCIATION,
                    subtitle = "Rhythm in multi-syllable leisure words",
                    targetSentence = "I love experimenting with photography and acoustic guitar.",
                    phoneticTip = "/fəˈtɒɡ.rə.fi/ — stress on the second syllable 'tog'.",
                    defaultCompleted = false
                ),
                RoadmapStep(
                    id = "u2_s3",
                    unitId = "unit_2",
                    stepNumber = 3,
                    title = "Vocabulary exercise",
                    type = RoadmapStepType.VOCABULARY,
                    subtitle = "Describing leisure with vivid adjectives",
                    targetSentence = "Words: 'Invigorating', 'Captivating', 'Therapeutic'",
                    defaultCompleted = false
                ),
                RoadmapStep(
                    id = "u2_s4",
                    unitId = "unit_2",
                    stepNumber = 4,
                    title = "Physical Activities",
                    type = RoadmapStepType.ROLEPLAY,
                    subtitle = "Gym, outdoor trekking, and competitive sports",
                    imageRes = R.drawable.img_tutor_arthur_1788578922005,
                    roleplayScenarioId = "scenario_cafe",
                    defaultCompleted = false
                ),
                RoadmapStep(
                    id = "u2_s5",
                    unitId = "unit_2",
                    stepNumber = 5,
                    title = "Grammar exercise",
                    type = RoadmapStepType.GRAMMAR,
                    subtitle = "Gerunds after prepositions",
                    grammarQuestion = "Choose the correct sentence for expressing interest:",
                    grammarOptions = listOf(
                        "I am really interested in learning acoustic guitar.",
                        "I am really interested to learning acoustic guitar.",
                        "I am really interested at learn acoustic guitar."
                    ),
                    correctOptionIndex = 0,
                    grammarExplanation = "Prepositions (like 'in', 'at', 'about') must be followed by a gerund (-ing form), not an infinitive.",
                    defaultCompleted = false
                ),
                RoadmapStep(
                    id = "u2_s6",
                    unitId = "unit_2",
                    stepNumber = 6,
                    title = "Pronunciation exercise",
                    type = RoadmapStepType.PRONUNCIATION,
                    subtitle = "Connected speech reductions",
                    targetSentence = "I'm going to take up mountain biking this coming weekend.",
                    phoneticTip = "Natural native speech links 'going to' as /ˈɡən.ə/ and 'take up' as /teɪk.ʌp/.",
                    defaultCompleted = false
                ),
                RoadmapStep(
                    id = "u2_s7",
                    unitId = "unit_2",
                    stepNumber = 7,
                    title = "Creative Hobbies",
                    type = RoadmapStepType.ROLEPLAY,
                    subtitle = "Digital arts, writing, and culinary skills",
                    imageRes = R.drawable.img_chapter_making_friends_1788579459754,
                    roleplayScenarioId = "scenario_debate",
                    defaultCompleted = false
                )
            )
        ),
        RoadmapUnit(
            id = "unit_3",
            unitNumber = 3,
            title = "Unit 3 | Professional & Job Interviews",
            subtitle = "Executive presence, structured STAR answers, and workplace influence",
            steps = listOf(
                RoadmapStep(
                    id = "u3_s1",
                    unitId = "unit_3",
                    stepNumber = 1,
                    title = "Telling your career story",
                    type = RoadmapStepType.ROLEPLAY,
                    subtitle = "Structured narrative from entry to lead",
                    imageRes = R.drawable.img_speaking_hero_1788577044196,
                    roleplayScenarioId = "scenario_interview",
                    defaultCompleted = false
                ),
                RoadmapStep(
                    id = "u3_s2",
                    unitId = "unit_3",
                    stepNumber = 2,
                    title = "Grammar exercise",
                    type = RoadmapStepType.GRAMMAR,
                    subtitle = "Strong action verbs in past tense",
                    grammarQuestion = "Which sentence uses the most impactful leadership verb?",
                    grammarOptions = listOf(
                        "I spearheaded the cloud migration initiative, reducing latency by 40%.",
                        "I was helping out with the cloud migration work.",
                        "I did cloud migration for my team."
                    ),
                    correctOptionIndex = 0,
                    grammarExplanation = "'Spearheaded' clearly demonstrates ownership and measurable impact, standard in executive resumes.",
                    defaultCompleted = false
                ),
                RoadmapStep(
                    id = "u3_s3",
                    unitId = "unit_3",
                    stepNumber = 3,
                    title = "Pronunciation exercise",
                    type = RoadmapStepType.PRONUNCIATION,
                    subtitle = "Downward inflection for authority",
                    targetSentence = "Our team exceeded all quarterly targets while optimizing operational expenditure.",
                    phoneticTip = "Drop pitch at the end of declarative sentences to convey confidence and certainty.",
                    defaultCompleted = false
                )
            )
        )
    )
}
