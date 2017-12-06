import java.io.*;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;



public class MyAudioPlayer {

  private final File wavFile;
  private AudioStream pcmStream;
  
  
  public MyAudioPlayer(String wavFileName) {
    this.wavFile = new File(wavFileName);
    this.initialize();
    return;
  }
  
  
  // Reset audio stream to the beginning
  private void initialize() {
    try {
      InputStream inStream = new FileInputStream(this.wavFile);
      this.pcmStream = new AudioStream(inStream);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return;
  }
  
  
  public void close() {
    try {
      this.pcmStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return;
  }
  
  
  public void play() {
    AudioPlayer.player.start(this.pcmStream);
    return;
  }
  
  
  public void stop() {
    AudioPlayer.player.stop(this.pcmStream);
    return;
  }
  
  
  // Skip forward
  public boolean skip(double percentage) {
    if (percentage > 1 || percentage < 0) {
      return false;
    }
    try {
      long totalBytes = this.pcmStream.getLength();
      long bytesToBeSkipped = (long)(totalBytes * percentage);
      while (bytesToBeSkipped > 2) {
        bytesToBeSkipped -= this.pcmStream.skip(bytesToBeSkipped);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return true;
  }
  
  
  public boolean skip(int framesToBeSkipped, int totalFrames) {
    return this.skip((double)framesToBeSkipped / (double)totalFrames);
  }
  
  
  // Skip to anywhere
  public boolean skipTo(double percentage) {
    this.close();
    this.initialize();
    return this.skip(percentage);
  }
  
  
  // Skip to anywhere (frames start from 0)
  public boolean skipTo(int targetFrame, int totalFrames) {
    return this.skipTo((double)targetFrame / (double)totalFrames);
  }

}
