package com.ScienceFiction.DronePassAndroid.domain.model

import java.util.Locale
import java.util.UUID

internal fun newCanonicalFirestoreUuid(): String =
    UUID.randomUUID().toString().uppercase(Locale.ROOT)
