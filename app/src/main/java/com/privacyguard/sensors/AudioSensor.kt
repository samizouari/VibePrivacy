package com.privacyguard.sensors

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Capteur audio pour détection de bruits suspects
 * 
 * Analyse le niveau sonore ambiant pour détecter :
 * - Conversations proches
 * - Bruits inhabituels
 * - Augmentation soudaine du volume
 * 
 * N'enregistre PAS l'audio, mesure uniquement l'amplitude.
 * Privacy-first: aucune donnée audio n'est stockée.
 */
class AudioSensor(context: Context) : BaseSensor<AudioData>(context, "AudioSensor") {
    
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Configuration audio
    private val sampleRate = 44100 // 44.1 kHz
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2
    
    // Seuils de détection (en décibels)
    private val silenceThresholdDb = 40f // En dessous = silence
    private val normalConversationDb = 60f // Conversation normale
    private val loudNoiseDb = 80f // Bruit fort
    
    private var isRecording = false
    
    override suspend fun onStart() {
        Timber.d("AudioSensor: Starting audio monitoring...")
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Timber.e("AudioSensor: Failed to initialize AudioRecord")
                return
            }
            
            audioRecord?.startRecording()
            isRecording = true
            
            // Démarrer l'analyse audio dans une coroutine
            recordingJob = scope.launch {
                analyzeAudio()
            }
            
            Timber.i("AudioSensor: Audio monitoring started")
        } catch (e: SecurityException) {
            Timber.e(e, "AudioSensor: Permission denied for microphone")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "AudioSensor: Failed to start audio monitoring")
            throw e
        }
    }
    
    override suspend fun onStop() {
        Timber.d("AudioSensor: Stopping audio monitoring...")
        
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        Timber.i("AudioSensor: Audio monitoring stopped")
    }
    
    /**
     * Analyse continue de l'audio
     */
    private suspend fun analyzeAudio() {
        val buffer = ShortArray(bufferSize / 2)
        
        while (isRecording && isActive) {
            val timestamp = System.currentTimeMillis()
            
            // Lire les échantillons audio
            val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            
            if (read > 0) {
                // Calculer l'amplitude RMS (Root Mean Square)
                val rms = calculateRMS(buffer, read)
                
                // Convertir en décibels
                val decibels = amplitudeToDecibels(rms)
                
                // Évaluer le niveau de menace
                val (threatLevel, confidence) = evaluateThreatLevel(decibels)
                
                // Émettre les données
                val audioDataToEmit = AudioData(
                    timestamp = timestamp,
                    threatLevel = threatLevel,
                    confidence = confidence,
                    averageDecibels = decibels,
                    peakDecibels = decibels, // Simplifié pour MVP
                    isSpeechDetected = decibels > normalConversationDb
                )
                
                Timber.i("AudioSensor: EMITTING data - dB=${decibels.toInt()}, speech=${audioDataToEmit.isSpeechDetected}, threat=$threatLevel")
                emitData(audioDataToEmit)
                
                Timber.v("AudioSensor: Level=${decibels.toInt()}dB, threat=$threatLevel")
            }
            
            // Analyser toutes les 500ms
            delay(500)
        }
    }
    
    /**
     * Calcule l'amplitude RMS d'un buffer audio
     */
    private fun calculateRMS(buffer: ShortArray, read: Int): Double {
        var sum = 0.0
        for (i in 0 until read) {
            val sample = buffer[i].toDouble()
            sum += sample * sample
        }
        return sqrt(sum / read)
    }
    
    /**
     * Convertit une amplitude en décibels
     */
    private fun amplitudeToDecibels(amplitude: Double): Float {
        if (amplitude <= 0) return 0f
        
        // Formule: dB = 20 * log10(amplitude / référence)
        // Référence = amplitude maximale pour 16-bit audio = 32768
        val db = 20.0 * log10(amplitude / 32768.0)
        
        // Normaliser entre 0 et 120 dB
        return (db + 120).toFloat().coerceIn(0f, 120f)
    }
    
    /**
     * Évalue le niveau de menace selon le niveau sonore
     */
    private fun evaluateThreatLevel(decibels: Float): Pair<ThreatLevel, Float> {
        return when {
            // Silence = aucune menace
            decibels < silenceThresholdDb -> ThreatLevel.NONE to 1.0f
            
            // Bruit très fort = haute menace
            decibels > loudNoiseDb -> ThreatLevel.HIGH to 0.8f
            
            // Conversation normale = menace moyenne
            decibels > normalConversationDb -> ThreatLevel.MEDIUM to 0.7f
            
            // Léger bruit = faible menace
            else -> ThreatLevel.LOW to 0.5f
        }
    }
    
    /**
     * Nettoyage des ressources
     */
    fun cleanup() {
        scope.cancel()
    }
}

