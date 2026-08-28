package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONObject

data class CatalogueGroup(
    val id: String,
    val kn: String,
    val sa: String,
    val en: String
)

data class CatalogueItem(
    val id: String,
    val group: String,
    val kn: String,
    val sa: String,
    val en: String,
    val type: String,
    val deity: String,
    val author: String,
    val file: String,
    val script: String,
    val url: String
)

/** Madhwa Festivals master catalogue. Titles from the catalogue; bodies from local texts only. */
object CatalogueStore {
    @Volatile private var loaded = false
    private var groups: List<CatalogueGroup> = emptyList()
    private var items: List<CatalogueItem> = emptyList()
    private var byId: Map<String, CatalogueItem> = emptyMap()

    @Synchronized
    fun load(context: Context): List<CatalogueItem> {
        if (loaded) return items
        val raw = runCatching {
            context.assets.open("catalogue/index.json").bufferedReader().use { it.readText() }
        }.getOrDefault("")
        val root = runCatching { JSONObject(raw) }.getOrNull()
        val gOut = ArrayList<CatalogueGroup>()
        val iOut = ArrayList<CatalogueItem>()
        val gArr = root?.optJSONArray("groups")
        if (gArr != null) {
            for (i in 0 until gArr.length()) {
                val g = gArr.optJSONObject(i) ?: continue
                val gid = g.optString("id")
                if (gid.isBlank()) continue
                gOut += CatalogueGroup(
                    id = gid,
                    kn = g.optString("kn"),
                    sa = g.optString("sa"),
                    en = g.optString("en")
                )
                val arr = g.optJSONArray("items") ?: continue
                for (j in 0 until arr.length()) {
                    val o = arr.optJSONObject(j) ?: continue
                    val id = o.optString("id").ifBlank { continueId(gid, j) }
                    iOut += CatalogueItem(
                        id = id,
                        group = gid,
                        kn = o.optString("kn"),
                        sa = o.optString("sa"),
                        en = o.optString("en"),
                        type = o.optString("type", gid),
                        deity = o.optString("deity"),
                        author = o.optString("author"),
                        file = o.optString("file"),
                        script = o.optString("script", "sa"),
                        url = o.optString("url")
                    )
                }
            }
        }
        groups = gOut
        items = iOut
        byId = iOut.associateBy { it.id }
        loaded = true
        augment(context)
        return items
    }

    fun groups(): List<CatalogueGroup> = groups

    fun groupsFor(page: String): List<CatalogueGroup> {
        val songs = setOf("dasara_padagalu", "suladhigalu", "ugabhogagalu")
        return if (page == "hadugalu") groups.filter { it.id in songs }
        else groups.filter { it.id !in songs }
    }

    fun item(id: String): CatalogueItem? = byId[id]

    fun first(): CatalogueItem? = items.firstOrNull()

    fun firstReadable(): CatalogueItem? =
        items.firstOrNull { it.file.isNotBlank() } ?: items.firstOrNull()

    fun firstReadable(page: String): CatalogueItem? {
        val allowed = groupsFor(page).map { it.id }.toSet()
        val mine = items.filter { it.group in allowed }
        return mine.firstOrNull { it.file.isNotBlank() } ?: mine.firstOrNull()
    }

    fun firstReadable(page: String, subpage: String): CatalogueItem? {
        val allowed = groupsFor(page).map { it.id }.toSet()
        val mine = items.filter { it.group in allowed && subpageFor(it) == subpage }
        return mine.firstOrNull { it.file.isNotBlank() } ?: mine.firstOrNull()
    }

    fun groupsForSubpage(page: String, subpage: String): List<CatalogueGroup> {
        val allowed = groupsFor(page)
        return allowed.filter { group ->
            items.any { it.group == group.id && it.file.isNotBlank() && subpageFor(it) == subpage }
        }
    }

    fun inGroup(group: String): List<CatalogueItem> {
        val base = if (group.isBlank()) items else items.filter { it.group == group }
        return base.filter { it.file.isNotBlank() }
    }

    fun inGroup(group: String, subpage: String): List<CatalogueItem> =
        inGroup(group).filter { subpageFor(it) == subpage }

    fun belongsToSubpage(item: CatalogueItem, subpage: String): Boolean = subpageFor(item) == subpage

    private fun subpageFor(item: CatalogueItem): String {
        val searchable = "${item.id} ${item.kn} ${item.sa} ${item.en}".lowercase()
        return when {
            item.group == "rigveda_suktas" || "sukta" in searchable || "सूक्त" in searchable || "ಸೂಕ್ತ" in searchable -> "suktas"
            item.type.equals("stotra", true) || item.group == "stotras" || item.group == "sanskrit_parayana" -> "stotra"
            else -> "mantras"
        }
    }

    fun groupLabel(group: CatalogueGroup, lang: IndicScript.Lang): String = when (lang) {
        IndicScript.Lang.KN -> group.kn.ifBlank { group.en }
        IndicScript.Lang.SA -> group.sa.ifBlank { group.en }
        IndicScript.Lang.EN -> group.en.ifBlank { group.kn }
    }

    fun label(item: CatalogueItem, lang: IndicScript.Lang): String = when (lang) {
        IndicScript.Lang.KN -> item.kn.ifBlank { item.en.ifBlank { item.sa } }
        IndicScript.Lang.SA -> item.sa.ifBlank { item.kn.ifBlank { item.en } }
        IndicScript.Lang.EN -> item.en.ifBlank { item.kn.ifBlank { item.sa } }
    }

    fun body(context: Context, item: CatalogueItem, lang: IndicScript.Lang): String {
        if (item.file.isBlank()) return ""
        val raw = read(context, item.file)
        if (raw.isBlank()) return ""
        val tidy = RitualText.tidy(raw, kannada = item.script == "kn")
        return if (item.script == "kn") {
            when (lang) {
                // Some imported Kannada catalogue files contain Devanagari lines.
                // Normalising the whole body keeps mixed-script texts consistent.
                IndicScript.Lang.KN -> IndicScript.convert(tidy, IndicScript.Lang.KN)
                IndicScript.Lang.EN -> IndicScript.convert(tidy, IndicScript.Lang.EN)
                IndicScript.Lang.SA -> ""
            }
        } else {
            when (lang) {
                IndicScript.Lang.KN -> IndicScript.convert(tidy, IndicScript.Lang.KN)
                IndicScript.Lang.EN -> IndicScript.convert(tidy, IndicScript.Lang.EN)
                IndicScript.Lang.SA -> IndicScript.convert(tidy, IndicScript.Lang.SA)
            }
        }
    }

    private fun read(context: Context, file: String): String {
        if (file.startsWith("lib:")) {
            val rest = file.removePrefix("lib:")
            val slash = rest.indexOf('/')
            if (slash <= 0) return ""
            return LibraryStore.text(
                context,
                rest.substring(0, slash),
                rest.substring(slash + 1),
                IndicScript.Lang.SA
            )
        }
        val path = when {
            file.startsWith("madhwa/") -> file
            file.endsWith(".txt") -> "madhwa/texts/$file"
            else -> "madhwa/texts/$file"
        }
        return runCatching {
            context.assets.open(path).bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }

    private fun augment(context: Context) {
        val rigveda = setOf(
            "AghamarshanaSuktam", "AyushyaSuktam", "BhagyaSuktam", "BhuSuktam",
            "PurushaSuktam", "ShriSuktam", "VarunaSuktam", "VishnuSuktam"
        )
        if (groups.none { it.id == "rigveda_suktas" }) {
            groups = groups + CatalogueGroup("rigveda_suktas", "ಋಗ್ವೇದ ಸೂಕ್ತಗಳು", "ऋग्वेदसूक्तानि", "Rigveda Suktas")
        }
        if (groups.none { it.id == "sanskrit_parayana" }) {
            groups = groups + CatalogueGroup("sanskrit_parayana", "ಸಂಸ್ಕೃತ ಪಾರಾಯಣ", "संस्कृतपारायणम्", "Sanskrit Parayana")
        }
        val skip = setOf(
            "Sandhyavandanam", "SandhyavandanaPrayoga", "SandhyavandanaMantrah",
            "SandhyopasthanaMantrah", "Aupasanam", "MahaNyasah", "LaghuNyasah",
            "RudraPrashnah", "RudraPadaPatha", "ChamakaPrashnah",
            "shiva-puja-with-basic-mahanyasa", "UdakaShanti", "YajurUpakarma"
        )
        val extra = ArrayList<CatalogueItem>()
        LibraryStore.load(context, LibraryStore.MANTRAS).forEach { lib ->
            if (lib.id in skip) return@forEach
            val file = "lib:mantras/${lib.id}"
            if (items.any { it.file == file || it.id == lib.id || it.id == "lib_${lib.id}" }) return@forEach
            extra += CatalogueItem(
                id = "lib_${lib.id}",
                group = if (lib.id in rigveda) "rigveda_suktas" else "mantras",
                kn = IndicScript.convert(lib.title, IndicScript.Lang.KN),
                sa = lib.title,
                en = IndicScript.convert(lib.title, IndicScript.Lang.EN),
                type = "mantra",
                deity = "",
                author = "",
                file = file,
                script = "sa",
                url = ""
            )
        }
        val requestedStotras = setOf(
            "VishnuSahasranamaStotram", "VenkateshaSuprabhatam", "AdityaHrdayam",
            "LakshmiNrisimhaKarunarasaStotram", "LakshmiNrisimhaPancharatnam"
        )
        LibraryStore.load(context, LibraryStore.STOTRA).forEach { lib ->
            if (lib.id !in requestedStotras) return@forEach
            val file = "lib:stotra/${lib.id}"
            if (items.any { it.file == file }) return@forEach
            extra += CatalogueItem(
                id = "lib_${lib.id}", group = "sanskrit_parayana",
                kn = IndicScript.convert(lib.title, IndicScript.Lang.KN), sa = lib.title,
                en = IndicScript.convert(lib.title, IndicScript.Lang.EN), type = "stotra",
                deity = "", author = "", file = file, script = "sa", url = ""
            )
        }
        if (extra.isEmpty()) return
        items = items + extra
        byId = items.associateBy { it.id }
    }

    private fun continueId(group: String, index: Int): String = "${group}_$index"
}
