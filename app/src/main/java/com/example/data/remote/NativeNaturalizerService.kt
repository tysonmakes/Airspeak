package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class NaturalizedOutput(
    val original: String,
    val casual: String,
    val workplace: String,
    val idiomatic: String,
    val coachTip: String
)

class NativeNaturalizerService(
    private val pollinationsService: PollinationsApiService = PollinationsApiService()
) {

    suspend fun naturalize(input: String): NaturalizedOutput = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return@withContext getFallback(trimmed)
        }

        val systemPrompt = """
You are an expert native English fluency coach (ELSA & Praktika style).
Transform the user's sentence into 3 distinct native English levels and return ONLY a valid JSON object without markdown formatting:
{
  "casual": "Casual friendly phrasing used with friends/colleagues",
  "workplace": "Polite, crisp professional phrasing for corporate or formal settings",
  "idiomatic": "Natural expressive phrasing with an idiom or smart phrasal verb",
  "coachTip": "1 concise sentence explaining the grammatical or stylistic improvement"
}
""".trimIndent()

        val userPrompt = "Naturalize this sentence: \"$trimmed\""

        // 1. Try Gemini first if available
        if (GeminiClient.hasValidApiKey()) {
            try {
                val response = GeminiClient.queryGeminiText(
                    prompt = userPrompt,
                    systemInstruction = systemPrompt,
                    temperature = 0.5f,
                    maxTokens = 300
                )
                if (!response.isNullOrBlank()) {
                    val parsed = parseJsonResponse(trimmed, response)
                    if (parsed != null) return@withContext parsed
                }
            } catch (e: Exception) {
                Log.w("Naturalizer", "Gemini query error: ${e.message}")
            }
        }

        // 2. Try Pollinations Keyless
        try {
            val response = pollinationsService.queryKeylessLlm(
                prompt = userPrompt,
                systemPrompt = systemPrompt
            )
            if (!response.isNullOrBlank()) {
                val parsed = parseJsonResponse(trimmed, response)
                if (parsed != null) return@withContext parsed
            }
        } catch (e: Exception) {
            Log.w("Naturalizer", "Pollinations query error: ${e.message}")
        }

        // 3. Smart local heuristic fallback
        return@withContext getFallback(trimmed)
    }

    private fun parseJsonResponse(original: String, rawJson: String): NaturalizedOutput? {
        try {
            val clean = rawJson.substringAfter("{").substringBeforeLast("}")
            val full = "{$clean}"
            val obj = JSONObject(full)
            val casual = obj.optString("casual").trim()
            val workplace = obj.optString("workplace").trim()
            val idiomatic = obj.optString("idiomatic").trim()
            val coachTip = obj.optString("coachTip").trim()

            if (casual.isNotEmpty() && workplace.isNotEmpty()) {
                return NaturalizedOutput(
                    original = original,
                    casual = casual,
                    workplace = workplace,
                    idiomatic = idiomatic.ifEmpty { casual },
                    coachTip = coachTip.ifEmpty { "Sounds smoother and more natural in daily speech." }
                )
            }
        } catch (e: Exception) {
            Log.w("Naturalizer", "JSON parse error: ${e.message}")
        }
        return null
    }

    private fun getFallback(original: String): NaturalizedOutput {
        val lower = original.lowercase()
        return when {
            "agree" in lower -> NaturalizedOutput(
                original = original,
                casual = "Totally on the same page with you!",
                workplace = "I completely align with your perspective on this.",
                idiomatic = "You took the words right out of my mouth.",
                coachTip = "In English, 'agree' is a verb on its own—say 'I agree' rather than 'I am agree'."
            )
            "late" in lower || "traffic" in lower -> NaturalizedOutput(
                original = original,
                casual = "Sorry I'm running behind, got caught in traffic!",
                workplace = "Apologies for the slight delay; traffic was heavier than anticipated.",
                idiomatic = "Got stuck bumper-to-bumper on the way in.",
                coachTip = "'Running behind' is the most natural conversational phrase when delayed."
            )
            "question" in lower || "ask" in lower -> NaturalizedOutput(
                original = original,
                casual = "Quick question for you whenever you have a second!",
                workplace = "May I request a brief clarification regarding this matter?",
                idiomatic = "Can I pick your brain on this for a minute?",
                coachTip = "'Pick your brain' is an extremely common, friendly workplace idiom."
            )
            "understand" in lower || "confus" in lower -> NaturalizedOutput(
                original = original,
                casual = "I didn't quite catch that—could you run it by me again?",
                workplace = "Could you please elaborate on that point for clarity?",
                idiomatic = "I couldn't quite wrap my head around that part.",
                coachTip = "'Catch that' and 'wrap my head around' show high conversational fluency."
            )
            "help" in lower -> NaturalizedOutput(
                original = original,
                casual = "Could you give me a hand with this real quick?",
                workplace = "Would you be available to assist with this deliverable?",
                idiomatic = "Could you help me out in a pinch?",
                coachTip = "'Give me a hand' sounds much more natural and warm than 'do my help'."
            )
            else -> NaturalizedOutput(
                original = original,
                casual = "Here is what I'm thinking: $original",
                workplace = "I would like to propose the following: $original",
                idiomatic = "To put it simply: $original",
                coachTip = "Clear subject-verb pacing makes your sentences flow with effortless confidence."
            )
        }
    }
}
