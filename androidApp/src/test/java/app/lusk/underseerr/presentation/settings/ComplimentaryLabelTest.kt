package app.lusk.underseerr.presentation.settings

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class ComplimentaryLabelTest : StringSpec({
    // 2027-12-31T23:59:59.999Z, as the server reports it
    val endOfDay2027 = 1_830_297_599_999L

    "shows the end date in UTC with the capacity asterisk" {
        complimentarySubtitle(endOfDay2027) shouldBe
            "Free during alpha until Dec 31, 2027*\n*May end sooner, subject to capacity"
    }

    "without a date it still carries the asterisk" {
        val text = complimentarySubtitle(null)

        text shouldContain "Free during alpha*"
        text shouldContain "*May end sooner, subject to capacity"
        text shouldNotContain "until"
    }
})
