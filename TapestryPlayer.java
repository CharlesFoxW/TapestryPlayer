import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.concurrent.TimeUnit;
import javax.swing.*;



public class TapestryPlayer {
  
  public static final int FPS = 20;
  public static final int REFRESH_RATE = 10;  // Milliseconds
  public static final int DURATION_PER_FRAME_MSEC = 1000 / TapestryPlayer.FPS;
  public static final int VIDEO_LENGTH_SEC = 300;
  public static final double SCALING_FACTOR = 2.0;
  // private final BufferedImage tapestry;
  private final MyAudioPlayer audio;
  private final MyVideoPlayer video;
  private final MyTapestry tapestry;
  private long timeStamp;
  private int framesSent;
  private boolean isPlaying;
  private BufferedImage currFrame;
  private Timer timer;
  private JFrame player;
  private JPanel buttonPanel;
  private JPanel framePanel;
  private JPanel tapestryPanel;
  private JLabel frameLabel;
  private JLabel tapestryLabel;
  private JButton buttonPlayOrPause;
  private JButton buttonStop;
  

  public static void main(String[] args) {
    
    Arguments argObj = new Arguments(args);
    TapestryPlayer myPlayer = new TapestryPlayer(argObj);
    myPlayer.nonsense();
    return;
  }
  
  
  private static ImageIcon scaleIcon(ImageIcon originalIcon, double scaleFactor) {
    if (originalIcon == null || scaleFactor <= 0 || scaleFactor == 1.0) {
      return originalIcon;
    } else {
      int scaledIconWidth = (int)(originalIcon.getIconWidth() * scaleFactor);
      int scaledIconHeight = (int)(originalIcon.getIconHeight() * scaleFactor);
      return new ImageIcon(originalIcon.getImage().getScaledInstance(scaledIconWidth, scaledIconHeight, Image.SCALE_SMOOTH));
    }
  }
  
  
  private class FrameUpdater implements ActionListener {
    
    @Override
    public void actionPerformed(ActionEvent e) {
      long delay = System.currentTimeMillis() - TapestryPlayer.this.timeStamp;
      double framesElapsed = (double)delay / (double)TapestryPlayer.DURATION_PER_FRAME_MSEC;
      int missedFrames = (int)(framesElapsed - TapestryPlayer.this.framesSent);
      if (TapestryPlayer.this.framesSent == 0) {
        missedFrames++;
      }
      if (missedFrames < 1) {  // Current frame is already sent
        return;
      } else if (missedFrames < 2) {  // It's time to send current/next frame
        this.sendOneFrame();
        TapestryPlayer.this.framesSent++;
      } else {
        TapestryPlayer.this.video.skip(missedFrames - 1);
        this.sendOneFrame();
        TapestryPlayer.this.framesSent += missedFrames;
      }
      return;  // Update time stamp and return
    }
    
    private void sendOneFrame() {
      BufferedImage nextFrame = TapestryPlayer.this.video.read();
      TapestryPlayer.this.currFrame = nextFrame;
      ImageIcon nextIcon = new ImageIcon(nextFrame);
      TapestryPlayer.this.frameLabel.setIcon(TapestryPlayer.scaleIcon(nextIcon, TapestryPlayer.SCALING_FACTOR));
      return;
    }
    
  }
  
  
  private class ButtonHandler implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == TapestryPlayer.this.buttonPlayOrPause) {
        TapestryPlayer.this.playOrPause();
      } else if (e.getSource() == TapestryPlayer.this.buttonStop) {
        TapestryPlayer.this.stop();
      }
      return;
    }
  }
  
  
  private class ClickHandler implements MouseListener {
    @Override
    public void mouseClicked(MouseEvent e) {
      System.out.println("[DEBUG] Mouse clicked");
      // TODO Auto-generated method stub
      return;
    }
    @Override
    public void mousePressed(MouseEvent e) {
      return;
    }
    @Override
    public void mouseReleased(MouseEvent e) {
      return;
    }
    @Override
    public void mouseEntered(MouseEvent e) {
      return;
    }
    @Override
    public void mouseExited(MouseEvent e) {
      return;
    }
  }
  
  
  public TapestryPlayer(Arguments argObj) {
    
    this.isPlaying = false;
    this.audio = new MyAudioPlayer(argObj.getAudioPath());
    this.video = new MyVideoPlayer(argObj.getVideoPath());
    this.tapestry = new MyTapestry(argObj.getVideoPath());
    this.tapestry.generateTapestry();
    this.timer = new Timer(TapestryPlayer.REFRESH_RATE, this.new FrameUpdater());
    
    this.player = new JFrame("CSCI-576 MyTapestry Player: Shiyu He, Yihao Wang");
    GridBagLayout layout = new GridBagLayout();
    this.player.getContentPane().setLayout(layout);
    this.player.getContentPane().setBackground(new Color(25, 25, 25));
    
    Font fontRegular = new Font("Arial", Font.PLAIN, 24);
    Font fontBold = new Font("Arial", Font.BOLD, 20);
    Font fontTitle = new Font("Arial", Font.BOLD, 30);
    
    JLabel titleLabel = new JLabel("CSCI-576 MyTapestry Player: Shiyu He, Yihao Wang");
    titleLabel.setFont(fontTitle);
    titleLabel.setForeground(new Color(150, 150, 150));
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    titleLabel.setVerticalAlignment(SwingConstants.TOP);
    JLabel videoLabel = new JLabel("Video: " + argObj.getVideoPath());
    videoLabel.setFont(fontRegular);
    videoLabel.setForeground(new Color(150, 150, 150));
    videoLabel.setHorizontalAlignment(SwingConstants.CENTER);
    videoLabel.setVerticalAlignment(SwingConstants.TOP);
    JLabel audioLabel = new JLabel("Audio: " + argObj.getAudioPath());
    audioLabel.setFont(fontRegular);
    audioLabel.setForeground(new Color(150, 150, 150));
    audioLabel.setHorizontalAlignment(SwingConstants.CENTER);
    audioLabel.setVerticalAlignment(SwingConstants.TOP);
    /*
    JLabel tapestryLabel = new JLabel("MyTapestry: " + argObj.getTapestryPath());
    tapestryLabel.setFont(fontRegular);
    tapestryLabel.setHorizontalAlignment(SwingConstants.CENTER);
    tapestryLabel.setVerticalAlignment(SwingConstants.TOP);
    */
    
    this.currFrame = ImageUtility.generateBlank();
    ImageIcon blankIcon = TapestryPlayer.scaleIcon(new ImageIcon(this.currFrame), TapestryPlayer.SCALING_FACTOR);
    this.frameLabel = new JLabel(blankIcon);
    this.framePanel = new JPanel();
    this.framePanel.setBackground(new Color(25, 25, 25));
    this.framePanel.add(frameLabel);

    ImageIcon tapestryIcon = new ImageIcon(tapestry.getTapestryImage());
    JLabel tapestryLabel = new JLabel("", tapestryIcon, JLabel.CENTER);
    this.tapestryPanel = new JPanel();
    this.tapestryPanel.setBackground(new Color(25, 25, 25));
    this.tapestryPanel.add(tapestryLabel);
    
    this.buttonPlayOrPause = new JButton("PLAY");
    this.buttonPlayOrPause.addActionListener(this.new ButtonHandler());
    this.buttonPlayOrPause.setFont(fontBold);
    this.buttonPlayOrPause.setForeground(new Color(195, 195, 195));
    this.buttonPlayOrPause.setBackground(new Color(56, 56, 56));
    this.buttonPlayOrPause.setOpaque(true);
    this.buttonPlayOrPause.setBorderPainted(false);
    this.buttonPlayOrPause.setEnabled(true);
    this.buttonStop = new JButton("STOP");
    this.buttonStop.addActionListener(this.new ButtonHandler());
    this.buttonStop.setFont(fontBold);
    this.buttonStop.setForeground(new Color(195, 195, 195));
    this.buttonStop.setBackground(new Color(56, 56, 56));
    this.buttonStop.setOpaque(true);
    this.buttonStop.setBorderPainted(false);
    this.buttonStop.setEnabled(false);
    this.buttonPanel = new JPanel();
    this.buttonPanel.setBackground(new Color(25, 25, 25));
    this.buttonPanel.add(this.buttonPlayOrPause);
    this.buttonPanel.add(this.buttonStop);
    
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.CENTER;
    constraints.weightx = 0.5;
    constraints.gridx = 0;
    constraints.gridy = 0;
    this.player.getContentPane().add(titleLabel, constraints);
    constraints.gridy++;
    this.player.getContentPane().add(videoLabel, constraints);
    constraints.gridy++;
    this.player.getContentPane().add(audioLabel, constraints);
    constraints.gridy++;
    //this.player.getContentPane().add(tapestryLabel, constraints);
    //constraints.gridy++;
    this.player.getContentPane().add(this.framePanel, constraints);
    constraints.gridx++;
    this.player.getContentPane().add(this.tapestryPanel, constraints);
    constraints.gridx--;
    constraints.gridy++;
    this.player.getContentPane().add(this.buttonPanel, constraints);
    
    this.player.pack();
    this.player.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.player.setVisible(true);
    
    return;
    
  }
  
  
  // Play/Resume the video from where it pauses
  private void play() {
    if (this.isPlaying) {
      return;
    }
    this.audio.skipTo(this.video.getFramesRead(), this.video.getTotalFrames());
    this.audio.play();  // Bug in sun.audio.AudioPlayer: It does not stop streaming upon stop()
    this.timeStamp = System.currentTimeMillis();
    this.framesSent = 0;
    this.timer.start();  // Resume video playing
    this.isPlaying = true;
    return;
  }
  
  
  public void stop() {
    this.buttonPlayOrPause.setText("PLAY");
    if (this.isPlaying) {  // Playing
      this.timer.stop();
      this.audio.stop();
      this.video.skipTo(0);
      this.audio.skipTo(0.0);
      this.currFrame = ImageUtility.generateBlank();
      ImageIcon blankIcon = TapestryPlayer.scaleIcon(new ImageIcon(this.currFrame), TapestryPlayer.SCALING_FACTOR);
      this.frameLabel.setIcon(blankIcon);
      this.isPlaying = false;
      this.buttonStop.setEnabled(false);
      return;
    } else if (this.video.getFramesRead() > 0) {  // Paused
      this.video.skipTo(0);
      this.audio.skipTo(0.0);
      this.currFrame = ImageUtility.generateBlank();
      ImageIcon blankIcon = TapestryPlayer.scaleIcon(new ImageIcon(this.currFrame), TapestryPlayer.SCALING_FACTOR);
      this.frameLabel.setIcon(blankIcon);
      this.buttonStop.setEnabled(false);
      return;
    } else {  // Stopped
      return;
    }
  }
  
  
  private void pause() {
    if (!this.isPlaying) {
      return;
    }
    this.timer.stop();
    this.audio.stop();
    this.isPlaying = false;
    return;
  }
  
  
  public void playOrPause() {
    if (this.isPlaying) {
      this.pause();
      this.buttonPlayOrPause.setText("PLAY");
    } else {
      this.play();
      this.buttonPlayOrPause.setText("PAUSE");
    }
    this.buttonStop.setEnabled(true);
    return;
  }
  
  
  // Skip forward
  public void skip(int framesToBeSkipped) {
    this.pause();
    this.video.skip(framesToBeSkipped);
    this.audio.skip(framesToBeSkipped, this.video.getTotalFrames());
    this.play();
    return;
  }
  
  
  // Skip to anywhere (frames start from 0)
  public void skipTo(int targetFrame) {
    this.pause();
    this.video.skipTo(targetFrame);
    this.audio.skipTo(targetFrame, this.video.getTotalFrames());
    this.play();
    return;
  }
  
  
  public void nonsense() {
    return;
  }
  
}



class Arguments {
  
  private final String videoPath;
  private final String audioPath;
  private final String tapestryPath;
  
  public Arguments(String args[]) {
    if (args.length < 3) {
      System.out.println("Usage: java -jar TapestryPlayer.jar <Video> <Audio> <MyTapestry>");
      System.exit(1);
    }
    this.videoPath = args[0];
    this.audioPath = args[1];
    this.tapestryPath = args[2];
    return;
  }
  
  public String getVideoPath() {
    return this.videoPath;
  }
  
  public String getAudioPath() {
    return this.audioPath;
  }
  
  //public String getTapestryPath() {
    //return this.tapestryPath;
  //}
  
}
