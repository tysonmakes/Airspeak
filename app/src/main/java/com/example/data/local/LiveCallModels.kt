package com.example.data.local

import com.example.R
import java.util.Locale

data class LiveCallTutor(
    val id: String,
    val name: String,
    val roleTitle: String,
    val origin: String,
    val avatarRes: Int,
    val accentLocale: Locale,
    val speechPitch: Float = 1.0f,
    val speechRate: Float = 1.0f,
    val geminiVoiceName: String = "Aoede", // Puck, Aoede, Fenrir, Kore, Charon
    val edgeVoiceName: String = "en-US-AnaNeural",
    val greeting: String,
    val bio: String,
    val specialties: List<String>,
    val defaultTopics: List<String>
)

object TutorCatalog {
    val tutors = listOf(
        LiveCallTutor(
            id = "tutor_gemini",
            name = "Gemini 2.5 Live",
            roleTitle = "Google AI Conversational Intelligence",
            origin = "Google AI Studio",
            avatarRes = R.drawable.img_tutor_emma_1788578908516,
            accentLocale = Locale.US,
            speechPitch = 1.0f,
            speechRate = 1.0f,
            geminiVoiceName = "Aoede",
            edgeVoiceName = "en-US-AnaNeural",
            greeting = "Hello! I am Gemini 2.5 Live, powered directly by Google's native conversational intelligence. What topic shall we explore together today?",
            bio = "Official Google Gemini 2.5 Flash direct live conversational agent. Speaks with high-clarity native neural voice, adapts dynamically to any topic, and provides intelligent instant corrections.",
            specialties = listOf("Direct Gemini Intelligence", "Adaptive Free Flow", "Instant Grammar Mastery"),
            defaultTopics = listOf(
                "Open Casual Conversation",
                "Advanced English Vocabulary",
                "Tech & Artificial Intelligence",
                "Daily English Fluency Drill",
                "Philosophy & Deep Discussions"
            )
        ),
        LiveCallTutor(
            id = "tutor_emma",
            name = "Emma Watson",
            roleTitle = "Friendly Native Speaker",
            origin = "San Francisco, USA",
            avatarRes = R.drawable.img_tutor_emma_1788578908516,
            accentLocale = Locale.US,
            speechPitch = 1.05f,
            speechRate = 1.0f,
            geminiVoiceName = "Aoede",
            edgeVoiceName = "en-US-AnaNeural",
            greeting = "Hey there! I'm Emma. It's awesome to chat with you today! What's on your mind, or should we talk about our day?",
            bio = "Warm, encouraging conversationalist specializing in everyday fluency, natural slang, and boosting speaking confidence.",
            specialties = listOf("Conversational Flow", "Confidence Building", "Idioms & Phrasal Verbs"),
            defaultTopics = listOf(
                "Daily Life & Routines",
                "Travel & Weekend Plans",
                "Favorite Movies & Music",
                "Cultural Differences",
                "Open Casual Chat"
            )
        ),
        LiveCallTutor(
            id = "tutor_arthur",
            name = "Dr. Arthur Pendelton",
            roleTitle = "Oxford Pronunciation Coach",
            origin = "Oxford, United Kingdom",
            avatarRes = R.drawable.img_tutor_arthur_1788578922005,
            accentLocale = Locale.UK,
            speechPitch = 0.95f,
            speechRate = 0.98f,
            geminiVoiceName = "Charon",
            edgeVoiceName = "en-GB-RyanNeural",
            greeting = "Good day! I am Dr. Arthur. We shall sharpen your articulation, cadence, and vocabulary precision today. Shall we begin?",
            bio = "Linguistics scholar dedicated to immaculate British English pronunciation, advanced discourse markers, and presentation elegance.",
            specialties = listOf("RP Accent Clarity", "Academic Vocabulary", "Discourse Markers"),
            defaultTopics = listOf(
                "Global Economics & Tech",
                "Art, Literature & Philosophy",
                "Formal Presentation Practice",
                "Scientific Innovations",
                "Advanced Grammar Polish"
            )
        ),
        LiveCallTutor(
            id = "tutor_david",
            name = "David Chen",
            roleTitle = "Silicon Valley Tech Lead",
            origin = "Seattle, USA",
            avatarRes = R.drawable.img_tutor_emma_1788578908516, // fallback
            accentLocale = Locale.US,
            speechPitch = 0.98f,
            speechRate = 1.0f,
            geminiVoiceName = "Puck",
            edgeVoiceName = "en-US-GuyNeural",
            greeting = "Hi! David here. Ready to nail your behavioral questions, system architecture pitching, or career discussions?",
            bio = "Seasoned hiring manager helping software engineers and tech professionals communicate technical concepts with punchy clarity.",
            specialties = listOf("Job Interviews", "Technical Pitching", "STAR Method Responses"),
            defaultTopics = listOf(
                "Behavioral Interview: Overcoming Conflict",
                "Pitching a Project Architecture",
                "Salary & Career Growth Talk",
                "Agile Team Standup Simulation",
                "Explaining Complex Ideas Simply"
            )
        ),
        LiveCallTutor(
            id = "tutor_sophia",
            name = "Sophia Taylor",
            roleTitle = "IELTS & TOEFL Examiner",
            origin = "Melbourne, Australia",
            avatarRes = R.drawable.img_tutor_arthur_1788578922005, // fallback
            accentLocale = Locale.US,
            speechPitch = 1.0f,
            speechRate = 1.0f,
            geminiVoiceName = "Kore",
            edgeVoiceName = "en-US-JennyNeural",
            greeting = "Welcome to your speaking assessment simulation. I am Examiner Sophia. We will focus on lexical resource and coherence.",
            bio = "Certified IELTS speaking examiner testing Part 1, 2 (cue card 2-minute talk), and Part 3 analytical discussions.",
            specialties = listOf("Band 8+ Criteria", "Complex Sentences", "Hesitation Reduction"),
            defaultTopics = listOf(
                "IELTS Part 1: Hometown & Leisure",
                "IELTS Part 2: Describe a Memorable Journey",
                "IELTS Part 3: Urbanization & Climate",
                "TOEFL Independent Speaking Task",
                "Expressing Abstract Opinions"
            )
        )
    )
}

data class LiveCallTranscriptItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: CallSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val liveCorrection: String? = null,
    val livePraise: String? = null,
    val turnScore: Int = 85
)

enum class CallSender {
    USER, AI
}

data class LiveCallSummaryReport(
    val tutorName: String,
    val tutorRole: String,
    val topic: String,
    val callDurationSeconds: Long,
    val turnsExchanged: Int,
    val averageWpm: Int,
    val overallFluencyScore: Int,
    val cefrBand: String,
    val fillerWordsCount: Int,
    val correctionsCount: Int,
    val correctionsList: List<LiveCallCorrectionItem>,
    val highlights: List<String>,
    val finalCoachAdvice: String
)

data class LiveCallCorrectionItem(
    val originalSaid: String,
    val correctedVersion: String,
    val reason: String
)
