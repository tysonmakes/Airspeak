package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.RoleplayScenario
import com.example.data.local.entity.RoleplayMessage
import com.example.data.local.entity.WeaknessItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class EvaluationResult(
    val overallScore: Int,
    val fluencyScore: Int,
    val grammarScore: Int,
    val vocabScore: Int,
    val pronunciationScore: Int,
    val wordsPerMinute: Int,
    val paceRating: String,
    val fillerWords: List<FillerWordOccurrence>,
    val grammarCorrections: List<GrammarCorrection>,
    val pronunciationTips: List<PronunciationTip>,
    val positivePraise: String,
    val executiveSummary: String
)

data class FillerWordOccurrence(
    val word: String,
    val count: Int
)

data class GrammarCorrection(
    val originalPhrase: String,
    val correctedPhrase: String,
    val errorCategory: String,
    val ruleExplanation: String
)

data class PronunciationTip(
    val word: String,
    val ipaPhonetic: String,
    val stressNote: String,
    val audioTip: String
)

data class RoleplayTurnResult(
    val aiResponse: String,
    val coachingFeedback: String?,
    val grammarCorrection: GrammarCorrection?
)

data class TargetedDrill(
    val id: String,
    val question: String,
    val incorrectSentence: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val sourceCategory: String
)

class AiEvaluationService {

    suspend fun evaluateSpeech(
        topic: String,
        transcript: String,
        durationSeconds: Int
    ): EvaluationResult = withContext(Dispatchers.IO) {
        val cleanTranscript = transcript.trim()
        val duration = max(1, durationSeconds)
        val wordCount = cleanTranscript.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val computedWpm = ((wordCount.toFloat() / duration) * 60).roundToInt()

        if (GeminiClient.hasValidApiKey()) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val prompt = """
                    You are an expert English speech and fluency examiner (combining Stimuler and Airlearn coaching).
                    Analyze the following spoken English transcript for the topic: "$topic".
                    Speaking Duration: $duration seconds. Calculated Word Count: $wordCount words (Approx WPM: $computedWpm).
                    
                    Transcript:
                    "$cleanTranscript"
                    
                    Return a valid JSON object ONLY with the following structure:
                    {
                      "overallScore": integer between 40 and 99,
                      "fluencyScore": integer between 40 and 99,
                      "grammarScore": integer between 40 and 99,
                      "vocabScore": integer between 40 and 99,
                      "pronunciationScore": integer between 40 and 99,
                      "wpm": $computedWpm,
                      "paceRating": "Optimal & Natural" or "Slightly Hesitant" or "Too Rushed",
                      "fillerWords": [
                        {"word": "um", "count": 2}
                      ],
                      "grammarCorrections": [
                        {
                          "originalPhrase": "exact phrase user said",
                          "correctedPhrase": "natural native English correction",
                          "errorCategory": "e.g. Subject-Verb Agreement / Tense / Preposition",
                          "ruleExplanation": "clear brief reason why"
                        }
                      ],
                      "pronunciationTips": [
                        {
                          "word": "difficult word used",
                          "ipaPhonetic": "/phonetic/",
                          "stressNote": "e.g. stress on 2nd syllable",
                          "audioTip": "how to enunciate clearly"
                        }
                      ],
                      "positivePraise": "1-2 encouraging sentences highlighting what the speaker did well",
                      "executiveSummary": "Concise 2-sentence actionable coaching roadmap"
                    }
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = prompt))
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.2f,
                        responseMimeType = "application/json"
                    )
                )

                val response = GeminiClient.api.generateContent(apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawText.isNullOrBlank()) {
                    return@withContext parseGeminiEvaluation(rawText, computedWpm)
                }
            } catch (e: Exception) {
                Log.e("AiEvaluationService", "Gemini API error, falling back to local analysis", e)
            }
        }

        // High-fidelity fallback heuristic evaluator
        return@withContext runLocalEvaluation(cleanTranscript, duration, computedWpm, topic)
    }

    private fun parseGeminiEvaluation(rawJson: String, fallbackWpm: Int): EvaluationResult {
        // Strip markdown code blocks if present
        val jsonStr = rawJson.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = JSONObject(jsonStr)

        val fillerList = mutableListOf<FillerWordOccurrence>()
        val fillerArr = json.optJSONArray("fillerWords")
        if (fillerArr != null) {
            for (i in 0 until fillerArr.length()) {
                val item = fillerArr.getJSONObject(i)
                fillerList.add(
                    FillerWordOccurrence(
                        word = item.optString("word", "filler"),
                        count = item.optInt("count", 1)
                    )
                )
            }
        }

        val grammarList = mutableListOf<GrammarCorrection>()
        val grammarArr = json.optJSONArray("grammarCorrections")
        if (grammarArr != null) {
            for (i in 0 until grammarArr.length()) {
                val item = grammarArr.getJSONObject(i)
                grammarList.add(
                    GrammarCorrection(
                        originalPhrase = item.optString("originalPhrase"),
                        correctedPhrase = item.optString("correctedPhrase"),
                        errorCategory = item.optString("errorCategory", "Grammar"),
                        ruleExplanation = item.optString("ruleExplanation")
                    )
                )
            }
        }

        val pronList = mutableListOf<PronunciationTip>()
        val pronArr = json.optJSONArray("pronunciationTips")
        if (pronArr != null) {
            for (i in 0 until pronArr.length()) {
                val item = pronArr.getJSONObject(i)
                pronList.add(
                    PronunciationTip(
                        word = item.optString("word"),
                        ipaPhonetic = item.optString("ipaPhonetic"),
                        stressNote = item.optString("stressNote"),
                        audioTip = item.optString("audioTip")
                    )
                )
            }
        }

        val wpm = json.optInt("wpm", fallbackWpm)
        val pace = json.optString("paceRating", if (wpm in 120..160) "Optimal & Natural" else if (wpm < 120) "Hesitant & Deliberate" else "Fast Paced")

        return EvaluationResult(
            overallScore = json.optInt("overallScore", 78),
            fluencyScore = json.optInt("fluencyScore", 75),
            grammarScore = json.optInt("grammarScore", 80),
            vocabScore = json.optInt("vocabScore", 76),
            pronunciationScore = json.optInt("pronunciationScore", 82),
            wordsPerMinute = wpm,
            paceRating = pace,
            fillerWords = fillerList,
            grammarCorrections = grammarList,
            pronunciationTips = pronList,
            positivePraise = json.optString("positivePraise", "Great effort! Your ideas came through clearly with good conversational momentum."),
            executiveSummary = json.optString("executiveSummary", "Focus on replacing verbal pauses with silent breaths and checking verb tenses.")
        )
    }

    private fun runLocalEvaluation(
        transcript: String,
        durationSeconds: Int,
        computedWpm: Int,
        topic: String
    ): EvaluationResult {
        val lower = transcript.lowercase(Locale.ENGLISH)

        // 1. Detect filler words
        val fillerTargets = listOf("um", "uh", "like", "you know", "actually", "basically", "sort of", "kind of", "i mean", "so")
        val detectedFillers = mutableListOf<FillerWordOccurrence>()
        var totalFillerCount = 0

        for (target in fillerTargets) {
            val pattern = Regex("\\b$target\\b", RegexOption.IGNORE_CASE)
            val matches = pattern.findAll(lower).count()
            if (matches > 0) {
                detectedFillers.add(FillerWordOccurrence(target, matches))
                totalFillerCount += matches
            }
        }

        // 2. Grammar rules detection
        val corrections = mutableListOf<GrammarCorrection>()

        val grammarRules = listOf(
            Triple(
                Regex("\\bi am agree\\b", RegexOption.IGNORE_CASE),
                "I agree",
                Pair("Verb Usage", "'Agree' is a verb, not an adjective. Avoid 'I am agree'.")
            ),
            Triple(
                Regex("\\bshe don't\\b", RegexOption.IGNORE_CASE),
                "She doesn't",
                Pair("Subject-Verb Agreement", "Third-person singular 'she' takes 'doesn't', not 'don't'.")
            ),
            Triple(
                Regex("\\bhe don't\\b", RegexOption.IGNORE_CASE),
                "He doesn't",
                Pair("Subject-Verb Agreement", "Third-person singular 'he' takes 'doesn't', not 'don't'.")
            ),
            Triple(
                Regex("\\byesterday i go\\b", RegexOption.IGNORE_CASE),
                "Yesterday I went",
                Pair("Past Tense", "Use the past tense form 'went' when referring to finished past time.")
            ),
            Triple(
                Regex("\\bmore better\\b", RegexOption.IGNORE_CASE),
                "Much better",
                Pair("Double Comparative", "'Better' is already comparative. Never say 'more better'.")
            ),
            Triple(
                Regex("\\bexplain me\\b", RegexOption.IGNORE_CASE),
                "Explain to me",
                Pair("Preposition Requirement", "'Explain' requires the preposition 'to' before the person object.")
            ),
            Triple(
                Regex("\\baccording to me\\b", RegexOption.IGNORE_CASE),
                "In my opinion",
                Pair("Collocation Nuance", "'According to' is used for external sources, not for oneself.")
            ),
            Triple(
                Regex("\\bi look forward to hear\\b", RegexOption.IGNORE_CASE),
                "I look forward to hearing",
                Pair("Gerund After Preposition", "'Look forward to' requires a gerund (-ing) noun phrase.")
            ),
            Triple(
                Regex("\\bdepend of\\b", RegexOption.IGNORE_CASE),
                "Depend on",
                Pair("Preposition Collocation", "The standard verb collocation is 'depend on', not 'depend of'.")
            ),
            Triple(
                Regex("\\bmany informations\\b", RegexOption.IGNORE_CASE),
                "A lot of information",
                Pair("Uncountable Noun", "'Information' is an uncountable noun in English with no plural form.")
            )
        )

        for ((regex, fix, meta) in grammarRules) {
            val match = regex.find(transcript)
            if (match != null) {
                corrections.add(
                    GrammarCorrection(
                        originalPhrase = match.value,
                        correctedPhrase = fix,
                        errorCategory = meta.first,
                        ruleExplanation = meta.second
                    )
                )
            }
        }

        // If no explicit error detected in simple transcript, provide a polish suggestion
        if (corrections.isEmpty() && transcript.split(" ").size > 5) {
            corrections.add(
                GrammarCorrection(
                    originalPhrase = "I think that " + transcript.take(25) + "...",
                    correctedPhrase = "From my perspective, " + transcript.take(25) + "...",
                    errorCategory = "Vocabulary & Register",
                    ruleExplanation = "Elevating sentence starters from 'I think' to 'From my perspective' immediately projects executive presence."
                )
            )
        }

        // 3. Pronunciation tips based on vocabulary found
        val pronunciationLibrary = mapOf(
            "comfortable" to Triple("/ˈkʌm.fət.ə.bəl/", "Stress 1st syllable (KUMF-ter-bl)", "Silent 'or' - condense into 3 fluid beats."),
            "colleague" to Triple("/ˈkɒl.iːɡ/", "Stress 1st syllable (KOL-eeg)", "The 'ue' at the end is silent."),
            "architecture" to Triple("/ˈɑː.kɪ.tek.tʃər/", "Stress 1st syllable (AR-ki-tek-cher)", "'ch' is pronounced like 'k', not 'ch'."),
            "resilient" to Triple("/rɪˈzɪl.jənt/", "Stress 2nd syllable (ri-ZIL-yunt)", "Soft vowel reduction at the beginning."),
            "development" to Triple("/dɪˈvel.əp.mənt/", "Stress 2nd syllable (di-VEL-up-munt)", "Do not stress 'de'; stress 'vel'."),
            "opportunity" to Triple("/ˌɒp.əˈtʃuː.nə.ti/", "Secondary stress on 'op', primary on 'tu'", "Enunciate the ending 'ti' crisply."),
            "specifically" to Triple("/spəˈsɪf.ɪ.kli/", "Stress on 2nd syllable (spe-SIF-ik-lee)", "Drop unnecessary middle syllables smoothly.")
        )

        val pronTips = mutableListOf<PronunciationTip>()
        for ((word, data) in pronunciationLibrary) {
            if (lower.contains(word)) {
                pronTips.add(
                    PronunciationTip(
                        word = word.replaceFirstChar { it.uppercase() },
                        ipaPhonetic = data.first,
                        stressNote = data.second,
                        audioTip = data.third
                    )
                )
            }
        }

        if (pronTips.isEmpty()) {
            pronTips.add(
                PronunciationTip(
                    word = "Vocal Resonance & Linking",
                    ipaPhonetic = "/kəˌnek.tɪd ˈspiːtʃ/",
                    stressNote = "Connected Speech Intonation",
                    audioTip = "Link consonant endings to vowel beginnings (e.g., 'think_about_it') for native flow."
                )
            )
        }

        // 4. Compute realistic scores
        val paceRating = when {
            computedWpm in 120..155 -> "Optimal & Natural (120-155 WPM)"
            computedWpm < 100 -> "Hesitant & Deliberate (<100 WPM)"
            computedWpm in 100..119 -> "Moderate Rhythm (100-119 WPM)"
            else -> "Fast & Energetic (>155 WPM)"
        }

        var fluency = 85
        if (totalFillerCount > 3) fluency -= (totalFillerCount * 3)
        if (computedWpm < 90 || computedWpm > 170) fluency -= 10
        fluency = fluency.coerceIn(52, 96)

        var grammar = 88 - (corrections.size * 6)
        grammar = grammar.coerceIn(55, 98)

        val vocab = if (transcript.length > 80) 84 else 75
        val pronunciation = 82

        val overall = ((fluency * 0.35) + (grammar * 0.3) + (vocab * 0.2) + (pronunciation * 0.15)).roundToInt()

        return EvaluationResult(
            overallScore = overall,
            fluencyScore = fluency,
            grammarScore = grammar,
            vocabScore = vocab,
            pronunciationScore = pronunciation,
            wordsPerMinute = computedWpm,
            paceRating = paceRating,
            fillerWords = detectedFillers,
            grammarCorrections = corrections,
            pronunciationTips = pronTips,
            positivePraise = "Strong effort! Your communicative intent was articulate and you maintained speaking momentum on '$topic'.",
            executiveSummary = "Practice breathing pauses to eliminate '${detectedFillers.firstOrNull()?.word ?: "filler words"}' and refine past/present verb consistency."
        )
    }

    suspend fun generateRoleplayReply(
        scenario: RoleplayScenario,
        conversationHistory: List<RoleplayMessage>,
        userMessage: String
    ): RoleplayTurnResult = withContext(Dispatchers.IO) {
        val cleanMsg = userMessage.trim()

        if (GeminiClient.hasValidApiKey()) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val historyString = conversationHistory.takeLast(6).joinToString("\n") {
                    "${it.sender.uppercase()}: ${it.message}"
                }

                val prompt = """
                    You are roleplaying as ${scenario.roleName} in the scenario: "${scenario.title}".
                    Scenario description: ${scenario.description}
                    
                    Recent dialogue history:
                    $historyString
                    
                    User just said:
                    "$cleanMsg"
                    
                    Respond naturally in character (2-3 spoken sentences). Keep your tone authentic, conversational, and engaging.
                    Also analyze user's message for:
                    1. Brief coaching feedback (e.g. tone, vocabulary choice, fluency).
                    2. If there's any grammar mistake, provide a side-by-side correction.
                    
                    Return a JSON object ONLY:
                    {
                      "aiResponse": "your spoken dialogue reply in character",
                      "coachingFeedback": "1 sentence constructive communication tip",
                      "grammarCorrection": null or {
                         "originalPhrase": "what user said",
                         "correctedPhrase": "corrected native phrase",
                         "errorCategory": "Grammar/Collocation",
                         "ruleExplanation": "brief why"
                      }
                    }
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = prompt))
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.5f,
                        responseMimeType = "application/json"
                    )
                )

                val response = GeminiClient.api.generateContent(apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawText.isNullOrBlank()) {
                    val jsonStr = rawText.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                    val json = JSONObject(jsonStr)

                    var correction: GrammarCorrection? = null
                    val corrObj = json.optJSONObject("grammarCorrection")
                    if (corrObj != null) {
                        correction = GrammarCorrection(
                            originalPhrase = corrObj.optString("originalPhrase"),
                            correctedPhrase = corrObj.optString("correctedPhrase"),
                            errorCategory = corrObj.optString("errorCategory", "Grammar"),
                            ruleExplanation = corrObj.optString("ruleExplanation")
                        )
                    }

                    return@withContext RoleplayTurnResult(
                        aiResponse = json.optString("aiResponse"),
                        coachingFeedback = json.optString("coachingFeedback", null),
                        grammarCorrection = correction
                    )
                }
            } catch (e: Exception) {
                Log.e("AiEvaluationService", "Gemini roleplay error, using intelligent fallback", e)
            }
        }

        return@withContext runLocalRoleplayReply(scenario, cleanMsg)
    }

    private fun runLocalRoleplayReply(
        scenario: RoleplayScenario,
        userMessage: String
    ): RoleplayTurnResult {
        val lower = userMessage.lowercase()

        // Quick heuristic check for common grammar slip
        var correction: GrammarCorrection? = null
        if (lower.contains("i am agree")) {
            correction = GrammarCorrection("i am agree", "I agree", "Verb Form", "Say 'I agree' instead of 'I am agree'.")
        } else if (lower.contains("more better")) {
            correction = GrammarCorrection("more better", "much better", "Double Comparative", "'Better' is already comparative.")
        } else if (lower.contains("she don't") || lower.contains("he don't")) {
            correction = GrammarCorrection("don't", "doesn't", "Subject-Verb", "Use 'doesn't' with 3rd-person singular.")
        }

        val (reply, tip) = when (scenario.id) {
            "interview" -> {
                if (lower.contains("lead") || lower.contains("team") || lower.contains("project")) {
                    Pair(
                        "That demonstrates solid initiative. When guiding cross-functional teams, how do you handle misaligned priorities or pushback from stakeholders?",
                        "Noticeable clarity! Try introducing quantifiable metrics (e.g., 'reduced turnaround by 25%') to substantiate your narrative."
                    )
                } else {
                    Pair(
                        "That gives good perspective. Could you dive deeper into the technical architecture or reasoning behind that decision?",
                        "Great pacing. Keep your answers structured with Situation-Task-Action-Result (STAR)."
                    )
                }
            }
            "cafe" -> {
                if (lower.contains("coffee") || lower.contains("latte") || lower.contains("cappuccino") || lower.contains("oat")) {
                    Pair(
                        "Coming right up! Oat milk flat white with a silky foam. Would you like a warm almond croissant or sourdough toast to go with that?",
                        "Very natural cadence! Phrasing it as 'Could I get an oat flat white, please?' sounds exceptionally native."
                    )
                } else {
                    Pair(
                        "Certainly! Take your time. We also have a lovely cold brew and drip of the day. Anything else for your table?",
                        "Polite and friendly tone. Connecting 'Can I have' smoothly aids casual conversation."
                    )
                }
            }
            "airport" -> {
                Pair(
                    "Thank you. Everything appears in order with your documentation. How many pieces of checked luggage do you have with you today?",
                    "Direct and concise responses are key during customs interactions."
                )
            }
            "debate" -> {
                Pair(
                    "You make a valid point about individual focus, but how do serendipitous water-cooler conversations and mentorship survive in fully remote environments?",
                    "Compelling stance! Try acknowledging the counter-perspective before refuting it: 'While spontaneous interaction has value, modern async tools...'"
                )
            }
            else -> {
                Pair(
                    "That sounds really exciting! I've been wanting to try something similar myself. Have you been doing that for long, or is it a new pursuit?",
                    "Warm social inflection! Using open-ended questions keeps the dialogue reciprocal and lively."
                )
            }
        }

        return RoleplayTurnResult(
            aiResponse = reply,
            coachingFeedback = tip,
            grammarCorrection = correction
        )
    }

    fun generateTargetedDrills(weaknesses: List<WeaknessItem>): List<TargetedDrill> {
        val drills = mutableListOf<TargetedDrill>()

        weaknesses.take(6).forEachIndexed { index, item ->
            val wrong = item.userSaid
            val right = item.correction

            val options = listOf(
                right,
                wrong,
                "Neither is standard English",
                right.replace(" ", " and ")
            ).distinct().shuffled()

            val correctIdx = options.indexOf(right)

            drills.add(
                TargetedDrill(
                    id = "drill_$index",
                    question = "Select the grammatically accurate and natural phrasing:",
                    incorrectSentence = wrong,
                    options = options,
                    correctIndex = if (correctIdx >= 0) correctIdx else 0,
                    explanation = item.explanation.ifBlank { "The standard native expression is: $right" },
                    sourceCategory = item.category
                )
            )
        }

        // Add default foundational drills if weakness bank has few items
        if (drills.size < 3) {
            drills.add(
                TargetedDrill(
                    id = "drill_default_1",
                    question = "Fix the common expression:",
                    incorrectSentence = "I am agree with your presentation.",
                    options = listOf("I am agreeing with your presentation.", "I agree with your presentation.", "I am agreeable for your presentation.", "I agrees with your presentation."),
                    correctIndex = 1,
                    explanation = "'Agree' is a verb. Say 'I agree', never 'I am agree'.",
                    sourceCategory = "Grammar"
                )
            )
            drills.add(
                TargetedDrill(
                    id = "drill_default_2",
                    question = "Select the correct past tense construction:",
                    incorrectSentence = "Yesterday I didn't went to the office.",
                    options = listOf("Yesterday I didn't go to the office.", "Yesterday I hadn't go to the office.", "Yesterday I didn't went to the office.", "Yesterday I not went to the office."),
                    correctIndex = 0,
                    explanation = "Auxiliary 'didn't' is already past tense, so it must be followed by base verb 'go'.",
                    sourceCategory = "Grammar"
                )
            )
            drills.add(
                TargetedDrill(
                    id = "drill_default_3",
                    question = "Choose the correct preposition collocation:",
                    incorrectSentence = "It all depends of the weather tomorrow.",
                    options = listOf("It all depends to the weather tomorrow.", "It all depends of the weather tomorrow.", "It all depends on the weather tomorrow.", "It all depends from the weather tomorrow."),
                    correctIndex = 2,
                    explanation = "In English, the verb 'depend' takes the preposition 'on'.",
                    sourceCategory = "Collocations"
                )
            )
        }

        return drills
    }
}
