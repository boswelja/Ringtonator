package com.boswelja.contactringtonegenerator.ringtonegen.item

class LastName : BaseNameItem(ID.LAST_NAME) {

    override fun getLabel(): String {
        return "Last Name"
    }

    override fun getEngineText(): String = Constants.LAST_NAME_PLACEHOLDER
}
