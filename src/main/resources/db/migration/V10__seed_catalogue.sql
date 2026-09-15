-- V10__seed_catalogue.sql
--
-- One-time seed of real catalogue content, shaped to
-- ihya-mobile/docs/seed-data-spec.md: the 10 active categories from that
-- spec's reference table (plus the 2 coming-soon ones, seeded with zero
-- Sunnahs per the spec), and 3 well-attested Sunnahs per active category.
--
-- arabicText is intentionally left blank for every row: the spec requires an
-- Arabic value to be verified against a second source
-- (ihya-mobile/docs/arabic-review.md) before shipping, and no such review has
-- happened yet. tags is left empty -- reserved for seasonal targeting, not
-- read by v1 assignment logic.
--
-- Every `source` citation below is a widely-attested, commonly cited hadith
-- reference; edition numbering can vary slightly across print collections.
-- This is a one-time content seed, not a scholarly hadith database -- treat
-- citations here as good-faith display strings, the same way the spec
-- describes `source` (a "human-readable citation shown on the card back").
--
-- A single statement with two chained CTEs: the first INSERT returns the
-- generated category ids, and the second INSERT joins the Sunnah VALUES list
-- against them by slug, so no id needs to be hand-authored or looked up
-- separately.

WITH ins_categories AS (
    INSERT INTO categories (slug, name, description, status, sort_order)
    VALUES
        ('faith-worship', 'Faith & Worship',
            'Acts of worship and devotion that strengthen your connection with Allah.', 'active', 1),
        ('social-manners', 'Social Manners',
            'The Prophetic etiquette of how Muslims treat one another.', 'active', 2),
        ('home-family', 'Home & Family',
            'Sunnahs that build a home rooted in kindness and mercy.', 'active', 3),
        ('food-eating', 'Food & Eating',
            'The Prophetic manners of eating and drinking.', 'active', 4),
        ('health-cleanliness', 'Health & Cleanliness',
            'Practices of physical and spiritual purity.', 'active', 5),
        ('knowledge-learning', 'Knowledge & Learning',
            'The Prophetic emphasis on seeking and sharing knowledge.', 'active', 6),
        ('work-career', 'Work & Career',
            'Sunnahs of honest work and diligence.', 'active', 7),
        ('travel-journey', 'Travel & Journey',
            'Guidance and remembrance for every journey.', 'active', 8),
        ('nature-creation', 'Nature & Creation',
            'Caring for the earth and all of Allah''s creation.', 'active', 9),
        ('character-good-deeds', 'Character & Good Deeds',
            'Sunnahs of good character, kindness, and charity.', 'active', 10),
        ('self-care', 'Self Care',
            'Caring for your own wellbeing (coming soon).', 'coming-soon', 11),
        ('dua-supplication', 'Du''a & Supplication',
            'Prophetic supplications for every occasion (coming soon).', 'coming-soon', 12)
    RETURNING id, slug
)
INSERT INTO sunnahs (category_id, slug, title, description, reflection, source, prompt, tags)
SELECT c.id, v.slug, v.title, v.description, v.reflection, v.source, v.prompt, ARRAY[]::text[]
FROM (VALUES
    -- faith-worship
    ('faith-worship', 'smile-is-charity', 'Smile at someone today',
        'The Prophet (peace be upon him) taught that smiling at another person is counted as an act of charity.',
        'Greet someone with a genuine smile before noon today.',
        'Jami` at-Tirmidhi 1956', 'Who could use a smile from you today?'),
    ('faith-worship', 'say-bismillah-before-eating', 'Say Bismillah before eating',
        'The Prophet (peace be upon him) taught that mentioning Allah''s name before a meal invites blessing into it.',
        'Say Bismillah out loud before your next meal.',
        'Sunan Abi Dawud 3767', 'How does pausing before a meal change how you eat?'),
    ('faith-worship', 'recite-ayat-al-kursi-after-prayer', 'Recite Ayat al-Kursi after prayer',
        'The Prophet (peace be upon him) taught that reciting Ayat al-Kursi after each obligatory prayer draws a person closer to Paradise.',
        'Recite Ayat al-Kursi after your next prayer.',
        'Sunan an-Nasa''i al-Kubra; authenticated by Ibn Hibban', 'What part of Ayat al-Kursi stands out to you?'),

    -- social-manners
    ('social-manners', 'spread-salaam', 'Spread salaam',
        'The Prophet (peace be upon him) taught that greeting one another with salaam builds the love that faith is built on.',
        'Greet three people with As-salamu alaykum today, even ones you already know well.',
        'Sahih Muslim 54', 'Who rarely gets a salaam from you?'),
    ('social-manners', 'remove-harm-from-the-road', 'Remove harm from the road',
        'The Prophet (peace be upon him) described removing something harmful from a shared path as a branch of faith.',
        'Clear one obstacle, litter, a sharp object, anything, from a shared space today.',
        'Sahih Muslim 35', 'What small hazard have you been walking past?'),
    ('social-manners', 'visit-the-sick', 'Visit someone who is unwell',
        'The Prophet (peace be upon him) taught that visiting the sick is a right one Muslim owes another.',
        'Call or visit someone who is unwell today, even briefly.',
        'Sahih al-Bukhari 5649', 'Who have you been meaning to check on?'),

    -- home-family
    ('home-family', 'greet-family-on-entering-home', 'Greet your family at the door',
        'The Prophet (peace be upon him) taught that saying salaam on entering your own home brings blessing to the household.',
        'Say As-salamu alaykum out loud the next time you walk through your front door.',
        'Sunan Abi Dawud 5195', 'How does your home feel different when you walk in with a greeting?'),
    ('home-family', 'be-good-to-your-spouse', 'Be kind to your spouse',
        'The Prophet (peace be upon him) said that the best among the believers are those who are best to their spouses.',
        'Do one small, thoughtful thing for your spouse or a close family member today.',
        'Jami` at-Tirmidhi 3895', 'What''s one kindness you''ve been putting off?'),
    ('home-family', 'honor-your-parents', 'Honor your parents',
        'The Prophet (peace be upon him) placed honoring one''s parents among the most beloved deeds to Allah.',
        'Call, visit, or do something kind for a parent today.',
        'Sahih al-Bukhari 5971', 'When did you last thank a parent directly?'),

    -- food-eating
    ('food-eating', 'eat-with-your-right-hand', 'Eat with your right hand',
        'The Prophet (peace be upon him) taught his companions to eat and drink with their right hand.',
        'Notice and use your right hand at your next meal.',
        'Sahih Muslim 2020', 'What habits do you eat on autopilot?'),
    ('food-eating', 'dont-criticize-food', 'Never criticize a meal',
        'The Prophet (peace be upon him) never criticized food; if he liked something he ate it, and if not, he simply left it.',
        'Eat your next meal without commenting on what you dislike about it.',
        'Sahih al-Bukhari 5409', 'How often do you comment on food before tasting it?'),
    ('food-eating', 'say-alhamdulillah-after-eating', 'Thank Allah after eating',
        'The Prophet (peace be upon him) taught a short dua of gratitude to say once a meal is finished.',
        'Say Alhamdulillah out loud after your next meal.',
        'Jami` at-Tirmidhi 3458', 'What are you grateful for in your next meal?'),

    -- health-cleanliness
    ('health-cleanliness', 'use-the-miswak', 'Use the miswak',
        'The Prophet (peace be upon him) said that if it were not too difficult for his people, he would have commanded them to use the miswak before every prayer.',
        'Brush your teeth or use a miswak before your next prayer.',
        'Sahih al-Bukhari 887', 'How does a clean mouth change how you feel before praying?'),
    ('health-cleanliness', 'trim-your-nails', 'Trim your nails regularly',
        'The Prophet (peace be upon him) listed trimming the nails among the practices of natural cleanliness (fitrah).',
        'Check your nails today and trim them if they need it.',
        'Sahih al-Bukhari 5891', 'What small grooming habit keeps slipping?'),
    ('health-cleanliness', 'cleanliness-is-half-of-faith', 'Treat cleanliness as worship',
        'The Prophet (peace be upon him) taught that cleanliness (tahara) is half of faith.',
        'Tidy one small space, your desk, your room, before you pray next.',
        'Sahih Muslim 223', 'What''s one corner you''ve been avoiding?'),

    -- knowledge-learning
    ('knowledge-learning', 'seek-knowledge-for-jannah', 'Take one step toward knowledge',
        'The Prophet (peace be upon him) said that whoever takes a path seeking knowledge, Allah makes easy for them a path to Paradise.',
        'Spend ten minutes today learning something about your deen.',
        'Sahih Muslim 2699', 'What''s one question about Islam you''ve never looked up?'),
    ('knowledge-learning', 'learn-and-teach-the-quran', 'Learn or teach a verse of Qur''an',
        'The Prophet (peace be upon him) said the best among you are those who learn the Qur''an and teach it.',
        'Read or review one verse of Qur''an with its meaning today.',
        'Sahih al-Bukhari 5027', 'Which surah do you know best?'),
    ('knowledge-learning', 'seeking-knowledge-is-an-obligation', 'Treat learning as a duty',
        'The Prophet (peace be upon him) described seeking knowledge as something required of every Muslim.',
        'Ask someone a genuine question today instead of guessing.',
        'Sunan Ibn Majah 224', 'What have you been assuming instead of asking about?'),

    -- work-career
    ('work-career', 'eat-from-your-own-labor', 'Take pride in honest work',
        'The Prophet (peace be upon him) said no one eats better food than what they have earned by their own hands.',
        'Notice and appreciate one piece of work you did today with your own effort.',
        'Sahih al-Bukhari 2072', 'What did you build or finish today?'),
    ('work-career', 'perfect-your-work', 'Do your work with excellence',
        'The Prophet (peace be upon him) taught that Allah loves for a person to do their work with excellence (ihsan).',
        'Redo or double-check one task today with extra care.',
        'Shu`ab al-Iman, Al-Bayhaqi', 'Where did you cut a corner recently?'),
    ('work-career', 'be-honest-in-trade', 'Be honest in every transaction',
        'The Prophet (peace be upon him) said the honest, trustworthy merchant will be with the prophets and the truthful on the Day of Judgment.',
        'Be fully transparent in one transaction or agreement today.',
        'Jami` at-Tirmidhi 1209', 'Where might a small shortcut cost your integrity?'),

    -- travel-journey
    ('travel-journey', 'dua-for-travel', 'Recite the traveler''s dua',
        'The Prophet (peace be upon him) taught a dua asking Allah for righteousness and piety on every journey.',
        'Recite the traveler''s dua before your next trip, even a short one.',
        'Sahih Muslim 1342', 'What do you usually forget to do before leaving the house?'),
    ('travel-journey', 'choose-a-travel-companion', 'Choose good travel companions',
        'The Prophet (peace be upon him) advised against traveling alone when a good companion is available.',
        'Invite someone along the next time you take a trip or errand.',
        'Sunan Abi Dawud 2607', 'Who makes your journeys better?'),
    ('travel-journey', 'shorten-prayers-while-traveling', 'Ease your prayers while traveling',
        'The Prophet (peace be upon him) shortened the four-rakah prayers to two while traveling, as a mercy for the traveler.',
        'If you are traveling, remember the ease Islam gives you, and use it.',
        'Sahih al-Bukhari 1090', 'Where do you resist accepting ease that''s allowed?'),

    -- nature-creation
    ('nature-creation', 'plant-something', 'Plant something today',
        'The Prophet (peace be upon him) said that when a Muslim plants a tree and a bird or person eats from it, it counts as charity for them.',
        'Plant a seed, a tree, or even one pot of herbs today.',
        'Sahih al-Bukhari 2320', 'What would you like to grow?'),
    ('nature-creation', 'dont-waste-water', 'Don''t waste water, even in abundance',
        'The Prophet (peace be upon him) taught his companions not to be wasteful with water even while standing at a flowing river.',
        'Notice and cut one moment of water waste today, a running tap, a long shower.',
        'Sunan Ibn Majah 425', 'Where do you waste water without noticing?'),
    ('nature-creation', 'show-mercy-to-animals', 'Show mercy to an animal',
        'The Prophet (peace be upon him) described a person forgiven by Allah simply for giving water to a thirsty dog.',
        'Feed, water, or show kindness to an animal today.',
        'Sahih al-Bukhari 2466', 'What animal crossed your path today?'),

    -- character-good-deeds
    ('character-good-deeds', 'best-in-character', 'Work on your character',
        'The Prophet (peace be upon him) said the best among you are those with the best character.',
        'Notice one moment today to respond with patience instead of irritation.',
        'Sahih al-Bukhari 6035', 'Where does your character get tested most?'),
    ('character-good-deeds', 'love-for-others-what-you-love-for-yourself', 'Wish for others what you wish for you',
        'The Prophet (peace be upon him) said none of you truly believes until you love for your brother what you love for yourself.',
        'Do for someone else today what you would want done for you.',
        'Sahih al-Bukhari 13', 'What do you wish someone would do for you?'),
    ('character-good-deeds', 'give-charity-even-small', 'Give even a small charity',
        'The Prophet (peace be upon him) said to protect yourself from the Fire even with half a date in charity.',
        'Give something today, even something small.',
        'Sahih al-Bukhari 1417', 'What''s the smallest act of generosity you could do right now?')
) AS v(category_slug, slug, title, description, reflection, source, prompt)
JOIN ins_categories c ON c.slug = v.category_slug;
