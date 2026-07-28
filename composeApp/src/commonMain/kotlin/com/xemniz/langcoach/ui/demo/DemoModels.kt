package com.xemniz.langcoach.ui.demo

data class DemoRecap(
    val duration: String,
    val turns: Int,
    val cost: String,
    val strength: String,
    val correctionBefore: String,
    val correctionAfter: String,
    val newWords: List<String>,
    val nextFocus: String,
)

val portfolioDemoRecap = DemoRecap(
    duration = "1:24",
    turns = 6,
    cost = "$0.08",
    strength = "You told a clear story and used connecting phrases naturally.",
    correctionBefore = "Ayer voy al mercado con mi amiga.",
    correctionAfter = "Ayer fui al mercado con mi amiga.",
    newWords = listOf("puesto", "regatear", "dar una vuelta"),
    nextFocus = "Past-tense contrast: fui vs. iba",
)
