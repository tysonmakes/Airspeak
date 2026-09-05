package com.example.data.local

import com.example.R

data class PracticeTopic(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val promptStarter: String,
    val initialAiGreeting: String,
    val starRating: Int = 2,
    val isLocked: Boolean = false,
    val iconRes: Int = R.drawable.img_tutor_emma_1788578908516
)

data class TopicChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: CallSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAudioInput: Boolean = false,
    val grammarCorrection: String? = null,
    val grammarReason: String? = null,
    val betterAlternative: String? = null,
    val fluencyScore: Int? = null
)

object PracticeTopicCatalog {
    val topics: List<PracticeTopic> = listOf(
        // Introduction
        PracticeTopic(
            id = "topic_intro",
            title = "Introduction - let's know about you",
            category = "Introduction",
            description = "Introduce yourself, your passions, and where you're from in relaxed conversational English.",
            promptStarter = "Hi there! Let's start with a warm introduction. Tell me your name, what you enjoy doing, and what brings you here today!",
            initialAiGreeting = "Hello! It's fantastic to meet you. To break the ice, could you tell me a little about where you're from and what you enjoy doing?",
            starRating = 2,
            iconRes = R.drawable.img_tutor_emma_1788578908516
        ),

        // Free Topics
        PracticeTopic(
            id = "topic_hobbies",
            title = "Hobbies",
            category = "Free Topics",
            description = "Chat about books, sports, music, photography, and how you spend weekends.",
            promptStarter = "Tell me about your favorite hobby and why it captivates you.",
            initialAiGreeting = "Hey! I'm curious, what do you usually do when you want to unwind after a long day? Any special hobbies you're passionate about?",
            starRating = 2,
            iconRes = R.drawable.img_chapter_making_friends_1788579459754
        ),
        PracticeTopic(
            id = "topic_movies",
            title = "Movies",
            category = "Free Topics",
            description = "Discuss movie plots, favorite actors, genres, and recommend must-watch films.",
            promptStarter = "What was the last movie or TV show that genuinely hooked you?",
            initialAiGreeting = "Hi! Let's talk cinema! What's the most memorable movie you've watched recently, and what made it stand out?",
            starRating = 2,
            iconRes = R.drawable.img_tutor_emma_1788578908516
        ),
        PracticeTopic(
            id = "topic_work",
            title = "Work",
            category = "Free Topics",
            description = "Talk about daily routines, your profession, challenges, and career goals.",
            promptStarter = "Describe a typical workday and what projects you're currently tackling.",
            initialAiGreeting = "Welcome! What does a typical day look like in your current work or studies? What part of it do you find most exciting?",
            starRating = 2,
            iconRes = R.drawable.img_tutor_arthur_1788578922005
        ),
        PracticeTopic(
            id = "topic_job_interviews_free",
            title = "Job Interviews",
            category = "Free Topics",
            description = "Quick interview warmup: strength/weakness, teamwork, and problem solving.",
            promptStarter = "Let's run a quick mock question: Why should we hire you for this position?",
            initialAiGreeting = "Hello candidate! Let's treat this like an interview. Could you tell me about a major achievement you're particularly proud of?",
            starRating = 2,
            iconRes = R.drawable.img_speaking_hero_1788577044196
        ),

        // Job Interview (Deep dive)
        PracticeTopic(
            id = "topic_career_story",
            title = "Telling your career story",
            category = "Job Interview",
            description = "Structure your personal professional timeline seamlessly.",
            promptStarter = "Walk me through your career journey and the turning points that shaped you.",
            initialAiGreeting = "Thanks for joining. To kick off our discussion, could you walk me through your career journey and what inspired your path?",
            starRating = 3,
            iconRes = R.drawable.img_speaking_hero_1788577044196
        ),
        PracticeTopic(
            id = "topic_tell_me_about_yourself",
            title = "'Tell me about yourself' answer",
            category = "Job Interview",
            description = "Master the famous 60-second elevator pitch for recruiters.",
            promptStarter = "Deliver your opening answer: 'Tell me about yourself.'",
            initialAiGreeting = "Welcome! Let's tackle the classic question: 'Tell me about yourself.' Whenever you're ready, take the floor!",
            starRating = 3,
            iconRes = R.drawable.img_tutor_arthur_1788578922005
        ),
        PracticeTopic(
            id = "topic_leave_job",
            title = "Why you want to leave your job",
            category = "Job Interview",
            description = "Diplomatically explain transitions and seek greater challenges.",
            promptStarter = "Frame your transition positively without sounding critical of past employers.",
            initialAiGreeting = "A critical question recruiters love to ask: 'Why are you looking to leave your current role?' How would you frame that gracefully?",
            starRating = 3,
            iconRes = R.drawable.img_chapter_making_friends_1788579459754
        ),
        PracticeTopic(
            id = "topic_negotiating_start_date",
            title = "Negotiating your start date",
            category = "Job Interview",
            description = "Communicate notice periods and relocation plans with confidence.",
            promptStarter = "How would you tell HR that you need three weeks before joining?",
            initialAiGreeting = "Congratulations on the tentative offer! The hiring team wants you to start next Monday, but you need notice. How do you respond?",
            starRating = 2,
            iconRes = R.drawable.img_tutor_emma_1788578908516
        ),
        PracticeTopic(
            id = "topic_telling_salary",
            title = "Telling hiring mgr your salary",
            category = "Job Interview",
            description = "State compensation targets, ranges, and market value firmly.",
            promptStarter = "Address the compensation question: 'What are your salary expectations?'",
            initialAiGreeting = "The recruiter asks: 'What compensation range are you targeting for this role?' How do you pitch your value?",
            starRating = 3,
            iconRes = R.drawable.img_speaking_hero_1788577044196
        ),

        // Meetings
        PracticeTopic(
            id = "topic_disagreeing_respectfully",
            title = "Disagreeing respectfully at work",
            category = "Meetings",
            description = "Express counter-arguments with tact and collaborative diplomacy.",
            promptStarter = "Use professional diplomatic language to push back against a deadline.",
            initialAiGreeting = "During a sprint planning meeting, your manager proposes an unrealistic deadline. How do you voice your concern respectfully?",
            starRating = 3,
            iconRes = R.drawable.img_tutor_arthur_1788578922005
        ),
        PracticeTopic(
            id = "topic_speaking_up",
            title = "Speaking up in meetings",
            category = "Meetings",
            description = "Interject smoothly, hold the floor, and articulate insights.",
            promptStarter = "Practice interjecting in a fast-paced meeting to share an important observation.",
            initialAiGreeting = "Everyone is debating heatedly in the room and you have an important insight to add. How do you interject and capture their attention?",
            starRating = 2,
            iconRes = R.drawable.img_chapter_making_friends_1788579459754
        )
    )
}
