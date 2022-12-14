package com.github.blackhole1.ideaspellcheck.dict

import com.intellij.spellchecker.dictionary.Dictionary
import com.intellij.spellchecker.dictionary.RuntimeDictionaryProvider

class RuntimeDictionaryProvider : RuntimeDictionaryProvider {
    override fun getDictionaries(): Array<Dictionary> {
        return arrayOf(SCDictionary)
    }
}

object SCDictionary : Dictionary {
    override fun getName(): String {
        return "spell-check"
    }

    override fun contains(word: String): Boolean {
        return this.words.contains(word)
    }

    override fun getWords(): MutableSet<String> {
        return com.github.blackhole1.ideaspellcheck.getWords()
    }
}
