package com.example.data.local

data class PronunciationItem(
    val id: String,
    val word: String,
    val ipa: String,
    val focusSound: String,
    val category: String,
    val tongueTip: String,
    val exampleSentence: String,
    val difficulty: String = "Intermediate"
)

object PronunciationLabData {
    val items: List<PronunciationItem> = listOf(
        // Tricky Phonemes
        PronunciationItem(
            id = "p_thought",
            word = "Thought",
            ipa = "/θɔːt/",
            focusSound = "/θ/ Unvoiced TH",
            category = "Tricky Phonemes",
            tongueTip = "Place tongue tip gently between front teeth. Push air through without vibrating vocal cords.",
            exampleSentence = "I thought about your suggestion all evening.",
            difficulty = "Beginner"
        ),
        PronunciationItem(
            id = "p_breathe",
            word = "Breathe",
            ipa = "/briːð/",
            focusSound = "/ð/ Voiced TH",
            category = "Tricky Phonemes",
            tongueTip = "Same tongue position as /θ/, but vibrate your vocal cords. Feel the buzz on your tongue.",
            exampleSentence = "Take a deep breath and breathe calmly.",
            difficulty = "Intermediate"
        ),
        PronunciationItem(
            id = "p_world",
            word = "World",
            ipa = "/wɜːld/",
            focusSound = "/ɜːl/ R-colored L",
            category = "Tricky Phonemes",
            tongueTip = "Start with rounded lips for 'w', curl tongue mid-mouth for 'r', then touch roof of mouth for 'l'.",
            exampleSentence = "English connects people around the world.",
            difficulty = "Advanced"
        ),
        PronunciationItem(
            id = "p_rural",
            word = "Rural",
            ipa = "/ˈrʊə.rəl/",
            focusSound = "Double /r/ glide",
            category = "Tricky Phonemes",
            tongueTip = "Keep tongue curled slightly backward without tapping teeth or palate twice.",
            exampleSentence = "They moved from the city to a quiet rural village.",
            difficulty = "Advanced"
        ),

        // Minimal Pairs
        PronunciationItem(
            id = "p_think_sink",
            word = "Think",
            ipa = "/θɪŋk/",
            focusSound = "/θ/ vs /s/",
            category = "Minimal Pairs",
            tongueTip = "Notice teeth: /θ/ tongue between teeth; /s/ tongue stays strictly behind teeth.",
            exampleSentence = "Think carefully before making a final decision.",
            difficulty = "Beginner"
        ),
        PronunciationItem(
            id = "p_vine_wine",
            word = "Vine",
            ipa = "/vaɪn/",
            focusSound = "/v/ vs /w/",
            category = "Minimal Pairs",
            tongueTip = "For /v/, gently rest upper teeth on lower lip. Do NOT round lips like in 'wine'.",
            exampleSentence = "Grape vines need plenty of direct sunlight.",
            difficulty = "Intermediate"
        ),
        PronunciationItem(
            id = "p_right_light",
            word = "Right",
            ipa = "/raɪt/",
            focusSound = "/r/ vs /l/",
            category = "Minimal Pairs",
            tongueTip = "For /r/, tongue floats backward without touching. For /l/, tongue tip firmly presses roof.",
            exampleSentence = "Turn right at the traffic lights.",
            difficulty = "Intermediate"
        ),
        PronunciationItem(
            id = "p_ship_sheep",
            word = "Ship",
            ipa = "/ʃɪp/",
            focusSound = "/ɪ/ Short vs /iː/ Long",
            category = "Minimal Pairs",
            tongueTip = "Relax mouth for short /ɪ/ in ship; smile wide and tense muscles for long /iː/ in sheep.",
            exampleSentence = "The cargo ship docked safely in the harbor.",
            difficulty = "Beginner"
        ),

        // Silent Letters & Syllable Reductions
        PronunciationItem(
            id = "p_comfortable",
            word = "Comfortable",
            ipa = "/ˈkʌmf.tə.bəl/",
            focusSound = "3 Syllables (Silent 'or')",
            category = "Silent Letters",
            tongueTip = "Say 'COMF-tuh-bul'. Do NOT say 'com-for-ta-ble'. The middle syllable is reduced.",
            exampleSentence = "This armchair is remarkably comfortable.",
            difficulty = "Intermediate"
        ),
        PronunciationItem(
            id = "p_wednesday",
            word = "Wednesday",
            ipa = "/ˈwenz.deɪ/",
            focusSound = "Silent D and E",
            category = "Silent Letters",
            tongueTip = "Pronounce as 'WENZ-day'. The first 'd' and 'nes' are completely silent.",
            exampleSentence = "Our team meeting is scheduled for Wednesday.",
            difficulty = "Beginner"
        ),
        PronunciationItem(
            id = "p_receipt",
            word = "Receipt",
            ipa = "/rɪˈsiːt/",
            focusSound = "Completely Silent P",
            category = "Silent Letters",
            tongueTip = "Rhymes with 'defeat'. Never pronounce the 'p'—say 'rih-SEET'.",
            exampleSentence = "Please keep your receipt for the expense report.",
            difficulty = "Beginner"
        ),
        PronunciationItem(
            id = "p_queue",
            word = "Queue",
            ipa = "/kjuː/",
            focusSound = "Pronounced like letter 'Q'",
            category = "Silent Letters",
            tongueTip = "All four letters after Q are silent! It is pronounced exactly like the letter 'Q'.",
            exampleSentence = "There was a long queue at the boarding gate.",
            difficulty = "Intermediate"
        ),
        PronunciationItem(
            id = "p_subtle",
            word = "Subtle",
            ipa = "/ˈsʌt.əl/",
            focusSound = "Silent B",
            category = "Silent Letters",
            tongueTip = "Pronounce as 'SUT-ul'. The 'b' is completely silent.",
            exampleSentence = "There is a subtle difference in their meanings.",
            difficulty = "Intermediate"
        ),
        PronunciationItem(
            id = "p_colonel",
            word = "Colonel",
            ipa = "/ˈkɜː.nəl/",
            focusSound = "Pronounced like 'Kernel'",
            category = "Silent Letters",
            tongueTip = "Historical quirk: Despite the spelling, pronounce it exactly like 'KERNEL'.",
            exampleSentence = "The retired army colonel gave an inspiring speech.",
            difficulty = "Advanced"
        ),

        // Workplace & Professional Vocabulary
        PronunciationItem(
            id = "p_schedule",
            word = "Schedule",
            ipa = "/ˈskedʒ.uːl/",
            focusSound = "'Sked' vs 'Shed'",
            category = "Workplace",
            tongueTip = "US: 'SKED-jool'. UK: 'SHED-yool'. Both are widely accepted in business.",
            exampleSentence = "Let's check our schedule before booking the call.",
            difficulty = "Intermediate"
        ),
        PronunciationItem(
            id = "p_hierarchy",
            word = "Hierarchy",
            ipa = "/ˈhaɪ.ər.ɑː.ki/",
            focusSound = "Hye-er-ar-kee",
            category = "Workplace",
            tongueTip = "Stress the first syllable: 'HYE-rahr-kee' or 'HYE-uh-rahr-kee'.",
            exampleSentence = "The startup maintained a relatively flat company hierarchy.",
            difficulty = "Advanced"
        ),
        PronunciationItem(
            id = "p_entrepreneur",
            word = "Entrepreneur",
            ipa = "/ˌɒn.trə.prəˈnɜːr/",
            focusSound = "On-truh-pruh-NUR",
            category = "Workplace",
            tongueTip = "Stress falls on the final syllable: 'on-truh-pruh-NUR'. French origin glide.",
            exampleSentence = "Every tech entrepreneur faces uncertain challenges early on.",
            difficulty = "Advanced"
        ),
        PronunciationItem(
            id = "p_negotiation",
            word = "Negotiation",
            ipa = "/nɪˌɡoʊ.ʃiˈeɪ.ʃən/",
            focusSound = "-shi-AY-shun rhythm",
            category = "Workplace",
            tongueTip = "Soft 'sh' sound in the middle: 'nuh-go-shee-AY-shun'.",
            exampleSentence = "Both sides entered the negotiation with an open mind.",
            difficulty = "Intermediate"
        )
    )

    val categories: List<String> = listOf(
        "All",
        "Tricky Phonemes",
        "Minimal Pairs",
        "Silent Letters",
        "Workplace"
    )
}
