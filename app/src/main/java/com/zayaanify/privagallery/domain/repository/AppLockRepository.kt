package com.zayaanify.privagallery.domain.repository

import kotlinx.coroutines.flow.Flow

interface AppLockRepository {
    /** PIN লক চালু আছে কিনা, রিয়েল-টাইমে observe করার জন্য। */
    fun isLockEnabled(): Flow<Boolean>

    /** নতুন PIN সেট করা বা আগের PIN বদলানো। */
    suspend fun setPin(pin: String)

    /** ইউজারের দেওয়া PIN সঠিক কিনা যাচাই করা। */
    suspend fun verifyPin(pin: String): Boolean

    /** লক ফিচার পুরোপুরি বন্ধ করা। */
    suspend fun disableLock()

    /** PIN সেট করা আছে কিনা (প্রথমবার অ্যাপ খোলার সময় সেটআপ স্ক্রিন দেখাতে হবে কিনা বোঝার জন্য)। */
    suspend fun hasPinSet(): Boolean
}
