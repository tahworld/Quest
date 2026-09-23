package com.vika.quest.model

enum class RejectionReason(val storageValue: String) {
    CANNOT_DO_NOW("cannot_do_now"), NOT_INTERESTED("not_interested"),
    TOO_EASY("too_easy"), TOO_HARD("too_hard"), LOW_VALUE("low_value"),
    ALREADY_DONE("already_done"), JUST_WANT_ANOTHER("just_want_another"), OTHER("other"),
}
