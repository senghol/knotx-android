package com.senghol.knotx.utils.objects

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

object ReviewHelper {
    fun askForReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                manager.launchReviewFlow(activity, request.result)
            }
        }
    }
}