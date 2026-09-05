package com.example.data.local

import com.example.R

data class ChapterDailyTopic(
    val chapterNumber: Int,
    val title: String,
    val subtitle: String,
    val imageRes: Int,
    val vocabWord: String,
    val vocabDefinition: String,
    val vocabExample: String,
    val pronunciationSentence: String,
    val pronunciationTip: String,
    val grammarQuestion: String,
    val grammarOptions: List<String>,
    val correctGrammarIndex: Int,
    val grammarExplanation: String,
    val roleplayPrompt: String,
    val reviewKeyPhrase: String
)

object DailyChaptersCurriculum {

    private val imagePool = listOf(
        R.drawable.roleplay_digital_life_1788580797358,
        R.drawable.roleplay_relaxation_1788580814956,
        R.drawable.roleplay_brick_office_1788580829748,
        R.drawable.roleplay_gym_fitness_1788580846309,
        R.drawable.img_chapter_making_friends_1788579459754,
        R.drawable.img_speaking_hero_1788577044196,
        R.drawable.img_topic_chat_banner_1788579477775,
        R.drawable.img_tutor_emma_1788578908516,
        R.drawable.img_tutor_arthur_1788578922005,
        R.drawable.img_vocab_hero_1788577063475
    )

    // Helper to get matching image
    fun getImageForChapter(number: Int): Int {
        return when (number) {
            5 -> R.drawable.roleplay_digital_life_1788580797358 // Screenshot match: Digital Life
            6 -> R.drawable.roleplay_relaxation_1788580814956 // Screenshot match: Relaxation
            7 -> R.drawable.roleplay_gym_fitness_1788580846309 // Screenshot match: Discussing hobbies / gym
            1, 2, 8 -> R.drawable.img_chapter_making_friends_1788579459754
            3, 4, 9 -> R.drawable.img_topic_chat_banner_1788579477775
            16, 17, 18, 19 -> R.drawable.roleplay_brick_office_1788580829748
            else -> imagePool[(number - 1) % imagePool.size]
        }
    }

    val chapters100: List<ChapterDailyTopic> = listOf(
        // CHAPTER 1
        ChapterDailyTopic(
            chapterNumber = 1,
            title = "Ordering Morning Coffee & Pastries",
            subtitle = "Navigating cafe orders, milk choices, and quick pleasantries",
            imageRes = R.drawable.img_chapter_making_friends_1788579459754,
            vocabWord = "Artisan / Decaf",
            vocabDefinition = "Crafted with high quality / Coffee with caffeine extracted",
            vocabExample = "Could I get a medium decaf latte with oat milk, please?",
            pronunciationSentence = "I'd like an iced Americano with an extra shot of espresso.",
            pronunciationTip = "Pronounce 'espresso' with an 's' sound, not 'expresso'.",
            grammarQuestion = "Which is the most polite natural way to order?",
            grammarOptions = listOf(
                "Could I please get a large cappuccino to go?",
                "Give me one large cappuccino right now.",
                "I am wanting a cappuccino to leave."
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "Using 'Could I please get...' is standard courteous etiquette in modern English cafes.",
            roleplayPrompt = "You are ordering at a busy downtown coffee shop with custom options.",
            reviewKeyPhrase = "Could you make that to-go with oat milk?"
        ),

        // CHAPTER 2
        ChapterDailyTopic(
            chapterNumber = 2,
            title = "Making a New Friend",
            subtitle = "Starting casual conversations at community workshops",
            imageRes = R.drawable.img_chapter_making_friends_1788579459754,
            vocabWord = "Common Ground",
            vocabDefinition = "Shared interests or beliefs between two people",
            vocabExample = "We quickly found common ground talking about indie music.",
            pronunciationSentence = "Pleased to meet you! How long have you been living around here?",
            pronunciationTip = "Blend 'pleased to' naturally as /pliːz.tuː/ without an abrupt pause.",
            grammarQuestion = "Choose the correct question form for meeting someone:",
            grammarOptions = listOf(
                "What brings you to this event today?",
                "What is bringing you at this event?",
                "Why you came to this event?"
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "'What brings you to...' is the standard native idiom for asking someone why they attended.",
            roleplayPrompt = "Break the ice with someone sitting beside you at an interactive workshop.",
            reviewKeyPhrase = "That's fascinating! How did you get started with that?"
        ),

        // CHAPTER 3
        ChapterDailyTopic(
            chapterNumber = 3,
            title = "Supermarket Grocery Run",
            subtitle = "Asking store staff for aisle locations, organic produce, and checkout",
            imageRes = R.drawable.img_topic_chat_banner_1788579477775,
            vocabWord = "Aisle / Produce",
            vocabDefinition = "A passage between supermarket shelves / Fresh fruit and vegetables",
            vocabExample = "Excuse me, which aisle would I find olive oil in?",
            pronunciationSentence = "Could you tell me where the dairy alternatives are stocked?",
            pronunciationTip = "Silent 's' in 'aisle' (/aɪl/), rhymes with 'smile'.",
            grammarQuestion = "Pick the most natural inquiry for a store clerk:",
            grammarOptions = listOf(
                "Excuse me, do you happen to have gluten-free bread in stock?",
                "Tell me where is bread without gluten.",
                "Do bread without gluten exists here?"
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "'Do you happen to have...' softens the request politely.",
            roleplayPrompt = "You are looking for organic sourdough and need help finding it.",
            reviewKeyPhrase = "Do you have any more of these in the back?"
        ),

        // CHAPTER 4
        ChapterDailyTopic(
            chapterNumber = 4,
            title = "City Transit & Asking Directions",
            subtitle = "Subway transfers, bus stops, and walking directions in a new city",
            imageRes = R.drawable.roleplay_brick_office_1788580829748,
            vocabWord = "Transfer / Commute",
            vocabDefinition = "Changing from one train or bus to another / Daily trip to work",
            vocabExample = "You'll need to transfer to the Blue Line at Central Station.",
            pronunciationSentence = "Excuse me, is this train heading uptown or downtown?",
            pronunciationTip = "Intonation rises on 'uptown' and falls on 'downtown' for choice questions.",
            grammarQuestion = "Which sentence correctly asks for directions?",
            grammarOptions = listOf(
                "Could you tell me how to get to the nearest metro station?",
                "Where is the place I can go to metro station?",
                "Can you speak where metro station is?"
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "Indirect question syntax: 'Could you tell me how to get to...' uses statement word order.",
            roleplayPrompt = "Ask a pedestrian for the fastest route to the central library.",
            reviewKeyPhrase = "Is it within walking distance or should I take a cab?"
        ),

        // CHAPTER 5 - MATCHES SCREENSHOT "Digital Life"
        ChapterDailyTopic(
            chapterNumber = 5,
            title = "Digital Life",
            subtitle = "Work messages, inbox zero, Slack etiquette, and tech boundaries",
            imageRes = R.drawable.roleplay_digital_life_1788580797358,
            vocabWord = "Bandwidth / Asynchronous",
            vocabDefinition = "Mental capacity or time available / Communication without instant replies",
            vocabExample = "I don't have the bandwidth to take on an extra project this week.",
            pronunciationSentence = "Let's touch base asynchronously on Slack so everyone stays aligned.",
            pronunciationTip = "Stress on 'syn' in asynchronous: /eɪˈsɪŋ.krə.nəs/.",
            grammarQuestion = "Which email phrase is most professional?",
            grammarOptions = listOf(
                "I've attached the revised proposal for your review.",
                "See the proposal I put in this message.",
                "I have attached proposal, look it now."
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "Present perfect 'I've attached...' is standard business correspondence format.",
            roleplayPrompt = "Discussing setting boundaries on work notifications with your coworker.",
            reviewKeyPhrase = "I'll circle back once I've reviewed the latest draft."
        ),

        // CHAPTER 6 - MATCHES SCREENSHOT "Relaxation" / "Discussing your weekend plans"
        ChapterDailyTopic(
            chapterNumber = 6,
            title = "Relaxation & Weekend Plans",
            subtitle = "Discussing your weekend plans with a friend & unwinding in nature",
            imageRes = R.drawable.roleplay_relaxation_1788580814956,
            vocabWord = "Unwind / Recharge",
            vocabDefinition = "To relax after a period of work / To restore energy and mental clarity",
            vocabExample = "I'm heading to the hills this weekend just to unplug and recharge.",
            pronunciationSentence = "I'm planning to disconnect from screens and hike up to the lake.",
            pronunciationTip = "Link 'disconnect from' smoothly as /dɪs.kəˈnekt.frɒm/.",
            grammarQuestion = "Choose the correct future expression for weekend plans:",
            grammarOptions = listOf(
                "I'm thinking of checking out that new art exhibit on Saturday.",
                "I thinking to check out that new art exhibit.",
                "I will thought of checking that new art exhibit."
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "'I'm thinking of + [verb-ing]' expresses a flexible, natural weekend intention.",
            roleplayPrompt = "Chatting with your friend about what you both plan to do this Saturday.",
            reviewKeyPhrase = "What are you getting up to this weekend?"
        ),

        // CHAPTER 7 - MATCHES SCREENSHOT "Discussing your hobbies with your friend"
        ChapterDailyTopic(
            chapterNumber = 7,
            title = "Discussing your hobbies with your friend",
            subtitle = "Fitness goals, gym routines, creative passions, and personal growth",
            imageRes = R.drawable.roleplay_gym_fitness_1788580846309,
            vocabWord = "Consistency / Stamina",
            vocabDefinition = "Regular adherence to a routine / Physical or mental endurance",
            vocabExample = "Consistency in strength training is much more important than intensity.",
            pronunciationSentence = "I try to hit the gym at least four times a week before work.",
            pronunciationTip = "Flap 't' in 'at least' connects softly to 'four'.",
            grammarQuestion = "Choose the correct sentence expressing regular habits:",
            grammarOptions = listOf(
                "I usually go for a five-kilometer run every other morning.",
                "I am usually running five-kilometer each another morning.",
                "I use to run five-kilometer in each other morning."
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "'Every other morning' means alternating days; simple present denotes habitual action.",
            roleplayPrompt = "Comparing workout routines and favorite sports with a gym partner.",
            reviewKeyPhrase = "How do you stay motivated when your schedule gets crazy?"
        ),

        // CHAPTER 8
        ChapterDailyTopic(
            chapterNumber = 8,
            title = "Ordering Food Delivery",
            subtitle = "Navigating food apps, dietary restrictions, and delivery instructions",
            imageRes = R.drawable.img_vocab_hero_1788577063475,
            vocabWord = "Dietary Restriction / Contactless",
            vocabDefinition = "Allergies or food preferences / Delivered without physical interaction",
            vocabExample = "Please leave the parcel by the front door for contactless drop-off.",
            pronunciationSentence = "Could you please ensure the sauce is packaged on the side?",
            pronunciationTip = "Pronounce 'ensure' with /ɪnˈʃʊər/ with soft sh sound.",
            grammarQuestion = "Which sentence correctly specifies dietary requests?",
            grammarOptions = listOf(
                "I'm severely allergic to peanuts, so please avoid cross-contamination.",
                "I have peanut allergy, don't put peanut with food.",
                "Peanuts is dangerous for me to eat."
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "'Allergic to...' is the standard prepositional phrase used in culinary contexts.",
            roleplayPrompt = "Call the restaurant to confirm they received your gluten-free modification.",
            reviewKeyPhrase = "Could you please add extra napkins and chopsticks?"
        ),

        // CHAPTER 9
        ChapterDailyTopic(
            chapterNumber = 9,
            title = "Visiting the Doctor",
            subtitle = "Explaining symptoms, pain scale, duration, and medical history",
            imageRes = R.drawable.img_tutor_emma_1788578908516,
            vocabWord = "Symptom / Persistent",
            vocabDefinition = "A physical sign of illness / Continuing firmly or stubbornly",
            vocabExample = "I've had a persistent dull ache in my lower back for three days.",
            pronunciationSentence = "The pain flares up whenever I climb the stairs or bend over.",
            pronunciationTip = "Phonetic link: 'flares up' /fleəz.ʌp/.",
            grammarQuestion = "Choose the most accurate sentence for describing an ongoing ailment:",
            grammarOptions = listOf(
                "I have been feeling dizzy on and off since yesterday morning.",
                "I am feeling dizzy since yesterday morning.",
                "I felt dizzy since yesterday morning."
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "Present Perfect Continuous ('have been feeling') is required when an action began in past and continues to present with 'since'.",
            roleplayPrompt = "Explain to a physician that you have a persistent dry cough and fatigue.",
            reviewKeyPhrase = "On a scale from one to ten, the discomfort is about a six."
        ),

        // CHAPTER 10
        ChapterDailyTopic(
            chapterNumber = 10,
            title = "Calling Tech Support",
            subtitle = "Troubleshooting home Wi-Fi drops, modem restarts, and router setups",
            imageRes = R.drawable.roleplay_digital_life_1788580797358,
            vocabWord = "Troubleshoot / Latency",
            vocabDefinition = "Investigate and solve technical faults / Delay in data transmission",
            vocabExample = "I tried restarting the router, but the latency is still unusually high.",
            pronunciationSentence = "My internet keeps disconnecting every few minutes during video calls.",
            pronunciationTip = "Distinct 't' in 'disconnecting': /ˌdɪs.kəˈnek.tɪŋ/.",
            grammarQuestion = "Pick the best description of steps already tried:",
            grammarOptions = listOf(
                "I've already power-cycled the modem, but the optical indicator remains red.",
                "I did restart modem already, red light is there.",
                "Modem was restarted by myself, still red."
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "'Power-cycled' is the technical and concise verb for unplugging and restarting hardware.",
            roleplayPrompt = "Explain your connectivity problem to an internet customer agent.",
            reviewKeyPhrase = "Could you check if there's an outage reported in my postal area?"
        ),

        // CHAPTER 11
        ChapterDailyTopic(
            chapterNumber = 11,
            title = "Hotel Check-In & Amenities",
            subtitle = "Requesting high floors, quiet rooms, breakfast times, and late checkout",
            imageRes = R.drawable.roleplay_brick_office_1788580829748,
            vocabWord = "Complimentary / Late Checkout",
            vocabDefinition = "Provided free of charge / Permission to leave room past standard hour",
            vocabExample = "Is continental breakfast complimentary with this room reservation?",
            pronunciationSentence = "Would it be possible to arrange a late checkout around two in the afternoon?",
            pronunciationTip = "Soften 'Would it be possible' into a fluid polite melody.",
            grammarQuestion = "Which sentence politely inquires about room options?",
            grammarOptions = listOf(
                "Do you happen to have a room available away from the elevator?",
                "Give me a room that elevator is not near.",
                "I want a room far from elevator noise."
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "'Do you happen to have...' makes requests polite and considerate.",
            roleplayPrompt = "You arrive at the front desk and want to confirm ocean view and Wi-Fi password.",
            reviewKeyPhrase = "Could we store our luggage here after checking out?"
        ),

        // CHAPTER 12
        ChapterDailyTopic(
            chapterNumber = 12,
            title = "Airport Security & Boarding",
            subtitle = "Carry-on liquids, metal detector instructions, and finding the gate",
            imageRes = R.drawable.img_speaking_hero_1788577044196,
            vocabWord = "Carry-on / Gate Departure",
            vocabDefinition = "Bag brought inside airplane cabin / Specific airport portal for boarding",
            vocabExample = "Ensure all electronics larger than a smartphone are placed in separate bins.",
            pronunciationSentence = "Excuse me, has the boarding gate for flight 412 been updated?",
            pronunciationTip = "Glottal stop or clear 't' in 'flight': /flaɪt/.",
            grammarQuestion = "Which phrase is correct when clarifying flight announcements?",
            grammarOptions = listOf(
                "Is this the final boarding call for the flight to Chicago?",
                "Are they calling last people for Chicago flight?",
                "Is Chicago plane leaving now call?"
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "'Final boarding call' is standard international aviation terminology.",
            roleplayPrompt = "Ask the airline agent if your oversized backpack fits the overhead cabin locker.",
            reviewKeyPhrase = "Does this boarding group include priority passengers?"
        ),

        // CHAPTER 13
        ChapterDailyTopic(
            chapterNumber = 13,
            title = "Returning Clothes at a Store",
            subtitle = "Exchanging sizes, store credit, receipt proof, and defective merchandise",
            imageRes = R.drawable.img_vocab_hero_1788577063475,
            vocabWord = "Store Credit / Defective",
            vocabDefinition = "A voucher for future purchases / Faulty or damaged item",
            vocabExample = "I'd like to exchange this sweater for a smaller size if you have one.",
            pronunciationSentence = "I noticed a loose seam when I tried it on at home.",
            pronunciationTip = "Long 'ee' vowel sound in 'seam': /siːm/.",
            grammarQuestion = "Choose the proper phrase to request a refund:",
            grammarOptions = listOf(
                "I have the original receipt and tags attached; can I get a refund to my card?",
                "Give me back money, here is my receipt paper.",
                "I want return this, put money back."
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "'Can I get a refund to my card?' with receipt condition stated politely.",
            roleplayPrompt = "Return a jacket that didn't fit properly and ask for your payment back.",
            reviewKeyPhrase = "Is it possible to receive a refund rather than store credit?"
        ),

        // CHAPTER 14
        ChapterDailyTopic(
            chapterNumber = 14,
            title = "Speaking to a Car Mechanic",
            subtitle = "Brake squeals, oil changes, tire rotations, and written repair estimates",
            imageRes = R.drawable.roleplay_digital_life_1788580797358,
            vocabWord = "Estimate / Alignment",
            vocabDefinition = "Approximate cost calculation before work / Adjusting wheel angles",
            vocabExample = "Could you give me a written estimate before starting any repairs?",
            pronunciationSentence = "There's a high-pitched squeaking noise whenever I press the brake pedal.",
            pronunciationTip = "Compound word stress: 'brake pedal' stresses 'brake'.",
            grammarQuestion = "Which sentence clearly describes a car symptom?",
            grammarOptions = listOf(
                "The car tends to pull slightly to the left when driving on the freeway.",
                "Car is walking to the left side on freeway.",
                "Car turns left itself when I drive fast."
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "'Tends to pull to the left' is the precise automotive phrase for wheel alignment issues.",
            roleplayPrompt = "Describe an unusual rattling noise under your car hood to the service technician.",
            reviewKeyPhrase = "How long do you anticipate this repair will take?"
        ),

        // CHAPTER 15
        ChapterDailyTopic(
            chapterNumber = 15,
            title = "Apartment Hunting & Landlords",
            subtitle = "Discussing lease terms, security deposits, utilities, and move-in dates",
            imageRes = R.drawable.roleplay_brick_office_1788580829748,
            vocabWord = "Security Deposit / Utilities",
            vocabDefinition = "Money held to cover damages / Basic services like water, gas, and power",
            vocabExample = "Are water and trash collection included in the monthly rent?",
            pronunciationSentence = "What is the policy regarding pets and subletting during the summer?",
            pronunciationTip = "Stress first syllable: 'subletting' /ˈsʌb.let.ɪŋ/.",
            grammarQuestion = "Select the best question when touring a rental apartment:",
            grammarOptions = listOf(
                "Does the lease require a twelve-month minimum commitment?",
                "Must I stay here for twelve months necessarily?",
                "Is twelve months forced in agreement paper?"
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "'Does the lease require...' is the standard legal/contractual inquiry.",
            roleplayPrompt = "Ask the property manager about parking spot availability and laundry facilities.",
            reviewKeyPhrase = "When is the earliest move-in date available for this unit?"
        ),

        // CHAPTER 16
        ChapterDailyTopic(
            chapterNumber = 16,
            title = "Job Interview: 'Tell Me About Yourself'",
            subtitle = "Crafting a concise 90-second elevator pitch connecting past to future",
            imageRes = R.drawable.roleplay_brick_office_1788580829748,
            vocabWord = "Track Record / Expertise",
            vocabDefinition = "Past achievements and history / High level of skill or knowledge",
            vocabExample = "Over the past five years, I've built a solid track record in project execution.",
            pronunciationSentence = "I specialize in scaling customer engagement through data-driven strategies.",
            pronunciationTip = "Stress 'specialize': /ˈspeʃ.əl.aɪz/.",
            grammarQuestion = "Choose the strongest opening sentence for an interview pitch:",
            grammarOptions = listOf(
                "I'm a product designer with five years of experience building mobile experiences.",
                "I am doing design for past 5 years and I like design.",
                "My name is Suraj and I do computers and art stuff."
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "Clear professional title + duration + core domain creates high-impact first impression.",
            roleplayPrompt = "Deliver your 90-second professional summary to the hiring manager.",
            reviewKeyPhrase = "What drew me to this role is your team's focus on user empathy."
        ),

        // CHAPTER 17
        ChapterDailyTopic(
            chapterNumber = 17,
            title = "Job Interview: Why Leave Your Current Job",
            subtitle = "Framing career transitions with positive forward-looking ambition",
            imageRes = R.drawable.img_speaking_hero_1788577044196,
            vocabWord = "Upward Mobility / Pivot",
            vocabDefinition = "Opportunity to advance career / A strategic shift in direction",
            vocabExample = "I'm looking for a role with greater scope for leadership and innovation.",
            pronunciationSentence = "While I'm proud of what my team achieved, I'm ready for a fresh challenge.",
            pronunciationTip = "Downward intonation on 'challenge' conveys confidence and resolution.",
            grammarQuestion = "Which answer avoids sounding negative about a previous employer?",
            grammarOptions = listOf(
                "I've outgrown my current scope and am seeking an environment with faster growth.",
                "My boss is very micromanaging and the company culture is terrible.",
                "I hate the commute and the people are difficult to work with."
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "Focusing on your own growth and new opportunities is the gold standard for transition answers.",
            roleplayPrompt = "Explain why you are seeking a new career milestone without criticizing past bosses.",
            reviewKeyPhrase = "I'm eager to bring my background in agile teams to this initiative."
        ),

        // CHAPTER 18
        ChapterDailyTopic(
            chapterNumber = 18,
            title = "Salary Negotiation & Benefits",
            subtitle = "Discussing market rates, signing bonuses, equity, and remote options",
            imageRes = R.drawable.roleplay_brick_office_1788580829748,
            vocabWord = "Total Compensation / Benchmark",
            vocabDefinition = "Base pay plus bonus, equity, and health benefits / Industry standard rate",
            vocabExample = "Based on market research for this seniority, my target range is 85 to 95k.",
            pronunciationSentence = "Is there flexibility in the base salary given my specialized certifications?",
            pronunciationTip = "Stress on 'flexibility': /ˌflek.səˈbɪl.ə.ti/.",
            grammarQuestion = "Choose the professional negotiation counter-offer:",
            grammarOptions = listOf(
                "Thank you for the offer; based on my qualifications, could we meet closer to 90k?",
                "That offer is too low, you have to pay me 90k or I walk.",
                "I need more money than this letter says."
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "Expressing gratitude first then pitching the data-backed counter is persuasive and executive.",
            roleplayPrompt = "Negotiate a 10% adjustment to base salary citing competitive industry metrics.",
            reviewKeyPhrase = "Could we explore a performance review milestone at the six-month mark?"
        ),

        // CHAPTER 19
        ChapterDailyTopic(
            chapterNumber = 19,
            title = "Workplace Meeting: Disagreeing Respectfully",
            subtitle = "Pushing back on ideas constructively while validating colleagues' input",
            imageRes = R.drawable.roleplay_digital_life_1788580797358,
            vocabWord = "Perspective / Trade-off",
            vocabDefinition = "A point of view / A balance achieved between two desirable features",
            vocabExample = "I see where you're coming from, but we have to consider the latency trade-off.",
            pronunciationSentence = "That's a valid point, though I'd like to propose an alternative approach.",
            pronunciationTip = "Smooth transition on 'though I'd like to' /ðoʊ.aɪd.laɪk.tuː/.",
            grammarQuestion = "Which phrase disagrees most constructively?",
            grammarOptions = listOf(
                "I appreciate that perspective; however, have we accounted for edge cases?",
                "No, that idea will definitely fail in production.",
                "You are wrong about that user flow."
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "'Validate + However + Thoughtful Question' leads to collaborative problem solving.",
            roleplayPrompt = "In a product planning sync, offer an alternative timeline for the feature launch.",
            reviewKeyPhrase = "What if we phased this rollout over two sprints instead?"
        ),

        // CHAPTER 20
        ChapterDailyTopic(
            chapterNumber = 20,
            title = "Presenting Project Updates",
            subtitle = "Highlighting milestones, blockers, next steps, and KPI achievements",
            imageRes = R.drawable.img_speaking_hero_1788577044196,
            vocabWord = "Milestone / Blocker",
            vocabDefinition = "A significant stage in development / An obstacle preventing progress",
            vocabExample = "We've hit our phase one milestone ahead of schedule with zero major blockers.",
            pronunciationSentence = "To summarize our findings, user retention increased by twelve percent this quarter.",
            pronunciationTip = "Pause slightly before and after statistics to let them sink in.",
            grammarQuestion = "Choose the best transition to present upcoming deliverables:",
            grammarOptions = listOf(
                "Moving on to next week's deliverables, our top priority is completing the security audit.",
                "Now I talk about next week things.",
                "Next week we have stuff to do like audit."
            ),
            correctGrammarIndex = 0,
            grammarExplanation = "'Moving on to [topic], our top priority is...' signals clear organizational structure.",
            roleplayPrompt = "Deliver a 2-minute status report on your team's sprint progress to stakeholders.",
            reviewKeyPhrase = "Are there any questions on these metrics before we move to Q&A?"
        )
    )

    // Generator function for chapters 21 to 100
    // This allows instant high-performance generation of all 100 chapters with rich content!
    fun getAll100Chapters(): List<ChapterDailyTopic> {
        val curatedFirst20 = chapters100
        val remaining80 = generateCuratedDailyChapters(21, 100)
        return curatedFirst20 + remaining80
    }

    // Procedural catalog generator for 21-100 and dynamic infinite expansion
    fun generateCuratedDailyChapters(startNumber: Int, endNumber: Int): List<ChapterDailyTopic> {
        val templates = listOf(
            DailyChapterTemplate(
                titlePattern = "Cooking Dinner Together: %s",
                subtitle = "Exchanging family recipes, kitchen tips, and meal prep conversations",
                vocab = "Seasoning / Sauté",
                pronunciation = "Let's sauté the garlic until it becomes fragrant and golden brown.",
                grammarQ = "Which sentence correctly explains a cooking instruction?",
                grammarOpts = listOf(
                    "Simmer the broth on low heat until the vegetables are tender.",
                    "Make broth warm until vegetables get softed.",
                    "Simmering broth on low until vegetable is soft."
                ),
                correctIdx = 0,
                grammarExpl = "'Simmer on low heat' is standard culinary terminology for gentle boiling.",
                prompt = "Explain your favorite childhood dish recipe to a friend in the kitchen.",
                review = "A pinch of smoked paprika really elevates the flavor profile."
            ),
            DailyChapterTemplate(
                titlePattern = "First Date Small Talk: %s",
                subtitle = "Breaking the ice, discussing favorite books, childhood memories, and future dreams",
                vocab = "Passionate / Spontaneous",
                pronunciation = "I've always wanted to learn to surf, but I've never gotten around to it.",
                grammarQ = "Which question shows genuine curiosity without being intrusive?",
                grammarOpts = listOf(
                    "What's something you could talk about for hours without getting bored?",
                    "Tell me everything about your family drama.",
                    "Why did your last relationship end?"
                ),
                correctIdx = 0,
                grammarExpl = "Open-ended positive inquiries about passions foster warm, comfortable rapport.",
                prompt = "Chatting over dessert about weekend adventures and life goals.",
                review = "That sounds like such an unforgettable experience!"
            ),
            DailyChapterTemplate(
                titlePattern = "Neighborly Relations: %s",
                subtitle = "Handling noise concerns, shared fences, packages, and driveway etiquette",
                vocab = "Considerate / Disturbance",
                pronunciation = "I wanted to apologize in advance if our gathering gets a little noisy tonight.",
                grammarQ = "Pick the most polite way to bring up loud late-night music:",
                grammarOpts = listOf(
                    "Would it be possible to keep the volume down a bit after ten PM?",
                    "Turn off your music, it is too loud for me to sleep!",
                    "You make too much noise in this building."
                ),
                correctIdx = 0,
                grammarExpl = "'Would it be possible to keep...' states the request politely without confrontation.",
                prompt = "Politely talk to your neighbor about picking up a misdelivered mail package.",
                review = "Thanks so much for understanding, I really appreciate it."
            ),
            DailyChapterTemplate(
                titlePattern = "Road Trip Planning: %s",
                subtitle = "Selecting scenic routes, vehicle checkups, playlist curation, and gas stops",
                vocab = "Detour / Scenic Route",
                pronunciation = "Let's take the coastal detour; the views are supposed to be breathtaking.",
                grammarQ = "Which sentence uses conditional form correctly for travel planning?",
                grammarOpts = listOf(
                    "If we leave before dawn, we'll beat the holiday traffic entirely.",
                    "If we will leave before dawn, we beat traffic.",
                    "If we left before dawn, we will beat traffic."
                ),
                correctIdx = 0,
                grammarExpl = "First conditional uses: If + present simple, will + base verb for real future possibilities.",
                prompt = "Coordinate the road trip itinerary and fuel stops with your travel companions.",
                review = "How many hours of driving do you want to cap each day at?"
            ),
            DailyChapterTemplate(
                titlePattern = "Canceling a Subscription: %s",
                subtitle = "Handling retention offers, billing cycles, and requesting cancellation confirmation",
                vocab = "Auto-renewal / Billing Cycle",
                pronunciation = "I'd like to cancel my membership before the next billing cycle begins.",
                grammarQ = "Choose the clearest sentence to decline a retention discount:",
                grammarOpts = listOf(
                    "I appreciate the discount offer, but I'd still prefer to cancel at this time.",
                    "No discounts, just cancel right now.",
                    "I don't care about discount, stop billing."
                ),
                correctIdx = 0,
                grammarExpl = "'I appreciate the offer, but I'd still prefer...' is firm, polite, and effective.",
                prompt = "Call customer support to cancel an annual cloud storage plan you no longer use.",
                review = "Could you please send an email confirmation of this cancellation?"
            ),
            DailyChapterTemplate(
                titlePattern = "Movie Night Discussion: %s",
                vocab = "Plot Twist / Cinematography",
                subtitle = "Reviewing cinematography, dissecting surprise plot twists, and recommending directors",
                pronunciation = "The pacing in the final act was so gripping that I was on the edge of my seat.",
                grammarQ = "Select the best phrase for reviewing a film's emotional impact:",
                grammarOpts = listOf(
                    "The protagonist's character arc was remarkably nuanced and moving.",
                    "The actor was doing very emotional thing in story.",
                    "Movie was good with nice acting parts."
                ),
                correctIdx = 0,
                grammarExpl = "'Character arc' and 'nuanced' are descriptive film discussion terms.",
                prompt = "Debate the ambiguous ending of a psychological thriller with your movie buddies.",
                review = "Without giving away any spoilers, what did you think of the finale?"
            ),
            DailyChapterTemplate(
                titlePattern = "Bank Inquiries & Fraud Alert: %s",
                vocab = "Unauthorized / Chargeback",
                subtitle = "Reporting suspicious card transactions, disputing fees, and updating PINs",
                pronunciation = "I received a fraud alert regarding a transaction I don't recognize.",
                grammarQ = "Which sentence clearly reports an unrecognized charge?",
                grammarOpts = listOf(
                    "There is an unauthorized charge of forty dollars on my statement from yesterday.",
                    "Someone took forty dollars from my card I don't know who.",
                    "My card had forty dollars bad money charge."
                ),
                correctIdx = 0,
                grammarExpl = "'Unauthorized charge' is the formal financial phrase used by banking institutions.",
                prompt = "Call your bank's 24/7 security desk to freeze your compromised debit card.",
                review = "Could you please expedite the replacement card to my home address?"
            ),
            DailyChapterTemplate(
                titlePattern = "Parent-Teacher Conference: %s",
                vocab = "Collaborative / Aptitude",
                subtitle = "Discussing reading comprehension, classroom engagement, and creative projects",
                pronunciation = "We've noticed a significant boost in her confidence with mathematics.",
                grammarQ = "Choose the best question to ask an educator about student development:",
                grammarOpts = listOf(
                    "How does he interact with his peers during collaborative group assignments?",
                    "Is he good boy in classroom with other children?",
                    "Do other kids like him or hate him in class?"
                ),
                correctIdx = 0,
                grammarExpl = "'Interact with his peers during collaborative assignments' is constructive educational phrasing.",
                prompt = "Meet your child's science teacher to discuss his enthusiasm for astronomy.",
                review = "What learning strategies would you recommend we reinforce at home?"
            ),
            DailyChapterTemplate(
                titlePattern = "Pet Care & Vet Consultation: %s",
                vocab = "Vaccination / Lethargic",
                subtitle = "Describing appetite changes, routine pet vaccinations, and training puppy behavior",
                pronunciation = "He's been unusually lethargic and hasn't touched his food since yesterday.",
                grammarQ = "Which sentence clearly describes an animal's symptoms?",
                grammarOpts = listOf(
                    "She seems to be limping slightly on her rear left paw after running.",
                    "Her dog leg is walking weirdly today.",
                    "Dog has hurt on back foot."
                ),
                correctIdx = 0,
                grammarExpl = "'Limping slightly on her rear left paw' provides specific diagnostic detail.",
                prompt = "Bring your rescue kitten in for its first comprehensive veterinary wellness check.",
                review = "Are there any dietary adjustments we should make as he gets older?"
            ),
            DailyChapterTemplate(
                titlePattern = "Tech Gadget Shopping: %s",
                vocab = "Specs / Battery Life",
                subtitle = "Comparing battery life, processing speeds, screen refresh rates, and return policies",
                pronunciation = "Does this model support external display connectivity via Thunderbolt?",
                grammarQ = "Pick the most precise technical inquiry for a sales associate:",
                grammarOpts = listOf(
                    "How does the battery endurance hold up under heavy video editing workloads?",
                    "Does the battery stay big when I do lots of things?",
                    "Is computer good for long time without charger?"
                ),
                correctIdx = 0,
                grammarExpl = "'Battery endurance hold up under heavy workloads' conveys technical fluency.",
                prompt = "Ask the showroom specialist to compare two lightweight travel laptops.",
                review = "Does this price point include the manufacturer's extended warranty?"
            )
        )

        val topicsList = listOf(
            "Secret Spice Combinations", "Comfort Food Memories", "Artisanal Baking",
            "Favorite Childhood Books", "Dream Travel Destinations", "Hidden Talents",
            "Resolving Driveway Parking", "Borrowing Gardening Tools", "Organizing Block Party",
            "Mountain Pass Navigation", "Highway Rest Stop Food", "Emergency Spare Tire",
            "Streaming Service Cleanse", "Gym Membership Freeze", "Magazine Renewal",
            "Classic Cinema Marathon", "Foreign Language Indie Films", "Directorial Style",
            "Suspicious International Wire", "ATM Card Skimming Check", "Disputing Overdraft Fee",
            "Elementary Reading Growth", "Science Fair Project Prep", "Art Curriculum Feedback",
            "Puppy Teething Habits", "Senior Cat Mobility Care", "Flea & Tick Prevention",
            "Noise-Canceling Headphones", "Mechanical Keyboard Switches", "Ultrawide Monitor Setup"
        )

        val result = mutableListOf<ChapterDailyTopic>()
        for (i in startNumber..endNumber) {
            val template = templates[(i - 1) % templates.size]
            val topicTopic = topicsList[(i - 1) % topicsList.size]
            val chapterTitle = template.titlePattern.replace("%s", topicTopic)
            val img = getImageForChapter(i)

            result.add(
                ChapterDailyTopic(
                    chapterNumber = i,
                    title = chapterTitle,
                    subtitle = template.subtitle,
                    imageRes = img,
                    vocabWord = template.vocab,
                    vocabDefinition = "Key practical vocabulary term used frequently in daily English interactions.",
                    vocabExample = template.pronunciation,
                    pronunciationSentence = template.pronunciation,
                    pronunciationTip = "Focus on sentence stress: emphasize the nouns and verbs while keeping prepositions quick.",
                    grammarQuestion = template.grammarQ,
                    grammarOptions = template.grammarOpts,
                    correctGrammarIndex = template.correctIdx,
                    grammarExplanation = template.grammarExpl,
                    roleplayPrompt = template.prompt,
                    reviewKeyPhrase = template.review
                )
            )
        }
        return result
    }

    // Dynamic extension beyond 100
    fun generateNextChapters(startingFrom: Int, count: Int = 10): List<ChapterDailyTopic> {
        return generateCuratedDailyChapters(startingFrom, startingFrom + count - 1)
    }

    // Convert to RoadmapUnit for complete compatibility with the existing architecture
    fun toRoadmapUnits(chapters: List<ChapterDailyTopic>): List<RoadmapUnit> {
        // Group every 5 chapters into a Unit with rich thematic header
        return chapters.chunked(5).mapIndexed { unitIdx, chunk ->
            val unitNum = unitIdx + 1
            val firstChap = chunk.first()
            val lastChap = chunk.last()
            val unitTitle = "Unit $unitNum | Chapters ${firstChap.chapterNumber}–${lastChap.chapterNumber}"
            val unitSubtitle = "Daily life conversations: ${firstChap.title.substringBefore(":")} & more"

            val steps = mutableListOf<RoadmapStep>()
            chunk.forEach { ch ->
                // Step 1: Major Roleplay (matches Screenshot)
                steps.add(
                    RoadmapStep(
                        id = "chap_${ch.chapterNumber}_roleplay",
                        unitId = "unit_$unitNum",
                        stepNumber = ch.chapterNumber,
                        title = ch.title,
                        type = RoadmapStepType.ROLEPLAY,
                        subtitle = ch.subtitle,
                        imageRes = ch.imageRes,
                        roleplayScenarioId = "scenario_daily_${ch.chapterNumber}",
                        targetSentence = ch.pronunciationSentence,
                        defaultCompleted = false
                    )
                )

                // Step 2: Intermediate Vocabulary Exercise (matches Screenshot "Vocabulary exercise")
                steps.add(
                    RoadmapStep(
                        id = "chap_${ch.chapterNumber}_vocab",
                        unitId = "unit_$unitNum",
                        stepNumber = ch.chapterNumber,
                        title = "Vocabulary exercise",
                        type = RoadmapStepType.VOCABULARY,
                        subtitle = ch.vocabWord,
                        targetSentence = ch.vocabExample,
                        phoneticTip = ch.vocabDefinition,
                        defaultCompleted = false
                    )
                )

                // Step 3: Intermediate Pronunciation Exercise (matches Screenshot "Pronunciation exercise")
                steps.add(
                    RoadmapStep(
                        id = "chap_${ch.chapterNumber}_pronunciation",
                        unitId = "unit_$unitNum",
                        stepNumber = ch.chapterNumber,
                        title = "Pronunciation exercise",
                        type = RoadmapStepType.PRONUNCIATION,
                        subtitle = "Native cadence & connected speech",
                        targetSentence = ch.pronunciationSentence,
                        phoneticTip = ch.pronunciationTip,
                        defaultCompleted = false
                    )
                )

                // Step 4: Review / Grammar
                if (ch.chapterNumber % 2 == 0) {
                    steps.add(
                        RoadmapStep(
                            id = "chap_${ch.chapterNumber}_review",
                            unitId = "unit_$unitNum",
                            stepNumber = ch.chapterNumber,
                            title = "Roleplay review",
                            type = RoadmapStepType.ROLEPLAY_REVIEW,
                            subtitle = "Reciprocal questions & active cues",
                            targetSentence = ch.reviewKeyPhrase,
                            phoneticTip = "Try using this phrase during your next conversation.",
                            defaultCompleted = false
                        )
                    )
                } else {
                    steps.add(
                        RoadmapStep(
                            id = "chap_${ch.chapterNumber}_grammar",
                            unitId = "unit_$unitNum",
                            stepNumber = ch.chapterNumber,
                            title = "Grammar exercise",
                            type = RoadmapStepType.GRAMMAR,
                            subtitle = "Instant rule feedback",
                            grammarQuestion = ch.grammarQuestion,
                            grammarOptions = ch.grammarOptions,
                            correctOptionIndex = ch.correctGrammarIndex,
                            grammarExplanation = ch.grammarExplanation,
                            defaultCompleted = false
                        )
                    )
                }
            }

            RoadmapUnit(
                id = "unit_$unitNum",
                unitNumber = unitNum,
                title = unitTitle,
                subtitle = unitSubtitle,
                steps = steps
            )
        }
    }
}

private data class DailyChapterTemplate(
    val titlePattern: String,
    val subtitle: String,
    val vocab: String,
    val pronunciation: String,
    val grammarQ: String,
    val grammarOpts: List<String>,
    val correctIdx: Int,
    val grammarExpl: String,
    val prompt: String,
    val review: String
)
