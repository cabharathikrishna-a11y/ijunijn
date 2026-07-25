package com.example.api

import android.content.Context
import android.util.Log
import com.example.util.TimeEngine
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * FirestoreCleaner
 *
 * Utility to inspect, clean, and consolidate Firestore user history and temporary nodes.
 *
 * KEY RESPONSIBILITIES:
 * 1. Sanitizes user email key so user always reconnects to the exact same folder (no duplicate folders on login/logout).
 * 2. Prunes duplicate session documents, empty/corrupted drafts, and orphaned temporary records in Firestore.
 * 3. Consolidates legacy raw email document structures into the normalized `users/{sanitizedEmail}` path.
 */
object FirestoreCleaner {
    private const val TAG = "FirestoreCleaner"
    private const val MIN_CLEAN_INTERVAL_MS = 60000L // 1 minute debounce per user

    private val lastCleanTimes = ConcurrentHashMap<String, Long>()
    private val isCleaningMap = ConcurrentHashMap<String, AtomicBoolean>()

    fun cleanUserData(context: Context, email: String, force: Boolean = false) {
        val sanitized = DevicePresenceManager.sanitizeEmail(email)
        if (sanitized.isBlank()) return

        val now = TimeEngine.getTrueTimeMs()
        val lastRun = lastCleanTimes[sanitized] ?: 0L
        if (!force && (now - lastRun < MIN_CLEAN_INTERVAL_MS)) {
            Log.d(TAG, "Cleaner skipped for $sanitized (debounced within $MIN_CLEAN_INTERVAL_MS ms)")
            return
        }

        val flag = isCleaningMap.computeIfAbsent(sanitized) { AtomicBoolean(false) }
        if (!flag.compareAndSet(false, true)) {
            Log.d(TAG, "Cleaning already in progress for $sanitized")
            return
        }

        lastCleanTimes[sanitized] = now

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!com.example.util.NetworkChecker.isOnline(context)) {
                    flag.set(false)
                    return@launch
                }

                val firestore = FirebaseFirestore.getInstance(
                    FirebaseApp.getInstance(),
                    "main"
                )

                // 1. Check for legacy non-sanitized document paths (e.g. raw email with upper case or dots)
                val rawTrimmedEmail = email.trim()
                if (rawTrimmedEmail.isNotEmpty() && rawTrimmedEmail != sanitized) {
                    consolidateLegacyUserFolder(firestore, rawTrimmedEmail, sanitized)
                }

                // 2. Clean focus_records subcollection under sanitized email
                cleanSubcollectionDuplicates(firestore, sanitized, "focus_records")

                // 3. Clean focus_history subcollection under sanitized email
                cleanSubcollectionDuplicates(firestore, sanitized, "focus_history")

                Log.d(TAG, "Firestore cleaning & consolidation completed successfully for $sanitized")
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning user data for $sanitized", e)
            } finally {
                flag.set(false)
            }
        }
    }

    private suspend fun consolidateLegacyUserFolder(
        firestore: FirebaseFirestore,
        legacyKey: String,
        targetKey: String
    ) {
        try {
            val legacyDocRef = firestore.collection("users").document(legacyKey)
            val legacyDocSnap = legacyDocRef.get().await()

            if (legacyDocSnap.exists()) {
                Log.i(TAG, "Found legacy user folder '$legacyKey', consolidating to '$targetKey'")
                
                // Copy collections
                val collections = listOf("focus_records", "focus_history")
                for (col in collections) {
                    val query = legacyDocRef.collection(col).get().await()
                    for (doc in query.documents) {
                        val data = doc.data ?: continue
                        firestore.collection("users").document(targetKey)
                            .collection(col).document(doc.id)
                            .set(data, com.google.firebase.firestore.SetOptions.merge())
                            .await()
                        // Delete legacy doc
                        doc.reference.delete().await()
                    }
                }
                
                // Delete legacy user root document
                legacyDocRef.delete().await()
                Log.i(TAG, "Successfully migrated legacy folder '$legacyKey' -> '$targetKey'")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice during legacy folder consolidation: ${e.message}")
        }
    }

    private suspend fun cleanSubcollectionDuplicates(
        firestore: FirebaseFirestore,
        sanitizedEmail: String,
        collectionName: String
    ) {
        try {
            val colRef = firestore.collection("users").document(sanitizedEmail).collection(collectionName)
            val snapshot = colRef.get().await()

            val seenSessionMap = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()

            for (doc in snapshot.documents) {
                val sId = doc.getString("Session_ID") ?: doc.getString("recordId") ?: doc.id
                val focusMs = doc.getLong("Total_Focus_Time_Ms") ?: doc.getLong("totalFocusMs") ?: 0L

                if (sId.isBlank() || sId == "null" || sId == "undefined") {
                    Log.w(TAG, "Deleting corrupted $collectionName doc with invalid ID: ${doc.id}")
                    doc.reference.delete().await()
                    continue
                }

                val existing = seenSessionMap[sId]
                if (existing != null) {
                    val existingFocusMs = existing.getLong("Total_Focus_Time_Ms") ?: existing.getLong("totalFocusMs") ?: 0L
                    if (focusMs >= existingFocusMs) {
                        Log.i(TAG, "Deleting duplicate session doc ${existing.id} in favor of ${doc.id}")
                        existing.reference.delete().await()
                        seenSessionMap[sId] = doc
                    } else {
                        Log.i(TAG, "Deleting redundant session doc ${doc.id}")
                        doc.reference.delete().await()
                    }
                } else {
                    seenSessionMap[sId] = doc
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning subcollection $collectionName for $sanitizedEmail: ${e.message}")
        }
    }
}
