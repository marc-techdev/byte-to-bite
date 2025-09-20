package com.example.testdesign

import android.content.Context

data class IngredientInfo(
    val canonical: String,
    val displayName: String,
    val altNames: String = "",
    val description: String = "",
    val uses: List<String> = emptyList(),
    val substitutes: List<String> = emptyList(),
    val storageTip: String = ""
)

object IngredientInfoRepository {

    private val map: Map<String, IngredientInfo> = listOf(
        IngredientInfo(
            "calamansi", "Calamansi",
            "also: kalamansi",
            "Small citrus used for marinade and sawsawan; bright, sour, aromatic.",
            listOf("Sawsawan", "Marinades", "Pancit & noodle finish"),
            listOf("Lime", "Lemon"),
            "Keep at room temp 2–3 days or refrigerate up to a week."
        ),
        IngredientInfo("carrot","Carrot","",
            "Crunchy root vegetable adding sweetness and color.",
            listOf("Ginisang gulay", "Nilaga", "Sinigang"), listOf("Chayote","Sayote"),
            "Refrigerate in crisper, 1–2 weeks."),
        IngredientInfo("chayote","Chayote","also: sayote",
            "Mild, tender gourd often sautéed or in soups.",
            listOf("Ginisang sayote","Tinola"), listOf("Upo","Papaya (green)"),
            "Refrigerate in a bag, up to 1 week."),
        IngredientInfo("egg","Egg","",
            "Adds richness and protein; binds or scrambles.", listOf("Torta","Padagdag sa pancit"),
            listOf("Tofu (firm)"), "Refrigerate; use within a week."),
        IngredientInfo("eggplant","Eggplant","also: talong",
            "Soft, absorbent vegetable great for grilling or stews.",
            listOf("Tortang talong","Pinakbet"), listOf("Zucchini"), "Cool dry place, use in 3–4 days."),
        IngredientInfo("garlic","Garlic","also: bawang",
            "Aromatic base for most savory Filipino dishes.",
            listOf("Gisa base","Marinade","Crispy topping"), listOf("Shallot (milder)"),
            "Cool, dark, dry; not refrigerated."),
        IngredientInfo("ginger","Ginger","also: luya",
            "Peppery warmth and aroma, especially in soups.",
            listOf("Tinola","Sinigang","Lugaw"), listOf("Galangal"), "Cool, dry; refrigerate peeled."),
        IngredientInfo("potato","Potato","also: patatas",
            "Starchy body for stews and adobo variants.",
            listOf("Afritada","Kaldereta","Nilaga"), listOf("Sweet potato"), "Cool, dark, dry."),
        IngredientInfo("radish","Radish","also: labanos",
            "Crisp root with mild heat used in soups and salads.",
            listOf("Sinigang","Ensalada"), listOf("Daikon"), "Refrigerate; use within 5–7 days."),
        IngredientInfo("red onion","Red Onion","also: sibuyas pula",
            "Sweet-aromatic onion; raw or sautéed.",
            listOf("Gisa base","Ensalada","Sawsawan"), listOf("White onion","Shallots"),
            "Cool, dark, ventilated; not refrigerated whole."),
        IngredientInfo("tomato","Tomato","also: kamatis",
            "Acidic sweetness; gives body to sauces and soups.",
            listOf("Gisa base","Sarsa","Sinigang"), listOf("Canned tomato"), "Room temp; refrigerate when cut."),
        IngredientInfo("white onion","White Onion","also: sibuyas puti",
            "Sharper onion used for sautéing and stocks.",
            listOf("Gisa base","Stocks","Fajita-style sautés"), listOf("Red onion","Leeks"),
            "Cool, dark, ventilated; not refrigerated whole.")
    ).associateBy { it.canonical }

    fun getInfo(context: Context, core: String): IngredientInfo {
        val key = core.trim().lowercase()
        return map[key] ?: IngredientInfo(
            canonical = key,
            displayName = core.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            description = "Basic ingredient information.",
            uses = listOf("General cooking"),
            substitutes = emptyList(),
            storageTip = "Store appropriately for freshness."
        )
    }
}
