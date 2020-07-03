package com.boswelja.contactringtonegenerator.ringtonegen.item

import com.boswelja.contactringtonegenerator.R

class FirstName : BaseNameItem(ID.FIRST_NAME) {

    override fun getLabelRes(): Int = labelRes
    override fun getEngineText(): String = Constants.FIRST_NAME_PLACEHOLDER

    companion object {
        const val labelRes: Int = R.string.label_first_name
    }
}