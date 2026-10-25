package com.github.blackhole1.ideaspellcheck.dict

import com.github.blackhole1.ideaspellcheck.containsWord
import com.github.blackhole1.ideaspellcheck.suggestWords
import com.intellij.spellchecker.dictionary.Dictionary
import com.intellij.spellchecker.dictionary.RuntimeDictionaryProvider
import com.intellij.util.Consumer

class RuntimeDictionaryProvider : RuntimeDictionaryProvider {
    override fun getDictionaries(): Array<Dictionary> {
        return arrayOf(SCDictionary)
    }
}

object SCDictionary : Dictionary {
    override fun getName(): String {
        return "spell-check"
    }

    override fun lookup(word: String): Dictionary.LookupStatus {
        return if (containsWord(word)) Dictionary.LookupStatus.Present else Dictionary.LookupStatus.Absent
    }

    // The default implementation suggests nothing, so project words would never be offered as typo fixes
    override fun consumeSuggestions(word: String, consumer: Consumer<String>) {
        suggestWords(word).forEach(consumer::consume)
    }

    override fun getWords(): MutableSet<String> {
        return com.github.blackhole1.ideaspellcheck.getWords()
    }
}
