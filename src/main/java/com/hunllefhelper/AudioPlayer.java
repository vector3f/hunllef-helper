package com.hunllefhelper;

import com.hunllefhelper.config.AudioMode;
import com.hunllefhelper.config.HunllefHelperConfig;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import javax.sound.sampled.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class AudioPlayer
{
	private final Map<String, byte[]> data = new HashMap<>(4);
	private float volume = 1f;

	public void tryLoadAudio(HunllefHelperConfig config, String[] clipNames)
	{
		if (config.audioMode() == AudioMode.Disabled)
		{
			return;
		}

		for (String clipName : clipNames)
		{
			tryLoadClip(config.audioMode(), clipName);
		}
	}

	public void unloadAudio()
	{
		data.clear();
	}

	public synchronized void playSoundClip(String sound)
	{
		if (data.containsKey(sound))
		{
			try {
				Clip clip = AudioSystem.getClip();
				byte[] bytes = data.get(sound);
				AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes));
				clip.open(audioInputStream);
				setClipVolume(clip);
				clip.setFramePosition(0);
				clip.start();
				CountDownLatch latch = new CountDownLatch(1);
				clip.addLineListener(e -> {
					if (e.getType() == LineEvent.Type.STOP) {
						latch.countDown();
					}
				});
				latch.await();
				clip.stop();
				clip.flush();
				clip.close();
			}
			catch (IOException | NullPointerException | SecurityException | UnsupportedAudioFileException |
				   LineUnavailableException | InterruptedException ex)
			{
				log.error("Unable to load sound " + sound, ex);
			}
		}
	}

	public void setVolume(int volume)
	{
		float volumeF = volume / 100f;
		volumeF = Math.max(volumeF, 0f);
		volumeF = Math.min(volumeF, 2f);

		this.volume = volumeF;
	}

	private boolean tryLoadClip(AudioMode audioMode, String clipName)
	{
		try (
				InputStream stream = getAppropriateInputStream(clipName, audioMode))
		{
			data.put(clipName, readAllBytes(stream));
			return true;
		}
		catch (IOException | NullPointerException | SecurityException ex)
		{
			log.error("Unable to load sound " + clipName, ex);
		}
		return false;
	}

	private byte[] readAllBytes(InputStream inputStream) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[4];
		while ((nRead = inputStream.read(data, 0, data.length)) != -1)
		{
			buffer.write(data, 0, nRead);
		}
		buffer.flush();
		return buffer.toByteArray();
	}

	private InputStream getAppropriateInputStream(String clipName, AudioMode mode) throws FileNotFoundException
	{
		if (mode == AudioMode.Custom)
		{
			final File customFile = new File(RuneLite.RUNELITE_DIR, clipName);
			return new BufferedInputStream(new FileInputStream(customFile));
		}
		else {
			return getClass().getResourceAsStream(clipName);
		}
	}

	private void setClipVolume(Clip clip)
	{
		FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		gainControl.setValue(20f * (float) Math.log10(volume));
	}
}
