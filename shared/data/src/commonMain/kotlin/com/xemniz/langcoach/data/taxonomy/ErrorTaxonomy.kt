package com.xemniz.langcoach.data.taxonomy

import com.xemniz.langcoach.data.db.ErrorCategory

val DEFAULT_ERROR_TAXONOMY: List<ErrorCategory> = listOf(
    ErrorCategory(code = "VERB_TENSE", name = "Verb tense", description = "Wrong tense or aspect (e.g., past for present)."),
    ErrorCategory(code = "SUBJECT_VERB_AGREEMENT", name = "Subject-verb agreement", description = "Subject and verb don't match in number/person."),
    ErrorCategory(code = "ARTICLE", name = "Article usage", description = "Missing, extra, or wrong article (a / an / the)."),
    ErrorCategory(code = "PREPOSITION", name = "Preposition", description = "Wrong or missing preposition."),
    ErrorCategory(code = "WORD_ORDER", name = "Word order", description = "Words placed in an unnatural order."),
    ErrorCategory(code = "PRONOUN", name = "Pronoun reference", description = "Wrong pronoun, gender, or unclear reference."),
    ErrorCategory(code = "PLURAL_SINGULAR", name = "Plural / singular", description = "Wrong number form on noun."),
    ErrorCategory(code = "GENDER_AGREEMENT", name = "Gender agreement", description = "Adjective/article doesn't match noun gender (where applicable)."),
    ErrorCategory(code = "VOCAB_CHOICE", name = "Vocabulary choice", description = "Wrong word for the intended meaning."),
    ErrorCategory(code = "FALSE_FRIEND", name = "False friend", description = "Cognate misused due to native-language interference."),
    ErrorCategory(code = "PRONUNCIATION", name = "Pronunciation", description = "Mispronounced word disrupting comprehension."),
    ErrorCategory(code = "REGISTER", name = "Register / formality", description = "Tone too formal or too casual for context."),
)
