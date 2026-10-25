package com.github.blackhole1.ideaspellcheck.dict

import com.github.blackhole1.ideaspellcheck.containsWord
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

    override fun lookup(word: String): Dictionary.LookupStatus {
        return if (containsWord(word)) Dictionary.LookupStatus.Present else Dictionary.LookupStatus.Absent
    }

    override fun getWords(): MutableSet<String> {
        return com.github.blackhole1.ideaspellcheck.getWords()
    }
}
