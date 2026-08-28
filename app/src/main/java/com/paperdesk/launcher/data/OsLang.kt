package com.paperdesk.launcher.data

import android.content.Context
import java.util.Calendar
import java.util.concurrent.CopyOnWriteArrayList

/** OS-wide script: Kannada only. */
object OsLang {
    private const val PREFS = "paperdesk_os_lang"
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun get(context: Context): IndicScript.Lang {
        val prefs = context.getSharedPreferences(PREFS, 0)
        if (prefs.getString("lang", "kn") != "kn") {
            prefs.edit().putString("lang", "kn").apply()
        }
        return IndicScript.Lang.KN
    }

    fun set(context: Context, lang: IndicScript.Lang) {
        if (lang != IndicScript.Lang.KN) return
        if (get(context) == IndicScript.Lang.KN) return
        context.getSharedPreferences(PREFS, 0).edit().putString("lang", "kn").apply()
        listeners.forEach { it.invoke() }
    }

    fun badge(lang: IndicScript.Lang): String = "ಕ"

    fun addListener(fn: () -> Unit) {
        listeners.add(fn)
    }

    fun removeListener(fn: () -> Unit) {
        listeners.remove(fn)
    }

    fun script(text: String, lang: IndicScript.Lang): String {
        if (text.isBlank()) return text
        val mapped = if (lang == IndicScript.Lang.EN) text else devanagari(text)
        return IndicScript.convert(mapped, lang)
    }

    fun script(context: Context, text: String): String = script(text, get(context))

    fun ui(context: Context, key: String): String = ui(key, get(context))

    fun ui(key: String, lang: IndicScript.Lang): String {
        return when (lang) {
            IndicScript.Lang.EN -> EN[key] ?: key
            IndicScript.Lang.KN -> KN[key] ?: IndicScript.convert(SA[key] ?: EN[key] ?: key, lang)
            IndicScript.Lang.SA -> SA[key] ?: EN[key] ?: key
        }
    }

    fun weekday(cal: Calendar, lang: IndicScript.Lang): String {
        val i = cal.get(Calendar.DAY_OF_WEEK)
        return ui(WEEKDAYS[i], lang)
    }

    fun weekdayShort(cal: Calendar, lang: IndicScript.Lang): String {
        val i = cal.get(Calendar.DAY_OF_WEEK)
        return ui(WEEK_SHORT[i], lang)
    }

    fun month(cal: Calendar, lang: IndicScript.Lang): String {
        val i = cal.get(Calendar.MONTH)
        return ui(MONTHS[i], lang)
    }

    fun dateLine(cal: Calendar, lang: IndicScript.Lang): String {
        val day = digits(cal.get(Calendar.DAY_OF_MONTH), lang)
        val year = digits(cal.get(Calendar.YEAR), lang)
        return "${weekday(cal, lang)}  ·  $day ${month(cal, lang)} $year"
    }

    fun dateShort(cal: Calendar, lang: IndicScript.Lang): String {
        val day = digits(cal.get(Calendar.DAY_OF_MONTH), lang)
        return "${weekdayShort(cal, lang)}  $day ${month(cal, lang)}"
    }

    fun digits(n: Int, lang: IndicScript.Lang): String = digits(n.toString(), lang)

    fun digits(raw: String, lang: IndicScript.Lang): String {
        if (lang == IndicScript.Lang.EN) return raw
        val map = if (lang == IndicScript.Lang.KN) KN_DIG else SA_DIG
        val out = StringBuilder(raw.length)
        raw.forEach { ch ->
            if (ch in '0'..'9') out.append(map[ch - '0']) else out.append(ch)
        }
        return out.toString()
    }

    private fun devanagari(text: String): String {
        var t = text
        NAMES.keys.sortedByDescending { it.length }.forEach { key ->
            val re = Regex("(?i)\\b${Regex.escape(key)}\\b")
            t = re.replace(t, NAMES[key] ?: key)
        }
        return t
    }

    private val WEEKDAYS = arrayOf(
        "", "sun", "mon", "tue", "wed", "thu", "fri", "sat"
    )
    private val WEEK_SHORT = arrayOf(
        "", "sun_s", "mon_s", "tue_s", "wed_s", "thu_s", "fri_s", "sat_s"
    )
    private val MONTHS = arrayOf(
        "jan", "feb", "mar", "apr", "may", "jun",
        "jul", "aug", "sep", "oct", "nov", "dec"
    )
    private const val SA_DIG = "०१२३४५६७८९"
    private const val KN_DIG = "೦೧೨೩೪೫೬೭೮೯"

    private val EN = mapOf(
        "desk" to "Panchanga",
        "schedule" to "Scheduler",
        "grantha" to "Grantha",
        "sandhya" to "Sandhyavandana",
        "nitya" to "Nitya Parayana",
        "mantras" to "Mantras",
        "hadugalu" to "Hadugalu",
        "japa" to "Japa",
        "profile" to "Profile",
        "aayadi" to "Aayadi",
        "stars" to "Stars",
        "done" to "Done",
        "todo" to "Todo",
        "sound" to "Sound",
        "always_on" to "Always on",
        "on" to "On",
        "off" to "Off",
        "empty_stars" to "No starred texts",
        "date" to "Date",
        "time" to "Time",
        "notes" to "Notes",
        "activity" to "Activity",
        "new_event" to "New schedule",
        "edit_event" to "Edit schedule",
        "stotra" to "Stotras",
        "namavali" to "Namavalies",
        "suktas" to "Suktas",
        "dasara" to "Dasara Padagalu",
        "puja" to "Pooja",
        "empty_page" to "Ready",
        "select_sandhya" to "Select Sandhyavandana section",
        "select_mantras" to "Select Mantra",
        "select_hadugalu" to "Select Song",
        "select_puja" to "Select Pooja",
        "sampradaya" to "Sampradaya",
        "ugabhoga" to "Ugabhoga",
        "suladhi" to "Suladhi",
        "kesava" to "Kesava nama",
        "haridasa" to "Haridasa sahitya",
        "yathi" to "Yathigalu",
        "festival" to "Festivals",
        "books" to "Books",
        "search" to "Search",
        "prev" to "Prev",
        "next" to "Next",
        "home" to "Home",
        "back" to "Back",
        "selected" to "Selected",
        "panchanga" to "Panchanga",
        "hari_motto" to "|| Hari Sarvottama Vayu Jeevottama ||",
        "lock" to "Lock",
        "unlock" to "Unlock",
        "save" to "Save",
        "cancel" to "Cancel",
        "pratah" to "Pratah",
        "madhya" to "Madhya",
        "sayam" to "Sayam",
        "sanskrit" to "Sanskrit",
        "kannada" to "Kannada",
        "english" to "English",
        "steps" to "Steps",
        "texts" to "Texts",
        "no_schedules" to "No schedules this paksha day",
        "create_schedule" to "New schedule",
        "shukla" to "Shukla paksha",
        "krishna" to "Krishna paksha",
        "sun" to "Sunday", "mon" to "Monday", "tue" to "Tuesday", "wed" to "Wednesday",
        "thu" to "Thursday", "fri" to "Friday", "sat" to "Saturday",
        "sun_s" to "Su", "mon_s" to "Mo", "tue_s" to "Tu", "wed_s" to "We",
        "thu_s" to "Th", "fri_s" to "Fr", "sat_s" to "Sa",
        "jan" to "January", "feb" to "February", "mar" to "March", "apr" to "April",
        "may" to "May", "jun" to "June", "jul" to "July", "aug" to "August",
        "sep" to "September", "oct" to "October", "nov" to "November", "dec" to "December"
    )

    private val SA = mapOf(
        "desk" to "पञ्चाङ्गम्",
        "schedule" to "कार्यक्रमसूची",
        "grantha" to "ग्रन्थः",
        "sandhya" to "सन्ध्यावन्दनम्",
        "nitya" to "नित्यपारायणम्",
        "mantras" to "मन्त्राः",
        "hadugalu" to "गीतानि",
        "japa" to "जपः",
        "profile" to "व्यक्तिविवरणम्",
        "aayadi" to "आयादि",
        "stars" to "ताराः",
        "done" to "कृतम्",
        "todo" to "कार्यम्",
        "sound" to "ध्वनिः",
        "always_on" to "नित्यप्रकाशः",
        "on" to "चालितम्",
        "off" to "निवर्तितम्",
        "empty_stars" to "न ताराः",
        "date" to "दिनाङ्कः",
        "time" to "समयः",
        "notes" to "टीकाः",
        "activity" to "कर्म",
        "new_event" to "नूतना सूची",
        "edit_event" to "सूची सम्पादनम्",
        "stotra" to "स्तोत्राणि",
        "namavali" to "नामावलयः",
        "suktas" to "सूक्तानि",
        "dasara" to "दासपदानि",
        "puja" to "पूजा",
        "empty_page" to "रिक्तम्",
        "select_sandhya" to "सन्ध्यावन्दनविभागं चिनोतु",
        "select_mantras" to "मन्त्रं चिनोतु",
        "select_hadugalu" to "गीतं चिनोतु",
        "select_puja" to "पूजां चिनोतु",
        "sampradaya" to "सम्प्रदायः",
        "ugabhoga" to "उगभोग",
        "suladhi" to "सुळादि",
        "kesava" to "केशवनाम",
        "haridasa" to "हरिदास साहित्यम्",
        "yathi" to "यतयः",
        "festival" to "उत्सवाः",
        "books" to "ग्रन्थाः",
        "search" to "अन्वेषणम्",
        "prev" to "पूर्वम्",
        "next" to "अग्रिमम्",
        "home" to "गृहम्",
        "back" to "प्रतिगमनम्",
        "selected" to "चयनितम्",
        "panchanga" to "पञ्चाङ्गम्",
        "hari_motto" to "|| हरिः सर्वोत्तमः वायुः जीवोत्तमः ||",
        "lock" to "तालकम्",
        "unlock" to "उद्घाटनम्",
        "save" to "रक्षतु",
        "cancel" to "निरस्यतु",
        "pratah" to "प्रातः",
        "madhya" to "माध्याह्नम्",
        "sayam" to "सायम्",
        "sanskrit" to "संस्कृतम्",
        "kannada" to "कन्नडम्",
        "english" to "आङ्ग्लम्",
        "steps" to "पदानि",
        "texts" to "ग्रन्थाः",
        "no_schedules" to "अस्मिन् दिने न किञ्चित्",
        "create_schedule" to "नूतना सूची",
        "shukla" to "शुक्लपक्षः",
        "krishna" to "कृष्णपक्षः",
        "sun" to "रविः", "mon" to "सोमः", "tue" to "मङ्गलः", "wed" to "बुधः",
        "thu" to "गुरुः", "fri" to "शुक्रः", "sat" to "शनिः",
        "sun_s" to "रवि", "mon_s" to "सोम", "tue_s" to "मङ्ग", "wed_s" to "बुध",
        "thu_s" to "गुरु", "fri_s" to "शुक्र", "sat_s" to "शनि",
        "jan" to "जनवरी", "feb" to "फरवरी", "mar" to "मार्च", "apr" to "अप्रैल",
        "may" to "मई", "jun" to "जून", "jul" to "जुलाई", "aug" to "अगस्त",
        "sep" to "सितम्बर", "oct" to "अक्तूबर", "nov" to "नवम्बर", "dec" to "दिसम्बर"
    )

    private val KN = mapOf(
        "desk" to "ಪಂಚಾಂಗ",
        "schedule" to "ವೇಳಾಪಟ್ಟಿ",
        "grantha" to "ಗ್ರಂಥ",
        "sandhya" to "ಸಂಧ್ಯಾವಂದನೆ",
        "nitya" to "ನಿತ್ಯ ಪಾರಾಯಣ",
        "mantras" to "ಮಂತ್ರಗಳು",
        "hadugalu" to "ಹಾಡುಗಳು",
        "japa" to "ಜಪ",
        "profile" to "ಪ್ರೊಫೈಲ್",
        "aayadi" to "ಆಯಾದಿ",
        "stars" to "ನಕ್ಷತ್ರಗಳು",
        "done" to "ಆಯಿತು",
        "todo" to "ಕಾರ್ಯ",
        "sound" to "ಧ್ವನಿ",
        "always_on" to "ಯಾವಾಗಲೂ ಬೆಳಕು",
        "on" to "ಆನ್",
        "off" to "ಆಫ್",
        "empty_stars" to "ನಕ್ಷತ್ರಗಳಿಲ್ಲ",
        "date" to "ದಿನಾಂಕ",
        "time" to "ಸಮಯ",
        "notes" to "ಟಿಪ್ಪಣಿ",
        "activity" to "ಕಾರ್ಯ",
        "new_event" to "ಹೊಸ ವೇಳಾಪಟ್ಟಿ",
        "edit_event" to "ವೇಳಾಪಟ್ಟಿ ತಿದ್ದು",
        "stotra" to "ಸ್ತೋತ್ರಗಳು",
        "stotras" to "ಸ್ತೋತ್ರಗಳು",
        "namavali" to "ನಾಮಾವಳಿಗಳು",
        "namavalis" to "ನಾಮಾವಳಿಗಳು",
        "suktas" to "ಸೂಕ್ತಗಳು",
        "rigveda_suktas" to "ಋಗ್ವೇದ ಸೂಕ್ತಗಳು",
        "sanskrit_parayana" to "ಸಂಸ್ಕೃತ ಪಾರಾಯಣ",
        "dasara" to "ದಾಸರ ಪದಗಳು",
        "dasara_padagalu" to "ದಾಸರ ಪದಗಳು",
        "puja" to "ಪೂಜೆ",
        "empty_page" to "ಸಿದ್ಧ",
        "select_sandhya" to "ಸಂಧ್ಯಾವಂದನೆ ವಿಭಾಗ ಆಯ್ಕೆ",
        "select_mantras" to "ಮಂತ್ರ ಆಯ್ಕೆ",
        "select_hadugalu" to "ಹಾಡು ಆಯ್ಕೆ",
        "select_puja" to "ಪೂಜೆ ಆಯ್ಕೆ",
        "sampradaya" to "ಸಂಪ್ರದಾಯ",
        "ugabhoga" to "ಉಗಾಭೋಗ",
        "ugabhogagalu" to "ಉಗಾಭೋಗಗಳು",
        "suladhi" to "ಸುಳಾದಿ",
        "suladhigalu" to "ಸುಳಾದಿಗಳು",
        "kesava" to "ಕೇಶವನಾಮ",
        "haridasa" to "ಹರಿದಾಸ ಸಾಹಿತ್ಯ",
        "yathi" to "ಯತಿಗಳು",
        "festival" to "ಹಬ್ಬಗಳು",
        "books" to "ಗ್ರಂಥಗಳು",
        "search" to "ಹುಡುಕು",
        "prev" to "ಹಿಂದೆ",
        "next" to "ಮುಂದೆ",
        "home" to "ಮುಖಪುಟ",
        "back" to "ಹಿಂದೆ",
        "selected" to "ಆಯ್ಕೆ",
        "panchanga" to "ಪಂಚಾಂಗ",
        "hari_motto" to "|| ಹರಿ ಸರ್ವೋತ್ತಮ ವಾಯು ಜೀವೋತ್ತಮ ||",
        "lock" to "ಲಾಕ್",
        "unlock" to "ಅನ್‌ಲಾಕ್",
        "save" to "ಉಳಿಸು",
        "cancel" to "ರದ್ದು",
        "pratah" to "ಪ್ರಾತಃ",
        "madhya" to "ಮಾಧ್ಯಾಹ್ನ",
        "sayam" to "ಸಾಯಂ",
        "sanskrit" to "ಸಂಸ್ಕೃತ",
        "kannada" to "ಕನ್ನಡ",
        "english" to "ಇಂಗ್ಲಿಷ್",
        "steps" to "ಹಂತಗಳು",
        "texts" to "ಗ್ರಂಥಗಳು",
        "no_schedules" to "ಈ ಪಕ್ಷದ ದಿನದಲ್ಲಿ ವೇಳಾಪಟ್ಟಿ ಇಲ್ಲ",
        "create_schedule" to "ಹೊಸ ವೇಳಾಪಟ್ಟಿ",
        "shukla" to "ಶುಕ್ಲಪಕ್ಷ",
        "krishna" to "ಕೃಷ್ಣಪಕ್ಷ",
        "nithya_parayana" to "ನಿತ್ಯ ಪಾರಾಯಣ",
        "sun" to "ಭಾನುವಾರ", "mon" to "ಸೋಮವಾರ", "tue" to "ಮಂಗಳವಾರ", "wed" to "ಬುಧವಾರ",
        "thu" to "ಗುರುವಾರ", "fri" to "ಶುಕ್ರವಾರ", "sat" to "ಶನಿವಾರ",
        "sun_s" to "ಭಾ", "mon_s" to "ಸೋ", "tue_s" to "ಮಂ", "wed_s" to "ಬು",
        "thu_s" to "ಗು", "fri_s" to "ಶು", "sat_s" to "ಶ",
        "jan" to "ಜನವರಿ", "feb" to "ಫೆಬ್ರವರಿ", "mar" to "ಮಾರ್ಚ್", "apr" to "ಏಪ್ರಿಲ್",
        "may" to "ಮೇ", "jun" to "ಜೂನ್", "jul" to "ಜುಲೈ", "aug" to "ಆಗಸ್ಟ್",
        "sep" to "ಸೆಪ್ಟೆಂಬರ್", "oct" to "ಅಕ್ಟೋಬರ್", "nov" to "ನವೆಂಬರ್", "dec" to "ಡಿಸೆಂಬರ್"
    )

    private val NAMES = mapOf(
        "PRATIPADA" to "प्रतिपदा", "DWITIYA" to "द्वितीया", "TRITIYA" to "तृतीया",
        "CHATURTHI" to "चतुर्थी", "PANCHAMI" to "पञ्चमी", "SHASHTHI" to "षष्ठी",
        "SAPTAMI" to "सप्तमी", "ASHTAMI" to "अष्टमी", "NAVAMI" to "नवमी",
        "DASHAMI" to "दशमी", "EKADASHI" to "एकादशी", "DWADASHI" to "द्वादशी",
        "TRAYODASHI" to "त्रयोदशी", "CHATURDASHI" to "चतुर्दशी",
        "PURNIMA" to "पूर्णिमा", "AMAVASYA" to "अमावास्या",
        "SHUKLA" to "शुक्ल", "KRISHNA" to "कृष्ण",
        "CHAITRA" to "चैत्र", "VAISHAKHA" to "वैशाख", "JYESHTHA" to "ज्येष्ठ",
        "ASHADHA" to "आषाढ", "SHRAVANA" to "श्रावण", "BHADRAPADA" to "भाद्रपद",
        "ASHWAYUJA" to "आश्वयुज", "KARTIKA" to "कार्तिक", "MARGASHIRSHA" to "मार्गशीर्ष",
        "PUSHYA" to "पुष्य", "MAGHA" to "माघ", "PHALGUNA" to "फाल्गुन",
        "ASHWINI" to "अश्विनी", "BHARANI" to "भरणी", "KRITTIKA" to "कृत्तिका",
        "ROHINI" to "रोहिणी", "MRIGASHIRA" to "मृगशिरा", "ARDRA" to "आर्द्रा",
        "PUNARVASU" to "पुनर्वसु", "ASHLESHA" to "आश्लेषा",
        "PURVA PHALGUNI" to "पूर्व फाल्गुनी", "UTTARA PHALGUNI" to "उत्तर फाल्गुनी",
        "HASTA" to "हस्त", "CHITRA" to "चित्रा", "SWATI" to "स्वाती",
        "VISHAKHA" to "विशाखा", "ANURADHA" to "अनुराधा", "MULA" to "मूल",
        "PURVA ASHADHA" to "पूर्वाषाढा", "UTTARA ASHADHA" to "उत्तराषाढा",
        "DHANISHTA" to "धनिष्ठा", "SHATABHISHA" to "शतभिषा",
        "PURVA BHADRAPADA" to "पूर्व भाद्रपदा", "UTTARA BHADRAPADA" to "उत्तर भाद्रपदा",
        "REVATI" to "रेवती",
        "UTTARAYANA" to "उत्तरायण", "DAKSHINAYANA" to "दक्षिणायन",
        "NAKSHATRA" to "नक्षत्र",
        "PRABHAVA" to "प्रभव", "VIBHAVA" to "विभव", "PRAMODA" to "प्रमोद",
        "PRAJAPATI" to "प्रजापति", "ANGIRA" to "अङ्गिरा", "SHRIMUKHA" to "श्रीमुख",
        "BHAVA" to "भाव", "YUVA" to "युवा", "DHATA" to "धाता", "DHATRI" to "धाता",
        "ISHVARA" to "ईश्वर", "BAHUDHANYA" to "बहुधान्य", "PRAMATHI" to "प्रमथि",
        "VIKRAMA" to "विक्रम", "VRISHA" to "वृष", "CHITRABHANU" to "चित्रभानु",
        "SVABHANU" to "स्वभानु", "TARANA" to "तरण", "PARTHIVA" to "पार्थिव",
        "VYAYA" to "व्यय", "SARVAJIT" to "सर्वजित्", "SARVADHARI" to "सर्वधारी",
        "VIRODHI" to "विरोधी", "VIKRITI" to "विकृति", "KHARA" to "खर",
        "NANDANA" to "नन्दन", "VIJAYA" to "विजय", "JAYA" to "जय",
        "MANMATHA" to "मन्मथ", "DURMUKHI" to "दुर्मुखी", "HEVILAMBI" to "हेविलम्बी",
        "VILAMBI" to "विलम्बी", "VIKARI" to "विकारी", "SHARVARI" to "शार्वरी",
        "PLAVA" to "प्लव", "SHUBHAKRIT" to "शुभकृत्", "SHOBHAKRIT" to "शोभकृत्",
        "KRODHI" to "क्रोधी", "VISHVAVASU" to "विश्वावसु", "VISHVA VASU" to "विश्वावसु",
        "PARABHAVA" to "पराभव", "PLAVANGA" to "प्लवङ्ग", "KILAKA" to "कीलक",
        "SAUMYA" to "सौम्य", "SADHARANA" to "साधारण", "VIRODHIKRIT" to "विरोधकृत्",
        "PARIDHAVI" to "परिधावी", "PRAMADI" to "प्रमादी", "ANANDA" to "आनन्द",
        "RAKSHASA" to "राक्षस", "ANALA" to "अनल", "NALA" to "नल",
        "PINGALA" to "पिङ्गल", "KALAYUKTI" to "कालयुक्ति", "SIDDHARTHI" to "सिद्धार्थी",
        "RAUDRA" to "रौद्र", "DURMATI" to "दुर्मति", "DUNDUBHI" to "दुन्दुभि",
        "RUDHIRODGARI" to "रुधिरोद्गारी", "RAKTAKSHA" to "रक्ताक्ष",
        "KRODHANA" to "क्रोधन", "AKSHAYA" to "अक्षय",
        "VISHKAMBHA" to "विष्कम्भ", "PRITI" to "प्रीति", "AYUSHMAN" to "आयुष्मान्",
        "SAUBHAGYA" to "सौभाग्य", "SHOBHANA" to "शोभन", "ATI GANDA" to "अतिगण्ड",
        "SUKARMA" to "सुकर्म", "DHRITI" to "धृति", "SHOOLA" to "शूल",
        "GANDA" to "गण्ड", "VRIDDHI" to "वृद्धि", "DHRUVA" to "ध्रुव",
        "VYAGHATA" to "व्याघात", "HARSHANA" to "हर्षण", "VAJRA" to "वज्र",
        "SIDDHI" to "सिद्धि", "VYATIPATA" to "व्यतीपात", "VARIYAN" to "वरीयान्",
        "PARIGHA" to "परिघ", "SHIVA" to "शिव", "SIDDHA" to "सिद्ध",
        "SADHYA" to "साध्य", "SHUBHA" to "शुभ", "BRAHMA" to "ब्रह्म",
        "INDRA" to "इन्द्र", "VAIDHRITI" to "वैधृति",
        "BAVA" to "बव", "BALAVA" to "बालव", "KAULAVA" to "कौलव",
        "TAITILA" to "तैतिल", "GARA" to "गर", "VANIJA" to "वणिज",
        "VISHTI" to "विष्टि", "KIMSTUGHNA" to "किंस्तुघ्न", "NAVAN" to "नवं",
        "SUNDAY" to "रविवासर", "MONDAY" to "सोमवासर",
        "TUESDAY" to "मङ्गलवासर", "WEDNESDAY" to "बुधवासर",
        "THURSDAY" to "गुरुवासर", "FRIDAY" to "शुक्रवासर", "SATURDAY" to "शनिवासर",
        "VASANTA" to "वसन्त", "GRISHMA" to "ग्रीष्म", "VARSHA" to "वर्षा",
        "SHARAD" to "शरद्", "HEMANTA" to "हेमन्त", "SHISHIRA" to "शिशिर",
        "SRS MANTRALAYA" to "एसआरएस मन्त्रालय",
        "PURN" to "पूर्णिमा", "AMA" to "अमावास्या", "PRA" to "प्रतिपदा",
        "DWI" to "द्वि", "TRI" to "त्रि", "CHT" to "चतुर्थी", "PAN" to "पञ्चमी",
        "SHA" to "षष्ठी", "SAP" to "सप्तमी", "ASH" to "अष्टमी", "NAV" to "नवमी",
        "DAS" to "दशमी", "EKA" to "एकादशी", "DWA" to "द्वादशी", "TRA" to "त्रयोदशी",
        "CHD" to "चतुर्दशी"
    )
}
