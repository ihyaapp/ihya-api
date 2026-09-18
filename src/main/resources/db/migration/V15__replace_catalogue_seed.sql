-- V15__replace_catalogue_seed.sql
--
-- Supersedes V10's one-time 30-Sunnah seed with a corrected/expanded 42-Sunnah
-- catalog. V10 is never edited (Flyway migrations are forward-only, per
-- CLAUDE.md) -- instead this migration deletes whatever V10 inserted and
-- replaces it, so that a brand-new database (docker compose down -v +
-- recreate) always converges on this catalog rather than V10's, even though
-- V10 still runs first in the migration sequence every time.
--
-- Deliberately touches only the sunnahs table, nothing else. On a genuine
-- from-scratch bootstrap this migration runs immediately after V10 in the
-- same startup, before any user exists, so practices/daily_assignments are
-- still empty and DELETE FROM sunnahs never conflicts with their foreign
-- keys (sunnah_id REFERENCES sunnahs(id), no ON DELETE CASCADE). This
-- migration does not clear practices/daily_assignments or touch
-- user_progress -- if this ever runs against a database that already has
-- real practice history pointing at the old 30 Sunnahs, that DELETE will
-- fail on the foreign key rather than silently destroying that history;
-- clear those tables by hand first in that situation, deliberately, rather
-- than have a migration do it silently.
--
-- categories is untouched: no new categories are introduced here, every
-- categorySlug below names one of the 10 active slugs V10 already created,
-- plus self-care (currently coming-soon -- these 3 rows are inert until
-- that category is flipped active).
--
-- arabicText IS populated here, sourced verbatim from sunnah.com by a
-- research pass, one source. This is a deliberate, lower bar than
-- ihya-mobile/docs/arabic-review.md's own two-source verification process
-- (sunnah.com + an independent character-by-character diff) -- accepted
-- explicitly by the project owner for this seed rather than left blank
-- pending a second-source check.
--
-- Several rows below intentionally cite the same hadith as a differently-
-- slugged row (e.g. smile-at-others / seek-forgiveness-often overlapping
-- prior themes) -- kept per explicit product decision, not an oversight.

DELETE FROM sunnahs;

INSERT INTO sunnahs (category_id, slug, title, description, reflection, source, prompt, tags, arabic_text)
SELECT c.id, v.slug, v.title, v.description, v.reflection, v.source, v.prompt, ARRAY[]::text[], v.arabic_text
FROM (VALUES
    -- social-manners
    ('social-manners', 'say-salam-first', 'Be the first to say salam',
        'The Prophet (peace be upon him) taught that the people nearest to Allah are those who are first to give the greeting.',
        'Say salam first to someone today before they say it to you.',
        'Sunan Abi Dawud 5197', 'Who do you usually wait on to greet you first?',
        'إِنَّ أَوْلَى النَّاسِ بِاللَّهِ مَنْ بَدَأَهُمْ بِالسَّلاَمِ'),
    ('social-manners', 'give-a-gift-today', 'Give a small gift today',
        'The Prophet (peace be upon him) taught that giving gifts to one another builds love between people.',
        'Give someone a small gift today, even a snack or a kind note.',
        'Al-Adab Al-Mufrad 594', 'Who would be surprised to get something from you today?',
        'تَهَادُوا تَحَابُّوا'),

    -- character-good-deeds
    ('character-good-deeds', 'avoid-lying-in-jokes', 'Avoid lying even in jokes',
        'The Prophet (peace be upon him) warned that a lie told only to make people laugh is still a lie.',
        'Today, if you make someone laugh, keep the story fully true.',
        'Jami` at-Tirmidhi 2315', 'Have you ever stretched the truth just for a laugh?',
        'وَيْلٌ لِلَّذِي يُحَدِّثُ بِالْحَدِيثِ لِيُضْحِكَ بِهِ الْقَوْمَ فَيَكْذِبُ'),
    ('character-good-deeds', 'be-gentle-in-speech', 'Be gentle and kind in speech',
        'The Prophet (peace be upon him) taught that gentleness never enters anything without making it better.',
        'Pick one conversation today, even a hard one, and keep your tone gentle through the whole thing.',
        'Sahih Muslim 2594', 'Which conversation today most needs a softer tone than usual?',
        'إِنَّ الرِّفْقَ لَا يَكُونُ فِي شَيْءٍ إِلَّا زَانَهُ'),

    -- faith-worship
    ('faith-worship', 'pray-istikhara-before-deciding', 'Pray istikhara before deciding',
        'The Prophet (peace be upon him) taught his companions a short prayer for guidance before any decision, big or small.',
        'If you have any decision pending, pray istikhara on it today. If not, learn the dua today so you are ready.',
        'Sahih al-Bukhari 1166', 'What decision have you been putting off making?',
        'كَانَ رَسُولُ اللَّهِ صلى الله عليه وسلم يُعَلِّمُنَا الاِسْتِخَارَةَ فِي الأُمُورِ'),
    ('faith-worship', 'make-dua-for-the-deceased', 'Make dua for the deceased',
        'The Prophet (peace be upon him) taught a short dua asking Allah''s mercy on a Muslim who has passed away.',
        'Say this dua today for any deceased Muslim you know of, even one you never met.',
        'Sahih Muslim 963', 'Who do you know who has passed that you rarely make dua for?',
        'اللَّهُمَّ اغْفِرْ لَهُ وَارْحَمْهُ وَعَافِهِ وَاعْفُ عَنْهُ'),
    ('faith-worship', 'seek-forgiveness-often', 'Ask forgiveness often today',
        'The Prophet (peace be upon him) turned to Allah in repentance more than seventy times a day, even though he was already forgiven.',
        'Say Astaghfirullah out loud at least ten times today, more if you can.',
        'Sahih al-Bukhari 6307', 'What''s on your mind that you haven''t asked forgiveness for yet?',
        'وَاللَّهِ إِنِّي لأَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ فِي الْيَوْمِ أَكْثَرَ مِنْ سَبْعِينَ مَرَّةً'),
    ('faith-worship', 'pray-two-rakahs-after-wudu', 'Pray two rak''ahs after wudu',
        'The Prophet (peace be upon him) told Bilal his best deed was praying two rak''ahs after every wudu, a habit that earned him footsteps in Paradise.',
        'Next time you make wudu today, stay on the prayer mat for two extra rak''ahs. That''s it.',
        'Sahih al-Bukhari 1149', 'What''s the smallest habit you could repeat every single day?',
        'يَا بِلاَلُ حَدِّثْنِي بِأَرْجَى عَمَلٍ عَمِلْتَهُ فِي الإِسْلاَمِ'),
    ('faith-worship', 'make-witr-your-closing-prayer', 'Make witr your closing prayer',
        'The Prophet (peace be upon him) taught his companions to make witr the very last prayer of the night.',
        'Tonight, before you sleep, make witr your very last prayer. Even one rak''ah counts.',
        'Sahih Muslim 751b', 'What''s usually the last thing you do before falling asleep?',
        'اجْعَلُوا آخِرَ صَلاَتِكُمْ بِاللَّيْلِ وِتْرًا'),
    ('faith-worship', 'pray-in-congregation', 'Pray with others today',
        'The Prophet (peace be upon him) taught that praying in congregation is twenty seven times more rewarding than praying alone.',
        'Pray your next prayer with someone else today, even if it''s just one other person.',
        'Sahih al-Bukhari 645', 'Who could you ask to pray alongside you today?',
        'صَلاَةُ الْجَمَاعَةِ تَفْضُلُ صَلاَةَ الْفَذِّ بِسَبْعٍ وَعِشْرِينَ دَرَجَةً'),
    ('faith-worship', 'alhamdulillah-on-waking', 'Your first words of the day',
        'Before doing anything else each morning, the Prophet (peace be upon him) began the day by thanking Allah for waking him up.',
        'Tomorrow, say Alhamdulillahi alladhi ahyana ba''da ma amatana wa ilayhin nushur before you even sit up in bed.',
        'Sahih al-Bukhari 6312', 'What''s the very first thing you usually reach for instead, your phone?',
        'الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ'),
    ('faith-worship', 'dua-before-leaving-home', 'Say this before you walk out the door',
        'The Prophet (peace be upon him) taught a short dua for leaving home that comes with a promise of guidance and protection for the whole outing.',
        'Say Bismillahi tawakkaltu ala Allah, la hawla wa la quwwata illa billah right before you next step outside.',
        'Sunan Abi Dawud 5095', 'See if you notice anything different about how the rest of your outing goes.',
        'بِسْمِ اللَّهِ تَوَكَّلْتُ عَلَى اللَّهِ لاَ حَوْلَ وَلاَ قُوَّةَ إِلاَّ بِاللَّهِ'),
    ('faith-worship', 'tasbih-hundred-times', 'Erase sins like sea foam',
        'The Prophet (peace be upon him) said that whoever says Subhan Allah wa bihamdihi one hundred times a day will have their sins forgiven, even if they were as much as the foam of the sea.',
        'Say SubhanAllahi wa bihamdihi 100 times today. Count it on your fingers if you need to.',
        'Sahih al-Bukhari 6405', 'What could you say on repeat that would actually mean something?',
        'مَنْ قَالَ سُبْحَانَ اللَّهِ وَبِحَمْدِهِ فِي يَوْمٍ مِائَةَ مَرَّةٍ حُطَّتْ خَطَايَاهُ'),

    -- food-eating
    ('food-eating', 'say-bismillah-and-eat-near-you', 'Say bismillah before you eat',
        'The Prophet (peace be upon him) taught a boy three simple table manners: say Allah''s name, eat with your right hand, and eat from what is nearest to you.',
        'At your very next meal, say bismillah out loud and eat from what''s closest to you.',
        'Sahih al-Bukhari 5376', 'What table habit did someone once have to correct in you?',
        'يَا غُلاَمُ سَمِّ اللَّهَ، وَكُلْ بِيَمِينِكَ وَكُلْ مِمَّا يَلِيكَ'),
    ('food-eating', 'sip-slowly-in-three-breaths', 'Sip slowly, in three breaths',
        'The Prophet (peace be upon him) used to pause and breathe outside the cup, drinking in three separate sips rather than one gulp.',
        'Next time you drink water today, pull the cup away and take three separate sips instead of one gulp.',
        'Sahih Muslim 2028a', 'What changes when you actually slow down to drink?',
        'أَنَّ رَسُولَ اللَّهِ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ كَانَ يَتَنَفَّسُ فِي الْإِنَاءِ ثَلَاثًا'),

    -- health-cleanliness
    ('health-cleanliness', 'wudu-and-right-side-before-sleep', 'Make wudu before you sleep',
        'The Prophet (peace be upon him) taught his companions to make wudu before bed, lie on their right side, then say a short dua of surrender.',
        'Tonight, make wudu before bed and lie down on your right side. That''s the whole habit.',
        'Sahih al-Bukhari 247', 'How would starting the night this clean change how you sleep?',
        'إِذَا أَتَيْتَ مَضْجَعَكَ فَتَوَضَّأْ وُضُوءَكَ لِلصَّلاَةِ، ثُمَّ اضْطَجِعْ عَلَى شِقِّكَ الأَيْمَنِ'),
    ('health-cleanliness', 'recite-three-quls-before-sleep', 'Recite the three Quls before sleep',
        'Before sleeping, the Prophet (peace be upon him) used to cup his hands, recite three short protective surahs into them, then wipe them over his body.',
        'Tonight before you close your eyes, recite the three Quls into your hands and wipe them over your body.',
        'Sahih al-Bukhari 5017', 'What''s your current bedtime routine missing?',
        'كَانَ إِذَا أَوَى إِلَى فِرَاشِهِ كُلَّ لَيْلَةٍ جَمَعَ كَفَّيْهِ ثُمَّ نَفَثَ فِيهِمَا'),
    ('health-cleanliness', 'ayat-al-kursi-before-sleep', 'Recite Ayat al-Kursi before sleep',
        'The Prophet (peace be upon him) confirmed that reciting Ayat al-Kursi before sleep brings a guardian from Allah for the whole night.',
        'Recite Ayat al-Kursi tonight right before you fall asleep.',
        'Sahih al-Bukhari 3275', 'What usually crosses your mind right before you fall asleep?',
        'إِذَا أَوَيْتَ إِلَى فِرَاشِكَ فَاقْرَأْ آيَةَ الْكُرْسِيِّ'),
    ('health-cleanliness', 'ghusl-on-friday', 'Take a full ghusl before Jumu''ah',
        'The Prophet (peace be upon him) taught that a full bath before Friday prayer is a distinct act of purification for that day.',
        'This Friday, take a full ghusl before you head to the mosque.',
        'Sahih al-Bukhari 879', 'How does Friday feel different when you start it clean?',
        'غُسْلُ يَوْمِ الْجُمُعَةِ وَاجِبٌ عَلَى كُلِّ مُحْتَلِمٍ'),
    ('health-cleanliness', 'allah-loves-beauty', 'Allah is beautiful and loves beauty',
        'When a man asked if wanting fine clothes was pride, the Prophet (peace be upon him) said Allah is beautiful and loves beauty, and pride is something else entirely.',
        'Wear your best outfit today, on purpose, as a small act of gratitude.',
        'Sahih Muslim 91a', 'Do you ever feel guilty for taking care of how you look?',
        'إِنَّ اللَّهَ جَمِيلٌ يُحِبُّ الْجَمَالَ'),
    ('health-cleanliness', 'start-everything-from-the-right', 'Start everything from the right',
        'The Prophet (peace be upon him) liked to start from the right side when putting on shoes, combing his hair, and washing himself.',
        'Today, put your right shoe on first, every single time you put on shoes.',
        'Sahih al-Bukhari 168', 'What small habit could you turn into a lifelong one?',
        'كَانَ النَّبِيُّ صلى الله عليه وسلم يُعْجِبُهُ التَّيَمُّنُ فِي تَنَعُّلِهِ وَتَرَجُّلِهِ وَطُهُورِهِ'),

    -- character-good-deeds
    ('character-good-deeds', 'give-in-secret', 'Give where only Allah can see',
        'The Prophet (peace be upon him) described seven people who will be shaded by Allah on a day with no other shade, among them someone who gave charity so quietly their own left hand didn''t know what their right hand gave.',
        'Give something today and tell absolutely no one, not even the person closest to you.',
        'Sahih al-Bukhari 1423', 'How does it feel differently when no one will ever know you did it?',
        'وَرَجُلٌ تَصَدَّقَ أَخْفَى حَتَّى لاَ تَعْلَمَ شِمَالُهُ مَا تُنْفِقُ يَمِينُهُ'),
    ('character-good-deeds', 'charity-that-outlives-you', 'Charity that keeps paying you back',
        'The Prophet (peace be upon him) said a person''s good deeds stop when they die, except for three, and ongoing charity is one of them.',
        'Set up one small recurring act of charity today, even something tiny you commit to weekly.',
        'Sahih Muslim 1631', 'What could you start once that keeps giving long after you stop thinking about it?',
        'إِذَا مَاتَ الإِنْسَانُ انْقَطَعَ عَنْهُ عَمَلُهُ إِلاَّ مِنْ ثَلاَثَةٍ إِلاَّ مِنْ صَدَقَةٍ جَارِيَةٍ'),
    ('character-good-deeds', 'feed-people-and-greet-everyone', 'Feed people and greet everyone',
        'When asked what the best quality of Islam was, the Prophet (peace be upon him) said to feed the poor and greet everyone you meet, known or not.',
        'Feed someone today, even a coworker or neighbor, and greet a stranger with salam.',
        'Sahih al-Bukhari 12', 'Who is someone you''ve never greeted, even though you see them often?',
        'تُطْعِمُ الطَّعَامَ، وَتَقْرَأُ السَّلاَمَ عَلَى مَنْ عَرَفْتَ وَمَنْ لَمْ تَعْرِفْ'),
    ('character-good-deeds', 'clearing-a-hazard-is-charity', 'Even clearing a rock is charity',
        'The Prophet (peace be upon him) said that removing something harmful from a shared path, a rock, a thorn, anything, counts as an act of charity.',
        'Move one hazard out of someone''s way today, a rock, litter, anything blocking a path.',
        'Jami` at-Tirmidhi 1956', 'What small hazard have you been walking past without noticing?',
        'وَإِمَاطَتُكَ الْحَجَرَ وَالشَّوْكَةَ وَالْعَظْمَ عَنِ الطَّرِيقِ لَكَ صَدَقَةٌ'),

    -- home-family
    ('home-family', 'raise-a-child-with-attention', 'Raise a child with real attention',
        'The Prophet (peace be upon him) said that whoever raises children well will be brought close to him on the Day of Resurrection.',
        'Spend ten minutes of real, undistracted attention with a child in your life today.',
        'Sahih Muslim 2631', 'When did you last put your phone down completely for a child?',
        'مَنْ عَالَ جَارِيَتَيْنِ حَتَّى تَبْلُغَا جَاءَ يَوْمَ الْقِيَامَةِ أَنَا وَهُوَ'),

    -- knowledge-learning
    ('knowledge-learning', 'ask-one-real-question-today', 'Ask one real question today',
        'The Prophet (peace be upon him) said the learned are the heirs of the Prophets, who left knowledge behind instead of money.',
        'Ask a knowledgeable person one real question today, in person, online, or from a trusted book.',
        'Sunan Abi Dawud 3641', 'What have you been meaning to ask but never have?',
        'وَإِنَّ فَضْلَ الْعَالِمِ عَلَى الْعَابِدِ كَفَضْلِ الْقَمَرِ لَيْلَةَ الْبَدْرِ'),
    ('knowledge-learning', 'pass-on-one-true-thing', 'Pass on one true thing today',
        'The Prophet (peace be upon him) taught that you don''t need to know everything to share something true, even a single verse counts.',
        'Share one thing you''ve learned about Islam with someone else today, even a single verse or hadith.',
        'Sahih al-Bukhari 3461', 'What''s one thing you know that someone else might not?',
        'بَلِّغُوا عَنِّي وَلَوْ آيَةً'),

    -- work-career
    ('work-career', 'the-giving-hand-beats-the-asking-hand', 'The giving hand beats the asking hand',
        'The Prophet (peace be upon him) said the hand that gives is better than the hand that asks.',
        'Today, do one thing that moves you toward earning your own way instead of relying on someone else for it.',
        'Sahih al-Bukhari 1429', 'Where could you rely on yourself a little more today?',
        'الْيَدُ الْعُلْيَا خَيْرٌ مِنَ الْيَدِ السُّفْلَى'),
    ('work-career', 'carry-your-own-load-today', 'Carry your own load today',
        'The Prophet (peace be upon him) said it is better to carry a bundle of wood on your own back than to ask someone who may or may not give you anything.',
        'Today, do the harder task yourself instead of asking someone else to handle it for you.',
        'Sahih al-Bukhari 2074', 'What task have you been putting off asking someone else to do?',
        'لأَنْ يَحْتَطِبَ أَحَدُكُمْ حُزْمَةً عَلَى ظَهْرِهِ خَيْرٌ مِنْ أَنْ يَسْأَلَ أَحَدًا'),

    -- self-care
    ('self-care', 'your-body-has-a-right-over-you', 'Your body has a right over you',
        'The Prophet (peace be upon him) corrected a companion who was overdoing fasting and night prayer, saying the body, the eyes, and the family all have a right over a person too.',
        'Give yourself one real break today, rest, sleep, or step back from something you''ve been overdoing.',
        'Sahih al-Bukhari 1975', 'Where have you been pushing yourself past what''s actually healthy?',
        'فَإِنَّ لِجَسَدِكَ عَلَيْكَ حَقًّا'),
    ('self-care', 'there-is-good-in-you-either-way', 'There''s good in you either way',
        'The Prophet (peace be upon him) said a strong believer is better, but there is real good in a weaker believer too, so no one should lose heart.',
        'Name one good thing about where you are right now, out loud or in writing, no comparing yourself to anyone.',
        'Sahih Muslim 2664', 'Who do you compare yourself to most, and is it fair to you?',
        'الْمُؤْمِنُ الْقَوِيُّ خَيْرٌ وَأَحَبُّ إِلَى اللَّهِ مِنَ الْمُؤْمِنِ الضَّعِيفِ وَفِي كُلٍّ خَيْرٌ'),
    ('self-care', 'allah-looks-at-your-heart', 'Allah looks at your heart',
        'The Prophet (peace be upon him) taught that Allah does not judge people by their faces or their wealth, but by their hearts and their deeds.',
        'Next time you catch yourself comparing looks or money today, remind yourself that''s not what''s being weighed.',
        'Sahih Muslim 2564c', 'What do you quietly measure yourself against that doesn''t actually matter?',
        'إِنَّ اللَّهَ لاَ يَنْظُرُ إِلَى صُوَرِكُمْ وَأَمْوَالِكُمْ وَلَكِنْ يَنْظُرُ إِلَى قُلُوبِكُمْ'),

    -- social-manners
    ('social-manners', 'smile-at-others', 'Smile at others',
        'A smile counts as charity.',
        'Smile at three people today, on purpose.',
        'Jami` at-Tirmidhi 1956', 'Even the smallest gesture is worship if the intention is right.',
        'تَبَسُّمُكَ فِي وَجْهِ أَخِيكَ لَكَ صَدَقَةٌ'),

    -- work-career
    ('work-career', 'no-meal-beats-one-you-earned', 'No meal beats one you earned',
        'A Prophet, David, is held up as the example of honest, earned work.',
        'Today, take real pride in one task at work you did yourself, start to finish.',
        'Sahih al-Bukhari 2072', 'Even a Prophet worked with his own hands for his food. Honest work is never beneath anyone, no matter their status.',
        'مَا أَكَلَ أَحَدٌ طَعَامًا قَطُّ خَيْرًا مِنْ أَنْ يَأْكُلَ مِنْ عَمَلِ يَدِهِ'),

    -- food-eating
    ('food-eating', 'eat-and-drink-with-right-hand', 'Eat and drink with your right hand',
        'Right hand for eating and drinking is a clear, direct instruction.',
        'Catch yourself today if you reach with your left hand and switch to your right, on purpose.',
        'Sahih Muslim 2020a', 'It''s a tiny physical habit that carries real spiritual weight every single time you pick up food.',
        'إِذَا أَكَلَ أَحَدُكُمْ فَلْيَأْكُلْ بِيَمِينِهِ وَإِذَا شَرِبَ فَلْيَشْرَبْ بِيَمِينِهِ'),

    -- home-family
    ('home-family', 'be-the-best-to-your-family', 'Be the best to your family',
        'The Prophet''s answer to what the best of Islam looks like: feed people, greet everyone.',
        'Do one thing today that''s kinder than usual for the people you live with.',
        'Jami` at-Tirmidhi 3895', 'Your character at home, not in public, is what this hadith uses as the real measure of a person.',
        'خَيْرُكُمْ خَيْرُكُمْ لأَهْلِهِ وَأَنَا خَيْرُكُمْ لأَهْلِي'),
    ('home-family', 'your-mother-comes-first', 'Your mother comes first',
        'The repetition isn''t accidental. It''s the Prophet emphasizing, in the strongest way possible, how much your mother comes first.',
        'Call or visit your mother today, even just for two minutes, and really listen.',
        'Sahih al-Bukhari 5971', 'The Prophet said your mother three times before ever mentioning your father, when asked who deserves your best companionship.',
        'قَالَ يَا رَسُولَ اللَّهِ مَنْ أَحَقُّ بِحُسْنِ صَحَابَتِي قَالَ أُمُّكَ'),

    -- knowledge-learning
    ('knowledge-learning', 'knowledge-eases-your-path-to-jannah', 'Knowledge eases your path to Jannah',
        'Studying, one class or one lesson at a time, is described as a literal path toward Paradise.',
        'Spend 15 minutes today learning something that genuinely benefits your deen or your life.',
        'Sahih Muslim 2699a', 'This isn''t only for scholars. Any real step you take to learn something beneficial counts as walking that path.',
        'وَمَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إلَى الْجَنَّةِ'),

    -- health-cleanliness
    ('health-cleanliness', 'use-the-miswak-before-prayer', 'Use the miswak before you pray',
        'The Prophet wanted to make miswak mandatory before every single prayer.',
        'Use a miswak (or brush your teeth) right before your next prayer today.',
        'Sahih al-Bukhari 887', 'He held back only out of mercy for the difficulty, not because it mattered less.',
        'لَوْلاَ أَنْ أَشُقَّ عَلَى أُمَّتِي، لأَمَرْتُهُمْ بِالسِّوَاكِ مَعَ كُلِّ صَلاَةٍ'),
    ('health-cleanliness', 'purity-is-half-your-faith', 'Purity is half of your faith',
        'Purity and cleanliness are counted as literally half of a believer''s faith.',
        'Do wudu right now, even if you don''t have a prayer due, just to refresh it.',
        'Sahih Muslim 223', 'Washing up isn''t a side chore in Islam, it carries the weight of half your iman.',
        'الطُّهُورُ شَطْرُ الْإِيمَانِ'),

    -- character-good-deeds
    ('character-good-deeds', 'want-for-others-what-you-want-for-you', 'Want for others what you want for you',
        'Faith includes wanting good for others.',
        'Pick one person today and do for them what you''d want done for you.',
        'Sahih al-Bukhari 13', 'Faith shows in how you treat people.',
        'لَا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لِأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ')
) AS v(category_slug, slug, title, description, reflection, source, prompt, arabic_text)
JOIN categories c ON c.slug = v.category_slug;
