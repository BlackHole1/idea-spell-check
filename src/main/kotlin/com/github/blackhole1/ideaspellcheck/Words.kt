package com.github.blackhole1.ideaspellcheck

private val words = mutableSetOf<String>()

fun getWords(): MutableSet<String> {
    return words
}

fun replaceWords(w: List<String>) {
    words.clear()
    words.addAll(w)
}
