package com.example.cuoiky_qllichhoctap.util;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Build;

import java.util.Random;

public class PomodoroSoundManager {
    private final Context context;
    private MediaPlayer backgroundAudioPlayer;
    private AudioTrack whiteNoiseTrack;
    private Thread whiteNoiseThread;
    private volatile boolean generatedNoisePlaying = false;
    private boolean muted = false;
    private float volume = 0.7f;
    private String soundName = "tiếng mưa";

    public PomodoroSoundManager(Context context) {
        this.context = context;
    }

    public void play() {
        if (backgroundAudioPlayer == null) {
            try {
                int resId = context.getResources().getIdentifier(rawName(), "raw", context.getPackageName());
                if (resId != 0) {
                    backgroundAudioPlayer = MediaPlayer.create(context, resId);
                    if (backgroundAudioPlayer != null) {
                        backgroundAudioPlayer.setLooping(true);
                        backgroundAudioPlayer.setVolume(volume, volume);
                        backgroundAudioPlayer.start();
                    }
                } else {
                    startGeneratedWhiteNoise();
                }
            } catch (Exception ignored) {
            }
        } else if (!backgroundAudioPlayer.isPlaying()) {
            backgroundAudioPlayer.setVolume(volume, volume);
            backgroundAudioPlayer.start();
        }
    }

    public void pause() {
        if (backgroundAudioPlayer != null && backgroundAudioPlayer.isPlaying()) {
            backgroundAudioPlayer.pause();
        }
        stopGeneratedWhiteNoise();
    }

    public void stop() {
        if (backgroundAudioPlayer != null) {
            backgroundAudioPlayer.stop();
            backgroundAudioPlayer.release();
            backgroundAudioPlayer = null;
        }
        stopGeneratedWhiteNoise();
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
        if (backgroundAudioPlayer != null) {
            backgroundAudioPlayer.setVolume(this.volume, this.volume);
        }
        if (whiteNoiseTrack != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                whiteNoiseTrack.setVolume(this.volume);
            } else {
                whiteNoiseTrack.setStereoVolume(this.volume, this.volume);
            }
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public float getVolume() {
        return volume;
    }

    public String getSoundName() {
        return soundName;
    }

    public void setSoundName(String soundName) {
        this.soundName = soundName;
    }

    public String rawName() {
        if ("tiếng sóng".equals(soundName)) {
            return "song_bien";
        }
        if ("tiếng củi cháy".equals(soundName)) {
            return "tieng_cui";
        }
        if ("tiếng rừng ban đêm".equals(soundName)) {
            return "night_forest";
        }
        if ("tiếng thư viện".equals(soundName)) {
            return "tieng_sach";
        }
        return "tieng_mua";
    }

    public static String subtitle(String name) {
        if ("tiếng sóng".equals(name)) {
            return "Sóng biển";
        }
        if ("tiếng củi cháy".equals(name)) {
            return "Lửa trại";
        }
        if ("tiếng rừng ban đêm".equals(name)) {
            return "Rừng đêm";
        }
        if ("tiếng thư viện".equals(name)) {
            return "Thư viện";
        }
        return "Danh sách phát";
    }

    public static String icon(String name) {
        if ("tiếng sóng".equals(name)) {
            return "≈";
        }
        if ("tiếng củi cháy".equals(name)) {
            return "♨";
        }
        if ("tiếng rừng ban đêm".equals(name)) {
            return "☾";
        }
        if ("tiếng thư viện".equals(name)) {
            return "▤";
        }
        return "☔";
    }

    public static String cardText(String name, String subtitle) {
        return icon(name) + "   " + name + "\n     " + subtitle + "                         ▶";
    }

    private void startGeneratedWhiteNoise() {
        if (generatedNoisePlaying) {
            return;
        }
        generatedNoisePlaying = true;
        int sampleRate = 22050;
        int minBuffer = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = Math.max(minBuffer, sampleRate / 2);
        whiteNoiseTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            whiteNoiseTrack.setVolume(volume);
        } else {
            whiteNoiseTrack.setStereoVolume(volume, volume);
        }
        whiteNoiseTrack.play();
        whiteNoiseThread = new Thread(() -> {
            Random random = new Random();
            short[] buffer = new short[1024];
            int smooth = 0;
            while (generatedNoisePlaying && whiteNoiseTrack != null) {
                for (int i = 0; i < buffer.length; i++) {
                    int raw = random.nextInt(Short.MAX_VALUE) - (Short.MAX_VALUE / 2);
                    if ("tiếng thư viện".equals(soundName)) {
                        smooth = (smooth * 7 + raw) / 8;
                        buffer[i] = (short) (smooth * 0.45f);
                    } else if ("tiếng sóng".equals(soundName)) {
                        smooth = (smooth * 3 + raw) / 4;
                        buffer[i] = (short) (smooth * 0.35f + random.nextInt(900));
                    } else {
                        buffer[i] = (short) (raw * 0.28f);
                    }
                }
                try {
                    whiteNoiseTrack.write(buffer, 0, buffer.length);
                } catch (Exception ignored) {
                    generatedNoisePlaying = false;
                }
            }
        });
        whiteNoiseThread.start();
    }

    private void stopGeneratedWhiteNoise() {
        generatedNoisePlaying = false;
        if (whiteNoiseThread != null) {
            whiteNoiseThread.interrupt();
            whiteNoiseThread = null;
        }
        if (whiteNoiseTrack != null) {
            try {
                whiteNoiseTrack.stop();
            } catch (IllegalStateException ignored) {
            }
            whiteNoiseTrack.release();
            whiteNoiseTrack = null;
        }
    }
}
