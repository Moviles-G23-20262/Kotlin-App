package com.campusswap.app.model.domain

object UniandesMajors {
    val all = listOf(
        "Administración",
        "Antropología",
        "Arquitectura",
        "Arte",
        "Biología",
        "Ciencia Política",
        "Contaduría Internacional",
        "Derecho",
        "Diseño",
        "Economía",
        "Filosofía",
        "Física",
        "Geociencias",
        "Gobierno y Asuntos Públicos",
        "Historia",
        "Historia del Arte",
        "Ingeniería Ambiental",
        "Ingeniería Biomédica",
        "Ingeniería Civil",
        "Ingeniería de Sistemas y Computación",
        "Ingeniería Eléctrica",
        "Ingeniería Electrónica",
        "Ingeniería Industrial",
        "Ingeniería Mecánica",
        "Ingeniería Química",
        "Literatura",
        "Matemáticas",
        "Medicina",
        "Microbiología",
        "Música",
        "Psicología",
        "Química",
    )

    fun error(major: String): String? = if (major in all) null else "Choose your major from the list"
}