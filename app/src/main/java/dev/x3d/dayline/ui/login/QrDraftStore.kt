package dev.x3d.dayline.ui.login

import dev.x3d.dayline.domain.model.School

data class QrDraft(val user: String, val secret: String, val school: School)

class QrDraftStore {
    @Volatile
    var latest: QrDraft? = null
}
