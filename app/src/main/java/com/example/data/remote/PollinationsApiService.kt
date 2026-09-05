package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Zero-Authentication, Keyless LLM Client utilizing Pollinations.ai endpoints.
 * Requires NO API keys, NO user registration, NO paid proxies, and NO tokens.
 */
class PollinationsApiService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        var g4fBackendUrl: String = "http://10.0.2.2:1337/v1/chat/completions"
    }

    /**
     * Send a prompt to Pollinations.ai (Primary), g4f GPT4Free (Backup), without any API key.
     */
    suspend fun queryKeylessLlm(prompt: String, systemPrompt: String? = null): String? = withContext(Dispatchers.IO) {
        // 1. Primary: Pollinations.ai POST
        try {
            val messagesArray = JSONArray()
            if (!systemPrompt.isNullOrBlank()) {
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
            }
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })

            val payload = JSONObject().apply {
                put("messages", messagesArray)
                put("model", "openai")
            }

            val request = Request.Builder()
                .url("https://text.pollinations.ai/")
                .header("User-Agent", "AirSpeak-Android/1.0")
                .header("Accept", "text/plain, application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()?.trim()
                if (!body.isNullOrBlank() && !body.startsWith("{\"error\"")) {
                    return@withContext body
                }
            }
        } catch (e: Exception) {
            Log.w("PollinationsApiService", "POST to Pollinations failed: ${e.message}")
        }

        // 2. Pollinations.ai GET endpoint fallback
        try {
            val encodedPrompt = URLEncoder.encode(
                if (systemPrompt != null) "$systemPrompt\n\n$prompt" else prompt,
                "UTF-8"
            )
            val getRequest = Request.Builder()
                .url("https://text.pollinations.ai/$encodedPrompt?model=openai")
                .header("User-Agent", "AirSpeak-Android/1.0")
                .get()
                .build()

            val getResponse = client.newCall(getRequest).execute()
            if (getResponse.isSuccessful) {
                val body = getResponse.body?.string()?.trim()
                if (!body.isNullOrBlank() && !body.startsWith("{\"error\"")) {
                    return@withContext body
                }
            }
        } catch (e: Exception) {
            Log.w("PollinationsApiService", "GET to Pollinations failed: ${e.message}")
        }

        // 3. Backup: g4f (GPT4Free) backend endpoint (OpenAI-compatible)
        try {
            val messagesArray = JSONArray()
            if (!systemPrompt.isNullOrBlank()) {
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
            }
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })

            val g4fPayload = JSONObject().apply {
                put("model", "gpt-4o")
                put("messages", messagesArray)
            }

            val g4fRequest = Request.Builder()
                .url(g4fBackendUrl)
                .post(g4fPayload.toString().toRequestBody(jsonMediaType))
                .build()

            val g4fResponse = client.newCall(g4fRequest).execute()
            if (g4fResponse.isSuccessful) {
                val g4fBody = g4fResponse.body?.string()?.trim()
                if (!g4fBody.isNullOrBlank()) {
                    val g4fJson = JSONObject(g4fBody)
                    val choices = g4fJson.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content")
                        if (!content.isNullOrBlank()) {
                            return@withContext content
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("PollinationsApiService", "g4f backup endpoint failed: ${e.message}")
        }

        return@withContext null
    }

    /**
     * Fluency & Grammar evaluation via keyless Pollinations endpoint
     */
    suspend fun evaluateSpeechKeyless(
        transcript: String,
        topic: String,
        computedWpm: Int
    ): KeylessGrammarEvaluation? = withContext(Dispatchers.IO) {
        val systemPrompt = "You are an expert English language examiner. Return ONLY a valid JSON object without markdown formatting."
        val userPrompt = """
            Analyze the following spoken English transcript on the topic '$topic':
            "$transcript"
            
            Return a JSON object with:
            {
              "correctedGrammar": [
                {
                  "original": "exact snippet with mistake",
                  "correction": "grammatically natural phrasing",
                  "category": "e.g. Subject-Verb Agreement / Tense / Preposition",
                  "explanation": "brief clear rule explanation"
                }
              ],
              "betterWordChoices": [
                {
                  "original": "simple or repeated word",
                  "suggested": "more precise, advanced, or native alternative",
                  "explanation": "why this elevates the tone"
                }
              ],
              "briefExplanation": "1-2 sentence overall coaching summary",
              "positivePraise": "1 encouraging sentence on what was expressed well",
              "overallScore": 82
            }
        """.trimIndent()

        val rawResponse = queryKeylessLlm(userPrompt, systemPrompt) ?: return@withContext null

        try {
            val cleanJson = rawResponse.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonObj = JSONObject(cleanJson)

            val corrections = mutableListOf<KeylessGrammarItem>()
            val corrArr = jsonObj.optJSONArray("correctedGrammar")
            if (corrArr != null) {
                for (i in 0 until corrArr.length()) {
                    val item = corrArr.getJSONObject(i)
                    corrections.add(
                        KeylessGrammarItem(
                            original = item.optString("original"),
                            correction = item.optString("correction"),
                            category = item.optString("category", "Grammar"),
                            explanation = item.optString("explanation")
                        )
                    )
                }
            }

            val wordChoices = mutableListOf<KeylessWordChoiceItem>()
            val choicesArr = jsonObj.optJSONArray("betterWordChoices")
            if (choicesArr != null) {
                for (i in 0 until choicesArr.length()) {
                    val item = choicesArr.getJSONObject(i)
                    wordChoices.add(
                        KeylessWordChoiceItem(
                            original = item.optString("original"),
                            suggested = item.optString("suggested"),
                            explanation = item.optString("explanation")
                        )
                    )
                }
            }

            return@withContext KeylessGrammarEvaluation(
                correctedGrammar = corrections,
                betterWordChoices = wordChoices,
                briefExplanation = jsonObj.optString("briefExplanation", "Communication was effective with key areas for grammar polish."),
                positivePraise = jsonObj.optString("positivePraise", "Great job expressing your ideas with good clarity."),
                overallScore = jsonObj.optInt("overallScore", 80)
            )
        } catch (e: Exception) {
            Log.e("PollinationsApiService", "Failed to parse keyless evaluation JSON", e)
            return@withContext null
        }
    }

    /**
     * Keyless Roleplay Dialogue generation
     */
    suspend fun generateRoleplayDialogueKeyless(
        roleName: String,
        scenarioTitle: String,
        userMessage: String,
        recentHistory: String
    ): KeylessRoleplayResponse? = withContext(Dispatchers.IO) {
        val systemPrompt = "You are $roleName in the roleplay scenario '$scenarioTitle'. Return a JSON object ONLY."
        val prompt = """
            Recent conversation:
            $recentHistory
            
            User said: "$userMessage"
            
            Reply in character (2-3 spoken sentences). Also provide 1 communication tip and any grammar correction if applicable.
            Format JSON:
            {
              "reply": "your spoken dialogue",
              "coachingTip": "1 concise constructive advice",
              "correction": null or {
                 "original": "user mistake",
                 "correction": "correct form",
                 "rule": "why"
              }
            }
        """.trimIndent()

        val raw = queryKeylessLlm(prompt, systemPrompt) ?: return@withContext null

        try {
            val clean = raw.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val obj = JSONObject(clean)
            val reply = obj.optString("reply")
            val tip = obj.optString("coachingTip", "Keep your sentences flowing naturally.")
            var corrItem: KeylessGrammarItem? = null
            val corrObj = obj.optJSONObject("correction")
            if (corrObj != null && corrObj.has("correction")) {
                corrItem = KeylessGrammarItem(
                    original = corrObj.optString("original"),
                    correction = corrObj.optString("correction"),
                    category = "Grammar",
                    explanation = corrObj.optString("rule")
                )
            }
            return@withContext KeylessRoleplayResponse(
                reply = reply,
                coachingTip = tip,
                correction = corrItem
            )
        } catch (e: Exception) {
            Log.e("PollinationsApiService", "Error parsing keyless roleplay JSON", e)
            return@withContext null
        }
    }

    /**
     * Keyless Dynamic Vocabulary Generation
     */
    suspend fun generateDynamicVocabularyKeyless(
        level: String,
        count: Int = 5
    ): List<KeylessVocabWord>? = withContext(Dispatchers.IO) {
        val prompt = """
            Generate $count high-impact English vocabulary words for '$level' level learners.
            Return a JSON array ONLY with format:
            [
              {
                "word": "Word",
                "partOfSpeech": "verb/noun/adjective",
                "phonetic": "/.../",
                "definition": "Clear concise definition",
                "exampleSentence": "A realistic modern example sentence using the word.",
                "synonyms": "synonym1, synonym2",
                "antonyms": "antonym1, antonym2"
              }
            ]
        """.trimIndent()

        val raw = queryKeylessLlm(prompt, "You are a professional lexicographer. Return valid JSON only.") ?: return@withContext null

        try {
            val clean = raw.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val array = JSONArray(clean)
            val list = mutableListOf<KeylessVocabWord>()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                list.add(
                    KeylessVocabWord(
                        word = item.optString("word"),
                        partOfSpeech = item.optString("partOfSpeech", "noun"),
                        phonetic = item.optString("phonetic", ""),
                        definition = item.optString("definition"),
                        exampleSentence = item.optString("exampleSentence"),
                        synonyms = item.optString("synonyms"),
                        antonyms = item.optString("antonyms")
                    )
                )
            }
            return@withContext list
        } catch (e: Exception) {
            Log.e("PollinationsApiService", "Error parsing keyless vocab JSON", e)
            return@withContext null
        }
    }

    /**
     * Keyless Live Call Voice Dialogue & Realtime Fluency Analysis
     */
    suspend fun generateLiveCallTurnKeyless(
        tutorName: String,
        tutorPersona: String,
        userSpokenText: String,
        callTopic: String,
        conversationHistory: String
    ): LiveCallCoachResponse? = withContext(Dispatchers.IO) {
        val systemPrompt = "You are $tutorName, an expert native English fluency coach on a live voice phone call. Personality: $tutorPersona. Topic: $callTopic. Return JSON ONLY."
        val prompt = """
            Recent call transcript:
            $conversationHistory
            
            Learner just said: "$userSpokenText"
            
            1. Respond naturally as on a live phone call (1-2 spoken sentences, warm, engaging, ask a follow-up or comment).
            2. Check if the learner made any grammar, tense, or word choice error. If yes, write a concise correction.
            3. Give 1 live encouragement note.
            4. Rate their turn fluency score (60-100).
            
            Respond with JSON ONLY:
            {
              "spokenReply": "Conversational reply to say over the phone",
              "liveCorrection": "Concise correction or null if fine",
              "livePraise": "Brief praise or encouragement",
              "turnFluencyScore": 88
            }
        """.trimIndent()

        val raw = queryKeylessLlm(prompt, systemPrompt) ?: return@withContext null
        try {
            val cleanJson = raw.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val jsonStart = cleanJson.indexOf('{')
            val jsonEnd = cleanJson.lastIndexOf('}')
            if (jsonStart == -1 || jsonEnd <= jsonStart) return@withContext null
            val jsonObj = JSONObject(cleanJson.substring(jsonStart, jsonEnd + 1))

            val corr = jsonObj.optString("liveCorrection").takeIf { it.isNotBlank() && it != "null" }
            return@withContext LiveCallCoachResponse(
                spokenReply = jsonObj.optString("spokenReply", "I hear you! That's an intriguing perspective, tell me more about it."),
                liveCorrection = corr,
                livePraise = jsonObj.optString("livePraise", "Good clear expression!"),
                turnFluencyScore = jsonObj.optInt("turnFluencyScore", 85)
            )
        } catch (e: Exception) {
            Log.e("PollinationsApiService", "Error parsing live call JSON", e)
            return@withContext null
        }
    }
}

data class LiveCallCoachResponse(
    val spokenReply: String,
    val liveCorrection: String?,
    val livePraise: String?,
    val turnFluencyScore: Int
)

data class KeylessGrammarEvaluation(
    val correctedGrammar: List<KeylessGrammarItem>,
    val betterWordChoices: List<KeylessWordChoiceItem>,
    val briefExplanation: String,
    val positivePraise: String,
    val overallScore: Int
)

data class KeylessGrammarItem(
    val original: String,
    val correction: String,
    val category: String,
    val explanation: String
)

data class KeylessWordChoiceItem(
    val original: String,
    val suggested: String,
    val explanation: String
)

data class KeylessRoleplayResponse(
    val reply: String,
    val coachingTip: String,
    val correction: KeylessGrammarItem?
)

data class KeylessVocabWord(
    val word: String,
    val partOfSpeech: String,
    val phonetic: String,
    val definition: String,
    val exampleSentence: String,
    val synonyms: String,
    val antonyms: String
)
