package com.example.data.local

import com.example.data.local.entity.VocabularyWord
import com.example.data.local.entity.WeaknessItem

data class RoleplayScenario(
    val id: String,
    val title: String,
    val roleName: String,
    val description: String,
    val initialAiMessage: String,
    val tips: List<String>,
    val iconName: String
)

data class SpeakingTopic(
    val id: String,
    val title: String,
    val category: String,
    val prompt: String,
    val sampleKeywords: List<String>
)

object DefaultData {

    val speakingTopics = listOf(
        SpeakingTopic(
            id = "topic_intro",
            title = "Personal Introduction & Passions",
            category = "Daily & Social",
            prompt = "Introduce yourself, explain your profession or studies, and share a hobby you are truly passionate about.",
            sampleKeywords = listOf("passion", "profession", "journey", "enriching", "balance")
        ),
        SpeakingTopic(
            id = "topic_work",
            title = "Handling High-Pressure Work Situations",
            category = "Workplace & Career",
            prompt = "Describe a moment when you faced a tight deadline or unexpected setback at work or school. How did you resolve it?",
            sampleKeywords = listOf("deadline", "prioritize", "resolution", "collaborate", "resilience")
        ),
        SpeakingTopic(
            id = "topic_travel",
            title = "A Memorable Journey or Culture",
            category = "Travel & Culture",
            prompt = "Talk about a place you visited or would love to explore. What cultural differences or memorable moments stand out?",
            sampleKeywords = listOf("breathtaking", "hospitality", "architecture", "perspective", "scenery")
        ),
        SpeakingTopic(
            id = "topic_ai_debate",
            title = "AI in Everyday Life",
            category = "Opinion & Debate",
            prompt = "Do you believe artificial intelligence will improve human creativity or make us overly dependent? Give reasons.",
            sampleKeywords = listOf("transformative", "ethical", "productivity", "critical thinking", "automation")
        )
    )

    val roleplayScenarios = listOf(
        RoleplayScenario(
            id = "interview",
            title = "Tech Job Interview",
            roleName = "Sarah Jenkins (Hiring Director)",
            description = "Practice answering behavioral interview questions, articulating career impact, and sounding confident under scrutiny.",
            initialAiMessage = "Hello! Thanks for joining today. I reviewed your background and was impressed. To kick things off, could you walk me through a complex challenge you led and the measurable outcome?",
            tips = listOf("Use STAR method (Situation, Task, Action, Result)", "Avoid filler words like 'um' and 'like'", "Quantify your impact"),
            iconName = "Work"
        ),
        RoleplayScenario(
            id = "cafe",
            title = "Ordering at Specialty Cafe",
            roleName = "Liam (London Barista)",
            description = "Natural everyday conversational fluency: ordering drinks, asking for custom dairy alternatives, and handling small talk.",
            initialAiMessage = "Hi there, good morning! Welcome to Roastery & Co. What can I brew for you today? We have a fresh batch of Ethiopian single-origin on pour-over.",
            tips = listOf("Use polite phrases: 'Could I have...', 'Would it be possible...'", "Practice linking words naturally"),
            iconName = "Coffee"
        ),
        RoleplayScenario(
            id = "airport",
            title = "Airport Check-In & Customs",
            roleName = "Officer Vance (Border Border Agent)",
            description = "Clear, precise communication: explaining travel intent, presenting documents, and answering customs queries.",
            initialAiMessage = "Good afternoon. Passport and boarding pass, please. What is the primary purpose of your visit, and how long do you plan to stay?",
            tips = listOf("Speak concisely and directly", "Use clear past and future tenses"),
            iconName = "Flight"
        ),
        RoleplayScenario(
            id = "debate",
            title = "Debate: Remote vs Office",
            roleName = "Marcus (Debate Challenger)",
            description = "Persuasive rhetoric: structuring arguments, conceding politely, and defending your viewpoint on workplace culture.",
            initialAiMessage = "I firmly believe full-time in-person collaboration is irreplaceable for innovation. Remote work fragments company culture. How do you respond?",
            tips = listOf("Acknowledge counterarguments: 'While that may be true...'", "Offer concrete counter-evidence"),
            iconName = "Forum"
        ),
        RoleplayScenario(
            id = "casual",
            title = "Weekend Social Chit-Chat",
            roleName = "Chloe (Friend & Colleague)",
            description = "Casual idioms, storytelling, sharing weekend recommendations, and informal conversational rhythm.",
            initialAiMessage = "Hey! So glad we caught each other. I desperately need to unwind this weekend. Do you have any plans lined up or recommendations for a good show?",
            tips = listOf("Use conversational connectors: 'Actually', 'Funny you should ask'", "Keep your tone relaxed"),
            iconName = "People"
        )
    )

    val initialWords = listOf(
        // Beginner Level (A1-A2)
        VocabularyWord(
            word = "Eloquent",
            phonetic = "/ˈel.ə.kwənt/",
            partOfSpeech = "adjective",
            level = "Beginner",
            definition = "Giving a clear, strong message; expressing oneself fluently and persuasively.",
            exampleSentence = "She made an eloquent appeal for greater understanding between colleagues.",
            synonyms = "articulate, persuasive, expressive",
            antonyms = "inarticulate, hesitant",
            intervalDays = 1
        ),
        VocabularyWord(
            word = "Resilient",
            phonetic = "/rɪˈzɪl.jənt/",
            partOfSpeech = "adjective",
            level = "Beginner",
            definition = "Able to quickly recover from difficulties, setbacks, or tough conditions.",
            exampleSentence = "The team remained resilient despite multiple technical obstacles.",
            synonyms = "tough, adaptable, enduring",
            antonyms = "fragile, vulnerable",
            intervalDays = 1
        ),
        VocabularyWord(
            word = "Collaborate",
            phonetic = "/kəˈlæb.ə.reɪt/",
            partOfSpeech = "verb",
            level = "Beginner",
            definition = "To work jointly with others on an activity or project to produce something.",
            exampleSentence = "Engineers and designers collaborated closely to release the mobile app on time.",
            synonyms = "cooperate, team up, coordinate",
            antonyms = "disagree, oppose",
            intervalDays = 1
        ),
        VocabularyWord(
            word = "Hesitate",
            phonetic = "/ˈhez.ɪ.teɪt/",
            partOfSpeech = "verb",
            level = "Beginner",
            definition = "To pause before saying or doing something, often due to doubt or uncertainty.",
            exampleSentence = "Do not hesitate to reach out if you have any questions during the lesson.",
            synonyms = "waver, falter, pause",
            antonyms = "decide, proceed",
            intervalDays = 1
        ),

        // Intermediate Level (B1-B2)
        VocabularyWord(
            word = "Articulate",
            phonetic = "/ɑːˈtɪk.jə.lət/",
            partOfSpeech = "adjective & verb",
            level = "Intermediate",
            definition = "Able to express thoughts and feelings easily and clearly in words.",
            exampleSentence = "He was very articulate about the reasons why the project needed to pivot.",
            synonyms = "coherent, lucid, fluent",
            antonyms = "unclear, mumbled",
            intervalDays = 1
        ),
        VocabularyWord(
            word = "Nuance",
            phonetic = "/ˈnjuː.ɑːns/",
            partOfSpeech = "noun",
            level = "Intermediate",
            definition = "A very slight difference in appearance, meaning, sound, or tone.",
            exampleSentence = "Language learners must pay attention to subtleties and nuances in tone.",
            synonyms = "subtlety, shade, variation",
            antonyms = "overstatement, generality",
            intervalDays = 1
        ),
        VocabularyWord(
            word = "Pragmatic",
            phonetic = "/præɡˈmæt.ɪk/",
            partOfSpeech = "adjective",
            level = "Intermediate",
            definition = "Dealing with things sensibly and realistically based on practical conditions rather than theoretical ideas.",
            exampleSentence = "We need to adopt a pragmatic approach to meet our product roadmap deadline.",
            synonyms = "practical, realistic, sensible",
            antonyms = "idealistic, impractical",
            intervalDays = 1
        ),
        VocabularyWord(
            word = "Exemplify",
            phonetic = "/ɪɡˈzem.plɪ.faɪ/",
            partOfSpeech = "verb",
            level = "Intermediate",
            definition = "To be a typical example of something, or to explain something by giving an example.",
            exampleSentence = "Her presentation exemplified the standard of professionalism we strive for.",
            synonyms = "illustrate, demonstrate, epitomize",
            antonyms = "obscure, distort",
            intervalDays = 1
        ),

        // Advanced Level (C1-C2)
        VocabularyWord(
            word = "Perspicacious",
            phonetic = "/ˌpɜː.spɪˈkeɪ.ʃəs/",
            partOfSpeech = "adjective",
            level = "Advanced",
            definition = "Having a quick, deep insight and understanding of things; discerning.",
            exampleSentence = "His perspicacious analysis of market trends allowed the startup to stay ahead.",
            synonyms = "astute, perceptive, sharp-witted",
            antonyms = "obtuse, naive",
            intervalDays = 1
        ),
        VocabularyWord(
            word = "Ubiquitous",
            phonetic = "/juːˈbɪk.wɪ.təs/",
            partOfSpeech = "adjective",
            level = "Advanced",
            definition = "Present, appearing, or found everywhere at once.",
            exampleSentence = "Smartphones have become ubiquitous companions in modern society.",
            synonyms = "omnipresent, pervasive, universal",
            antonyms = "rare, scarce",
            intervalDays = 1
        ),
        VocabularyWord(
            word = "Cogent",
            phonetic = "/ˈkəʊ.dʒənt/",
            partOfSpeech = "adjective",
            level = "Advanced",
            definition = "Powerfully persuasive, clear, logical, and convincing.",
            exampleSentence = "She put forward a cogent argument that won over even the skeptical board members.",
            synonyms = "compelling, potent, well-founded",
            antonyms = "unconvincing, flimsy",
            intervalDays = 1
        ),
        VocabularyWord(
            word = "Ephemeral",
            phonetic = "/ɪˈfem.ər.əl/",
            partOfSpeech = "adjective",
            level = "Advanced",
            definition = "Lasting for only a very short time; transient.",
            exampleSentence = "Viral internet trends are notoriously ephemeral, disappearing within days.",
            synonyms = "transitory, fleeting, momentary",
            antonyms = "permanent, perpetual",
            intervalDays = 1
        )
    )

    val initialWeaknesses = listOf(
        WeaknessItem(
            category = "Grammar",
            userSaid = "I am agree with your opinion.",
            correction = "I agree with your opinion.",
            explanation = "'Agree' is a verb in English, not an adjective. Say 'I agree', not 'I am agree'.",
            phoneticTip = "Stress on the second syllable: uh-GREE.",
            sourceSession = "Roleplay: Tech Job Interview",
            isMastered = false,
            practiceCount = 1
        ),
        WeaknessItem(
            category = "Pronunciation",
            userSaid = "Comfortable (pronounced as com-for-tay-ble)",
            correction = "Comfortable /ˈkʌm.fət.ə.bəl/ (pronounced as KUMF-ter-bl)",
            explanation = "Notice the silent 'or' in casual spoken English; compress into 3 syllables: KUMF-ter-bl.",
            phoneticTip = "Primary stress on first syllable: KUMF.",
            sourceSession = "Speaking Practice: Cafe",
            isMastered = false,
            practiceCount = 2
        ),
        WeaknessItem(
            category = "Filler Words",
            userSaid = "Um, like, basically I want to, uh, explain the architecture.",
            correction = "I would like to explain the architecture clearly.",
            explanation = "Pausing in silence for 1 second instead of saying 'um' or 'like' makes your speech 3x more authoritative.",
            phoneticTip = "Take a calm breath instead of vocalizing fillers.",
            sourceSession = "Speaking Test: Job Interview",
            isMastered = false,
            practiceCount = 0
        ),
        WeaknessItem(
            category = "Grammar",
            userSaid = "She don't know the answer yesterday.",
            correction = "She didn't know the answer yesterday.",
            explanation = "Past tense negative requires 'did not' (didn't), and 3rd-person present would be 'doesn't'.",
            phoneticTip = "Link 'didn't know': DID-nt-no.",
            sourceSession = "Speaking Practice: Daily Social",
            isMastered = false,
            practiceCount = 1
        )
    )
}
