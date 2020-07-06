package com.boswelja.contactringtonegenerator.ringtonegen

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.boswelja.contactringtonegenerator.StringJoinerCompat
import com.boswelja.contactringtonegenerator.contacts.Contact
import com.boswelja.contactringtonegenerator.contacts.ContactsHelper
import com.boswelja.contactringtonegenerator.mediastore.MediaStoreHelper
import com.boswelja.contactringtonegenerator.ringtonegen.item.Constants
import com.boswelja.contactringtonegenerator.ringtonegen.item.common.AudioItem
import com.boswelja.contactringtonegenerator.ringtonegen.item.common.StructureItem
import com.boswelja.contactringtonegenerator.ringtonegen.item.common.TextItem
import com.boswelja.contactringtonegenerator.tts.SynthesisJob
import com.boswelja.contactringtonegenerator.tts.SynthesisResult
import com.boswelja.contactringtonegenerator.tts.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class RingtoneGenerator(
    private val context: Context,
    private val ringtoneStructure: List<StructureItem>,
    private val contacts: List<Contact>
) :
    TtsManager.JobProgressListener,
    TtsManager.EngineEventListener {

    private val coroutineScope = MainScope()
    private val cacheDir: File = context.cacheDir
    private val ttsManager = TtsManager(context)
    private val customAudioCacheMap: HashMap<Uri, File> by lazy { HashMap() }

    private var remainingJobs: HashMap<String, Contact> = HashMap()
    private var jobsQueued: Boolean = false

    val totalJobCount: Int get() = remainingJobs.count()

    var progressListener: ProgressListener? = null
    var stateListener: StateListener? = null
    var state: State = State.NOT_READY
        private set(value) {
            field = value
            stateListener?.onStateChanged(value)
        }

    init {
        ttsManager.apply {
            jobProgressListener = this@RingtoneGenerator
            engineEventListener = this@RingtoneGenerator
        }
        coroutineScope.launch(Dispatchers.Default) {
            ringtoneStructure.filterIsInstance<AudioItem>().forEach {
                val uri = it.getAudioContentUri()
                if (uri != null) saveFileToCache(uri)
            }
            contacts.forEach {
                queueJobFor(it)
            }
            jobsQueued = true
            if (ttsManager.isEngineReady) state = State.READY
        }
    }

    override fun onInitialised(success: Boolean) {
        if (!success) throw IllegalStateException("TTS failed to initialise")
        if (jobsQueued) state = State.READY
    }

    override fun onJobStarted(synthesisJob: SynthesisJob) {
        val contact = remainingJobs[synthesisJob.id]!!
        progressListener?.onJobStarted(contact)
    }

    override fun onJobCompleted(success: Boolean, synthesisResult: SynthesisResult) {
        coroutineScope.launch(Dispatchers.Main) {
            val contact = remainingJobs[synthesisResult.id]
            val saveSuccess = if (success) handleGenerateCompleted(contact!!, synthesisResult)
            else false
            remainingJobs.remove(synthesisResult.id)
            progressListener?.onJobCompleted(saveSuccess, synthesisResult)
            if (remainingJobs.count() < 1) state = State.FINISHED
        }
    }

    private suspend fun saveFileToCache(uri: Uri) {
        withContext(Dispatchers.IO) {
            val destination: File
            val fileType = MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri))
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)!!.use {
                it.moveToFirst()
                val fileName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                destination = File(cacheDir, "$fileName.$fileType")
            }
            context.contentResolver.openInputStream(uri)?.use { inStream ->
                FileOutputStream(destination).use {
                    it.write(inStream.read())
                }
            }
            customAudioCacheMap[uri] = destination
        }
    }

    private suspend fun queueJobFor(contact: Contact) {
        withContext(Dispatchers.Default) {
            val messageBuilder = StringJoinerCompat(" ")
            ringtoneStructure.forEach {
                when (it) {
                    is TextItem -> messageBuilder.add(it.getEngineText())
                    is AudioItem -> {
                        //TODO Set this up
                    }
                }
            }
            val message = messageBuilder.toString()
                .replace(Constants.FIRST_NAME_PLACEHOLDER, contact.firstName)
                .replace(Constants.MIDDLE_NAME_PLACEHOLDER, contact.middleName ?: "")
                .replace(Constants.LAST_NAME_PLACEHOLDER, contact.lastName ?: "")
                .replace(Constants.NAME_PREFIX_PLACEHOLDER, contact.prefix ?: "")
                .replace(Constants.NAME_SUFFIX_PLACEHOLDER, contact.suffix ?: "")
                .replace(Constants.NICKNAME_PLACEHOLDER, contact.nickname ?: "")
            val synthesisId = contact.displayName.replace(" ", "_") + "-generated-ringtone"
            SynthesisJob(synthesisId, message).also {
                ttsManager.enqueueJob(it)
                remainingJobs[it.id] = contact
            }
        }
    }

    private suspend fun handleGenerateCompleted(contact: Contact, synthesisResult: SynthesisResult): Boolean {
        return withContext(Dispatchers.IO) {
            val uri = MediaStoreHelper.scanNewFile(context, synthesisResult.result)
            return@withContext if (uri != null) {
                synthesisResult.result.delete()
                ContactsHelper.setContactRingtone(context, contact, uri)
                true
            } else false
        }
    }

    fun start() {
        if (state == State.READY) {
            state = State.GENERATING
            ttsManager.startSynthesis()
        } else {
            throw IllegalStateException("Generator not ready")
        }
    }

    fun destroy() {
        ttsManager.destroy()
        cacheDir.deleteRecursively()
    }

    interface StateListener {
        fun onStateChanged(state: State)
    }

    interface ProgressListener {
        fun onJobStarted(contact: Contact)
        fun onJobCompleted(success: Boolean, synthesisResult: SynthesisResult)
    }

    enum class State {
        NOT_READY,
        READY,
        GENERATING,
        FINISHED
    }
}
