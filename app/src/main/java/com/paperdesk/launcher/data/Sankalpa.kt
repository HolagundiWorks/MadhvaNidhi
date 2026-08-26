package com.paperdesk.launcher.data

import android.content.Context

/** Live panchanga filled into sandhya sankalpa. */
object Sankalpa {

    fun block(context: Context, sessionId: String, lang: IndicScript.Lang): String {
        val pan = PanchangaStore.forDate(context, PanchangaStore.dateKey())
        val sa = build(pan, sessionId)
        return IndicScript.convert(sa, lang)
    }

    private fun build(pan: PanchangaDay, sessionId: String): String {
        val kala = when (sessionId) {
            SandhyaStore.PRATAH -> "प्रातः"
            SandhyaStore.MADHYA -> "माध्याह्न"
            else -> "सायं"
        }
        val sam = sa(pan.samvatsara)
        val ayana = sa(pan.ayana)
        val ritu = sa(pan.ritu)
        val masa = sa(pan.masa)
        val paksha = sa(pan.paksha)
        val tithi = sa(pan.tithi)
        val vara = vara(pan.vara)
        val nak = nak(pan.nakshatra)
        val yoga = sa(pan.yoga)
        val karana = sa(pan.karana)
        return "श्रीमद्भगवतो महापुरुषस्य विष्णोराज्ञया प्रवर्तमानस्य अद्य ब्रह्मणः द्वितीयपरार्धे श्रीश्वेतवाराहकल्पे वैवस्वतमन्वन्तरे अष्टाविंशतितमे कलियुगे कलिप्रथमचरणे जम्बूद्वीपे भारतवर्षे भरतखण्डे मेरोः दक्षिणे पार्श्वे अस्मिन् वर्तमाने व्यावहारिके चान्द्रमानेन ${sam} संवत्सरे ${ayana} अयने ${ritu} ऋतौ ${masa} मासे ${paksha} पक्षे ${tithi} तिथौ ${vara} वासरे ${nak} नक्षत्रे ${yoga} योगे ${karana} करणे ${kala} काले एवंगुणविशेषणविशिष्टायां शुभतिथौ।"
    }

    private fun vara(raw: String): String = when (raw.uppercase()) {
        "SUNDAY" -> "रवि"
        "MONDAY" -> "सोम"
        "TUESDAY" -> "मङ्गल"
        "WEDNESDAY" -> "बुध"
        "THURSDAY" -> "गुरु"
        "FRIDAY" -> "शुक्र"
        "SATURDAY" -> "शनि"
        else -> sa(raw)
    }

    private fun nak(raw: String): String = when (raw.uppercase()) {
        "JYESHTHA" -> "ज्येष्ठा"
        "SHRAVANA" -> "श्रवण"
        else -> sa(raw)
    }

    private fun sa(raw: String): String {
        val key = raw.trim().uppercase()
        return MAP[key] ?: OsLang.script(raw, IndicScript.Lang.SA)
    }

    private val MAP = mapOf(
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
        "VASANTA" to "वसन्त", "GRISHMA" to "ग्रीष्म", "VARSHA" to "वर्षा",
        "SHARAD" to "शरद्", "HEMANTA" to "हेमन्त", "SHISHIRA" to "शिशिर",
        "UTTARAYANA" to "उत्तरायण", "DAKSHINAYANA" to "दक्षिणायन",
        "ASHWINI" to "अश्विनी", "BHARANI" to "भरणी", "KRITTIKA" to "कृत्तिका",
        "ROHINI" to "रोहिणी", "MRIGASHIRA" to "मृगशिरा", "ARDRA" to "आर्द्रा",
        "PUNARVASU" to "पुनर्वसु", "ASHLESHA" to "आश्लेषा",
        "PURVA PHALGUNI" to "पूर्वफाल्गुनी", "UTTARA PHALGUNI" to "उत्तरफाल्गुनी",
        "HASTA" to "हस्त", "CHITRA" to "चित्रा", "SWATI" to "स्वाती",
        "VISHAKHA" to "विशाखा", "ANURADHA" to "अनुराधा",
        "MULA" to "मूल", "PURVA ASHADHA" to "पूर्वाषाढा", "UTTARA ASHADHA" to "उत्तराषाढा",
        "DHANISHTA" to "धनिष्ठा", "SHATABHISHA" to "शतभिषा",
        "PURVA BHADRAPADA" to "पूर्वभाद्रपदा", "UTTARA BHADRAPADA" to "उत्तरभाद्रपदा",
        "REVATI" to "रेवती",
        "PRABHAVA" to "प्रभव", "VIBHAVA" to "विभव", "PRAMODA" to "प्रमोद",
        "PRAJAPATI" to "प्रजापति", "ANGIRA" to "अङ्गिरा", "SHRIMUKHA" to "श्रीमुख",
        "BHAVA" to "भाव", "YUVA" to "युवा", "DHATA" to "धाता", "DHATRI" to "धाता", "ISHVARA" to "ईश्वर",
        "BAHUDHANYA" to "बहुधान्य", "PRAMATHI" to "प्रमथि", "VIKRAMA" to "विक्रम",
        "VRISHA" to "वृष", "CHITRABHANU" to "चित्रभानु", "SVABHANU" to "स्वभानु",
        "TARANA" to "तरण", "PARTHIVA" to "पार्थिव", "VYAYA" to "व्यय",
        "SARVAJIT" to "सर्वजित्", "SARVADHARI" to "सर्वधारी", "VIRODHI" to "विरोधी",
        "VIKRITI" to "विकृति", "KHARA" to "खर", "NANDANA" to "नन्दन",
        "VIJAYA" to "विजय", "JAYA" to "जय", "MANMATHA" to "मन्मथ",
        "DURMUKHI" to "दुर्मुखी", "HEVILAMBI" to "हेविलम्बी", "VILAMBI" to "विलम्बी",
        "VIKARI" to "विकारी", "SHARVARI" to "शार्वरी", "PLAVA" to "प्लव",
        "SHUBHAKRIT" to "शुभकृत्", "SHOBHAKRIT" to "शोभकृत्", "KRODHI" to "क्रोधी",
        "VISHVAVASU" to "विश्वावसु", "VISHVA VASU" to "विश्वावसु", "PARABHAVA" to "पराभव", "PLAVANGA" to "प्लवङ्ग",
        "KILAKA" to "कीलक", "SAUMYA" to "सौम्य", "SADHARANA" to "साधारण",
        "VIRODHIKRIT" to "विरोधकृत्", "PARIDHAVI" to "परिधावी", "PRAMADI" to "प्रमादी",
        "ANANDA" to "आनन्द", "RAKSHASA" to "राक्षस", "ANALA" to "अनल",
        "PINGALA" to "पिङ्गल", "KALAYUKTI" to "कालयुक्ति", "SIDDHARTHI" to "सिद्धार्थी",
        "RAUDRA" to "रौद्र", "DURMATI" to "दुर्मति", "DUNDUBHI" to "दुन्दुभि",
        "RUDHIRODGARI" to "रुधिरोद्गारी", "RAKTAKSHA" to "रक्ताक्ष",
        "KRODHANA" to "क्रोधन", "AKSHAYA" to "अक्षय",
        "VISHKAMBHA" to "विष्कम्भ", "PRITI" to "प्रीति", "AYUSHMAN" to "आयुष्मान्",
        "SAUBHAGYA" to "सौभाग्य", "SHOBHANA" to "शोभन", "ATI GANDA" to "अतिगण्ड",
        "SUKARMA" to "सुकर्म", "DHRITI" to "धृति", "SHOOLA" to "शूल", "GANDA" to "गण्ड",
        "VRIDDHI" to "वृद्धि", "DHRUVA" to "ध्रुव", "VYAGHATA" to "व्याघात",
        "HARSHANA" to "हर्षण", "VAJRA" to "वज्र", "SIDDHI" to "सिद्धि",
        "VYATIPATA" to "व्यतीपात", "VARIYAN" to "वरीयान्", "PARIGHA" to "परिघ",
        "SHIVA" to "शिव", "SIDDHA" to "सिद्ध", "SADHYA" to "साध्य", "SHUBHA" to "शुभ",
        "BRAHMA" to "ब्रह्म", "INDRA" to "इन्द्र", "VAIDHRITI" to "वैधृति",
        "BAVA" to "बव", "BALAVA" to "बालव", "KAULAVA" to "कौलव",
        "TAITILA" to "तैतिल", "GARA" to "गर", "VANIJA" to "वणिज",
        "VISHTI" to "विष्टि", "KIMSTUGHNA" to "किंस्तुघ्न", "NAVAN" to "नाग"
    )
}
