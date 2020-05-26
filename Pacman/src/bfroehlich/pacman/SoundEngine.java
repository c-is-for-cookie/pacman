package bfroehlich.pacman;
import java.io.*;
import javax.sound.sampled.*;
import java.net.URL;

/**
 * Class SoundEngine provides functionality to process sound files.
 * Sounds can be loaded, played, paused, resumed, and stopped.
 * 
 * The sound engine can play multiple sounds simultaniously, but only
 * the last sound loaded can be controlled (pased, resumed, etc.). 
 * 
 * The sound engine accepts files in WAV, AIFF, and AU formats (although 
 * not all WAV files - it depends on the encoding in the file).
 * 
 * @author Michael Kolling and David J Barnes 
 * @version 1.0
 */
public class SoundEngine
{
    // the following three fields hold information about the sound clip
    // currently loaded in this engine
    private Clip currentSoundClip = null;
    private int currentSoundDuration = 0;
    private int currentSoundFrameLength = 0;
    private static SoundEngine player;
    private int volume;
    private boolean mute;
    private int prevVol;
    
    /**
     * Create a SoundEngine that can play sound files.
     */
    public SoundEngine()
    {
    }
    
    /**
     * Just initializes the static object if necessary and uses it to
     * play the given sound file. This avoids needing every class that plays
     * sound files to have their own SoundEngine.
     * @param soundFile The file to be loaded
     * @return          whether the load was successful
     */
    public static Float staticPlay(URL soundFile) {
        if(player == null) {
            player = new SoundEngine();
        }
        return player.play(soundFile);
    }
    
    public static Float staticPlay(String path) {
    	URL url = SoundEngine.class.getResource("/" + path);
    	return staticPlay(url);
    }

//	public static void staticPrint() {
//		if(player != null) {
//			System.out.println(player.getDuration());
//		}
//	}
    
    public void mute() {
    	mute = !mute;
    	if(mute) {
    		prevVol = volume;
    		setVolume(0);
    	}
    	else {
    		setVolume(prevVol);
    	}
    }

    /**
     * Load and play a specified sound file. If the file is not in a
     * recognised file format, false is returned. Otherwise the sound
     * is started and true is returned. The method returns immediately
     * after the sound starts (not after the sound finishes).
     * 
     * @param soundFile  The file to be loaded.
     * @return  True, if the sound file could be loaded, false otherwise.
     */
    public Float play(URL soundFile)
    {
    	Float length = loadSound(soundFile);
        if(length != null) {
        	if(mute) {
        		setVolume(0);
        	}
            currentSoundClip.start();
        }
        return length;
    }
    
    public Float play(String path)
    {
    	URL url = getClass().getResource("/" + path);
    	return play(url);
    }

    /**
     * Stop the currently playing sound (if there is one playing). If no 
     * sound is playing, this method has no effect.
     */
    public void stop()
    {
        if(currentSoundClip != null) {
            currentSoundClip.stop();
            currentSoundClip = null;
        }
    }            

    /**
     * Pause the currently playing sound. If no sound is currently playing,
     * calling this method has no effect.
     */
    public void pause()
    {
        if(currentSoundClip != null) {
            currentSoundClip.stop();
        }
    }

    /**
     * Resume the currently paused sound. If no sound is currently paused,
     * this method has no effect.
     */
    public void resume()
    {
        if(currentSoundClip != null) {
            currentSoundClip.start();
        }
    }

    /**
     * Set the current playing position in the currently playing sound to 'value'.
     * 'value' is a percentage value (0 to 100). If there is no sound currently
     * playing, this method has no effect.
     * 
     * @param value  The target position in the sound file, as a percentage.
     */
    public void seek(int value)
    {
        if(currentSoundClip != null) {
            int seekPosition = currentSoundFrameLength / 100 * value;
            currentSoundClip.setFramePosition(seekPosition);
        }
    }
    
    /**
     * Set the playback volume of the current sound. If no sound is currently
     * loaded, this method has no effect.
     * 
     * @param vol  Volume level as a percentage (0..100).
     */
    public void setVolume(int vol)
    {
        if(currentSoundClip == null) {
            return;
        }
        if(vol < 0 || vol > 100) {
            vol = 100;
        }
        
        if(mute && vol>0) {
        	prevVol = vol;
        	return;
        }

        double val = vol / 100.0;

        try {
            FloatControl volControl =
              (FloatControl) currentSoundClip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float)(Math.log(val == 0.0 ? 0.0001 : val) / Math.log(10.0) * 20.0);
            volControl.setValue(dB);
            this.volume = vol;
        } catch (Exception ex) {
            System.out.println("could not set volume");
        }
    }

    /**
     * Return the duration of the currently loaded sound clip.
     * 
     * @return  The duration of the current sound, or 0 if no sound has been loaded.
     */
    public int getDuration()
    {
        return currentSoundDuration;
    }

    /**
     * Load the sound file supplied by the parameter into this sound engine.
     * 
     * @return  True if successful, false if the file could not be decoded.
     */
    private Float loadSound(URL url) 
    {
        currentSoundDuration = 0;

        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(url);
            AudioFormat format = stream.getFormat();

            // we cannot play ALAW/ULAW, so we convert them to PCM
            //
            if ((format.getEncoding() == AudioFormat.Encoding.ULAW) ||
                (format.getEncoding() == AudioFormat.Encoding.ALAW)) 
            {
                AudioFormat tmp = new AudioFormat(
                                          AudioFormat.Encoding.PCM_SIGNED, 
                                          format.getSampleRate(),
                                          format.getSampleSizeInBits() * 2,
                                          format.getChannels(),
                                          format.getFrameSize() * 2,
                                          format.getFrameRate(),
                                          true);
                stream = AudioSystem.getAudioInputStream(tmp, stream);
                format = tmp;
            }
            DataLine.Info info = new DataLine.Info(Clip.class, 
                                           stream.getFormat(),
                                           ((int) stream.getFrameLength() *
                                           format.getFrameSize()));

            currentSoundClip = (Clip) AudioSystem.getLine(info);
            currentSoundClip.open(stream);
            currentSoundFrameLength = (int) stream.getFrameLength();
            float duration = (float) (currentSoundClip.getMicrosecondLength()/1000000.0);
//            Console.staticPrint(url.getPath() + "\nBuffer size: " + currentSoundClip.getBufferSize() + "\nFrame size: " + currentSoundClip.getFormat().getFrameSize() + "\nFrame rate: " + currentSoundClip.getFormat().getFrameRate()
//            		+ "\nFrame length:" + currentSoundClip.getFrameLength() + "\nMicrosecond length:" + currentSoundClip.getMicrosecondLength());
//            float duration = (currentSoundClip.getBufferSize() / 
//                              (currentSoundClip.getFormat().getFrameSize() * 
//                              currentSoundClip.getFormat().getFrameRate()));
//            currentSoundDuration = (int) duration;
//            Console.staticPrint("" + duration);
            return duration;
        } catch (Exception ex) {
        	System.out.println(ex.getClass());
        	System.out.println(ex.getMessage());
            currentSoundClip = null;
            return null;
        }
    }
}