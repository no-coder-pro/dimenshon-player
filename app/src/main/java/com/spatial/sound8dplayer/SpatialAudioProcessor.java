package com.spatial.sound8dplayer;

import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.UnstableApi;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@UnstableApi
public class SpatialAudioProcessor implements AudioProcessor {

    private AudioFormat inputAudioFormat = AudioFormat.NOT_SET;
    private AudioFormat outputAudioFormat = AudioFormat.NOT_SET;

    private ByteBuffer buffer = EMPTY_BUFFER;
    private ByteBuffer outputBuffer = EMPTY_BUFFER;
    private boolean inputEnded = false;

    private volatile Spatial8DEngine.AudioMode mode = Spatial8DEngine.AudioMode.MODE_8D;
    private volatile float azimuth = 0.0f;
    private volatile float elevation = 0.0f;
    private volatile float orbitRadius = 1.0f;

    private float smoothAzimuth = 0.0f;
    private float smoothElevation = 0.0f;
    private float smoothDelayL = 0.0f;
    private float smoothDelayR = 0.0f;
    private float smoothVolL = 1.0f;
    private float smoothVolR = 1.0f;
    private float smoothShadowL = 1.0f;
    private float smoothShadowR = 1.0f;
    private float smoothPinna = 1.0f;

    private static final int DELAY_BUFFER_SIZE = 512;
    private final float[] leftDelayLine = new float[DELAY_BUFFER_SIZE];
    private final float[] rightDelayLine = new float[DELAY_BUFFER_SIZE];
    private int delayWriteIndex = 0;

    private float leftLpfState = 0.0f;
    private float rightLpfState = 0.0f;

    public void setSpatialState(Spatial8DEngine.AudioMode mode, float azimuth, float elevation, float radius) {
        this.mode = mode;
        this.azimuth = azimuth;
        this.elevation = elevation;
        this.orbitRadius = Math.max(0.5f, Math.min(2.0f, radius));
    }

    @Override
    public AudioFormat configure(AudioFormat inputAudioFormat) throws UnhandledAudioFormatException {
        if (inputAudioFormat.encoding != androidx.media3.common.C.ENCODING_PCM_16BIT) {
            throw new UnhandledAudioFormatException(inputAudioFormat);
        }
        this.inputAudioFormat = inputAudioFormat;
        this.outputAudioFormat = inputAudioFormat;
        return outputAudioFormat;
    }

    @Override
    public boolean isActive() {
        return inputAudioFormat != AudioFormat.NOT_SET;
    }

    @Override
    public void queueInput(ByteBuffer input) {
        int remaining = input.remaining();
        if (remaining == 0) return;

        if (buffer.capacity() < remaining) {
            buffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder());
        } else {
            buffer.clear();
        }

        if (mode == Spatial8DEngine.AudioMode.OFF || inputAudioFormat.channelCount != 2) {
            buffer.put(input);
            input.position(input.limit());
            buffer.flip();
            outputBuffer = buffer;
            return;
        }

        int sampleRate = inputAudioFormat.sampleRate > 0 ? inputAudioFormat.sampleRate : 44100;
        float maxDelaySamples = (0.00065f * sampleRate);

        while (input.remaining() >= 4) {
            short rawLeft = input.getShort();
            short rawRight = input.getShort();

            float inL = rawLeft / 32768.0f;
            float inR = rawRight / 32768.0f;

            leftDelayLine[delayWriteIndex] = inL;
            rightDelayLine[delayWriteIndex] = inR;

            float outL;
            float outR;

            if (mode == Spatial8DEngine.AudioMode.MODE_3D) {
                smoothAzimuth += (azimuth - smoothAzimuth) * 0.0040f;
                float pan = Math.max(-1.0f, Math.min(1.0f, smoothAzimuth));

                float normPan = (pan + 1.0f) * 0.5f;
                float targetVolL = (float) Math.cos(normPan * Math.PI * 0.5);
                float targetVolR = (float) Math.sin(normPan * Math.PI * 0.5);

                smoothVolL += (targetVolL - smoothVolL) * 0.0035f;
                smoothVolR += (targetVolR - smoothVolR) * 0.0035f;

                float targetDelayL = Math.max(0.0f, pan * maxDelaySamples * 1.15f);
                float targetDelayR = Math.max(0.0f, -pan * maxDelaySamples * 1.15f);

                smoothDelayL += (targetDelayL - smoothDelayL) * 0.0015f;
                smoothDelayR += (targetDelayR - smoothDelayR) * 0.0015f;

                float signalL = readInterpolated(leftDelayLine, delayWriteIndex, smoothDelayL);
                float signalR = readInterpolated(rightDelayLine, delayWriteIndex, smoothDelayR);

                float targetShadowL = Math.max(0.28f, 1.0f - Math.max(0.0f, pan) * 0.65f);
                float targetShadowR = Math.max(0.28f, 1.0f - Math.max(0.0f, -pan) * 0.65f);

                smoothShadowL += (targetShadowL - smoothShadowL) * 0.0025f;
                smoothShadowR += (targetShadowR - smoothShadowR) * 0.0025f;

                leftLpfState += (signalL - leftLpfState) * smoothShadowL;
                rightLpfState += (signalR - rightLpfState) * smoothShadowR;

                float processedL = (leftLpfState * 0.75f + signalL * 0.25f) * smoothVolL;
                float processedR = (rightLpfState * 0.75f + signalR * 0.25f) * smoothVolR;

                outL = (processedL * 1.28f) + (processedR * 0.06f);
                outR = (processedR * 1.28f) + (processedL * 0.06f);

            } else if (mode == Spatial8DEngine.AudioMode.MODE_8D) {
                float angleDiff = azimuth - smoothAzimuth;
                while (angleDiff > (float) Math.PI) angleDiff -= (float) (2.0 * Math.PI);
                while (angleDiff < (float) -Math.PI) angleDiff += (float) (2.0 * Math.PI);
                smoothAzimuth += angleDiff * 0.0035f;

                float sinAz = (float) Math.sin(smoothAzimuth);
                float cosAz = (float) Math.cos(smoothAzimuth);

                float gainL = (1.0f - sinAz) * 0.5f;
                float gainR = (1.0f + sinAz) * 0.5f;

                float targetVolL = (float) Math.sin(gainL * Math.PI * 0.5);
                float targetVolR = (float) Math.sin(gainR * Math.PI * 0.5);

                float frontBackDepth = 0.86f + 0.14f * cosAz;
                targetVolL *= frontBackDepth;
                targetVolR *= frontBackDepth;

                smoothVolL += (targetVolL - smoothVolL) * 0.003f;
                smoothVolR += (targetVolR - smoothVolR) * 0.003f;

                float targetDelayL = Math.max(0.0f, sinAz * maxDelaySamples * 1.10f);
                float targetDelayR = Math.max(0.0f, -sinAz * maxDelaySamples * 1.10f);

                smoothDelayL += (targetDelayL - smoothDelayL) * 0.0012f;
                smoothDelayR += (targetDelayR - smoothDelayR) * 0.0012f;

                float signalL = readInterpolated(leftDelayLine, delayWriteIndex, smoothDelayL);
                float signalR = readInterpolated(rightDelayLine, delayWriteIndex, smoothDelayR);

                float targetShadowL = Math.max(0.32f, 1.0f - Math.max(0.0f, sinAz) * 0.60f);
                float targetShadowR = Math.max(0.32f, 1.0f - Math.max(0.0f, -sinAz) * 0.60f);

                smoothShadowL += (targetShadowL - smoothShadowL) * 0.002f;
                smoothShadowR += (targetShadowR - smoothShadowR) * 0.002f;

                leftLpfState += (signalL - leftLpfState) * smoothShadowL;
                rightLpfState += (signalR - rightLpfState) * smoothShadowR;

                float targetPinna = (cosAz < 0) ? (0.82f + 0.18f * (1.0f + cosAz)) : 1.0f;
                smoothPinna += (targetPinna - smoothPinna) * 0.002f;

                float processedL = (leftLpfState * 0.72f + signalL * 0.28f) * smoothVolL * smoothPinna;
                float processedR = (rightLpfState * 0.72f + signalR * 0.28f) * smoothVolR * smoothPinna;

                outL = (processedL * 1.25f) + (processedR * 0.08f);
                outR = (processedR * 1.25f) + (processedL * 0.08f);

            } else {
                float angleDiff = azimuth - smoothAzimuth;
                while (angleDiff > (float) Math.PI) angleDiff -= (float) (2.0 * Math.PI);
                while (angleDiff < (float) -Math.PI) angleDiff += (float) (2.0 * Math.PI);
                smoothAzimuth += angleDiff * 0.0035f;

                smoothElevation += (elevation - smoothElevation) * 0.0035f;

                float sinAz = (float) Math.sin(smoothAzimuth);
                float cosAz = (float) Math.cos(smoothAzimuth);
                float sinEl = (float) Math.sin(smoothElevation);
                float cosEl = (float) Math.cos(smoothElevation);

                float gainL = (1.0f - sinAz * cosEl) * 0.5f;
                float gainR = (1.0f + sinAz * cosEl) * 0.5f;

                float targetVolL = (float) Math.sin(gainL * Math.PI * 0.5);
                float targetVolR = (float) Math.sin(gainR * Math.PI * 0.5);

                float elevFactor = 0.88f + 0.22f * cosEl;
                targetVolL *= elevFactor;
                targetVolR *= elevFactor;

                smoothVolL += (targetVolL - smoothVolL) * 0.003f;
                smoothVolR += (targetVolR - smoothVolR) * 0.003f;

                float targetDelayL = Math.max(0.0f, (sinAz * cosEl) * maxDelaySamples * 1.15f);
                float targetDelayR = Math.max(0.0f, (-sinAz * cosEl) * maxDelaySamples * 1.15f);

                smoothDelayL += (targetDelayL - smoothDelayL) * 0.0012f;
                smoothDelayR += (targetDelayR - smoothDelayR) * 0.0012f;

                float sigL = readInterpolated(leftDelayLine, delayWriteIndex, smoothDelayL);
                float sigR = readInterpolated(rightDelayLine, delayWriteIndex, smoothDelayR);

                float targetShadowL = Math.max(0.30f, 1.0f - Math.max(0.0f, sinAz) * 0.65f);
                float targetShadowR = Math.max(0.30f, 1.0f - Math.max(0.0f, -sinAz) * 0.65f);

                smoothShadowL += (targetShadowL - smoothShadowL) * 0.002f;
                smoothShadowR += (targetShadowR - smoothShadowR) * 0.002f;

                leftLpfState += (sigL - leftLpfState) * smoothShadowL;
                rightLpfState += (sigR - rightLpfState) * smoothShadowR;

                float targetTopDamp = (sinEl > 0.3f) ? 0.82f : 1.0f;
                smoothPinna += (targetTopDamp - smoothPinna) * 0.002f;

                float procL = (leftLpfState * 0.75f + sigL * 0.25f) * smoothVolL * smoothPinna;
                float procR = (rightLpfState * 0.75f + sigR * 0.25f) * smoothVolR * smoothPinna;

                outL = (procL * 1.30f) + (procR * 0.10f);
                outR = (procR * 1.30f) + (procL * 0.10f);
            }

            delayWriteIndex = (delayWriteIndex + 1) % DELAY_BUFFER_SIZE;

            short sOutL = softClip(outL);
            short sOutR = softClip(outR);

            buffer.putShort(sOutL);
            buffer.putShort(sOutR);
        }

        input.position(input.limit());
        buffer.flip();
        outputBuffer = buffer;
    }

    private float readInterpolated(float[] delayBuffer, int writeIdx, float delaySamples) {
        float readPos = writeIdx - delaySamples;
        while (readPos < 0) readPos += DELAY_BUFFER_SIZE;

        int idx0 = ((int) readPos) % DELAY_BUFFER_SIZE;
        int idx1 = (idx0 + 1) % DELAY_BUFFER_SIZE;
        float frac = readPos - (int) readPos;

        return delayBuffer[idx0] * (1.0f - frac) + delayBuffer[idx1] * frac;
    }

    private short softClip(float x) {
        float out;
        if (x > 1.2f) {
            out = 0.98f;
        } else if (x < -1.2f) {
            out = -0.98f;
        } else {
            out = x - (0.12f * x * x * x);
        }
        int pcm = Math.round(out * 32767.0f);
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, pcm));
    }

    @Override
    public void queueEndOfStream() {
        inputEnded = true;
    }

    @Override
    public ByteBuffer getOutput() {
        ByteBuffer output = outputBuffer;
        outputBuffer = EMPTY_BUFFER;
        return output;
    }

    @Override
    public boolean isEnded() {
        return inputEnded && outputBuffer == EMPTY_BUFFER;
    }

    @Override
    public void flush() {
        outputBuffer = EMPTY_BUFFER;
        inputEnded = false;
        leftLpfState = 0.0f;
        rightLpfState = 0.0f;
    }

    @Override
    public void reset() {
        flush();
        buffer = EMPTY_BUFFER;
        inputAudioFormat = AudioFormat.NOT_SET;
        outputAudioFormat = AudioFormat.NOT_SET;
    }
}
