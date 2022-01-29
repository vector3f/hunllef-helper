package com.hunllefhelper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import javax.sound.sampled.*;
import java.io.*;
import java.util.HashMap;

@Slf4j
public class AudioPlayer {
    private HashMap<String, Clip> clips = new HashMap<String, Clip>();

    public void tryLoadAudio(HunllefHelperConfig config, String[] clipNames) {
        for (String clipName : clipNames) {
            tryLoadClip(config.audioMode(), clipName);
        }
    }

    public void unloadAudio() {
        for (String clipName : clips.keySet()) {
            Clip clip = clips.get(clipName);
            clip.stop();
            clip.flush();
            clip.close();
        }

        clips.clear();
    }

    public void playSoundClip(String sound) {
        if (clips.containsKey(sound)) {
            clips.get(sound).loop(1);
        }
    }

    private boolean tryLoadClip(AudioMode audioMode, String clipName) {
        if (audioMode == AudioMode.Custom) {
            final File customFile = new File(RuneLite.RUNELITE_DIR, clipName);

            try (InputStream fileStream = new BufferedInputStream(new FileInputStream(customFile));
                 AudioInputStream sound = AudioSystem.getAudioInputStream(fileStream)) {
                Clip clip = AudioSystem.getClip();
                clips.put(clipName, clip);
                clip.open(sound);
                clip.setFramePosition(clip.getFrameLength());
                return true;
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | SecurityException ex) {
                log.error("Unable to load sound " + clipName, ex);
            }
        }

        try (
                InputStream audioSource = getClass().getResourceAsStream(clipName);
                BufferedInputStream bufferedStream = new BufferedInputStream(audioSource);
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedStream)) {
            Clip clip = AudioSystem.getClip();
            clips.put(clipName, clip);
            clip.open(audioInputStream);
            clip.setFramePosition(clip.getFrameLength());
            return true;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | SecurityException ex) {
            log.error("Unable to load sound " + clipName, ex);
        }

        return false;
    }
}