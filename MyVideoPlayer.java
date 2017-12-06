import java.awt.image.BufferedImage;
import java.io.*;



public class MyVideoPlayer {
  
  public static final int IMAGE_WIDTH = 352;
  public static final int IMAGE_HEIGHT = 288;
  public static final int BYTES_PER_FRAME = MyVideoPlayer.IMAGE_WIDTH * MyVideoPlayer.IMAGE_HEIGHT * 3;
  private final File rgbFile;
  private final int totalFrames;
  private FileInputStream rgbStream;
  private int framesRead;
  private byte[] currFrameByteData;
  
  
  public MyVideoPlayer(String rgbFileName) {
    this.rgbFile = new File(rgbFileName);
    this.totalFrames = (int)(rgbFile.length() / MyVideoPlayer.BYTES_PER_FRAME);
    this.initialize();
    return;
  }
  
  
  // Reset video stream to the beginning
  private void initialize() {
    try {
      this.rgbStream = new FileInputStream(this.rgbFile);
      this.framesRead = 0;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return;
  }
  
  
  public void close() {
    try {
      this.rgbStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return;
  }
  
  
  public int getFramesRead() {
    return this.framesRead;
  }
  
  
  public int getTotalFrames() {
    return this.totalFrames;
  }
  
  
  // Read the one frame from frames.
  public BufferedImage read() {
    byte[] frame = new byte[MyVideoPlayer.BYTES_PER_FRAME];
    BufferedImage image = new BufferedImage(MyVideoPlayer.IMAGE_WIDTH, MyVideoPlayer.IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
    try {
      this.rgbStream.read(frame);
      this.framesRead++;
    } catch (IOException e) {
      e.printStackTrace();
    }

    currFrameByteData = frame;

    int pixelPos = 0;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        // byte alpha = 0;
        byte red = frame[pixelPos];
        byte green = frame[pixelPos + image.getHeight() * image.getWidth()];
        byte blue = frame[pixelPos + 2 * image.getHeight() * image.getWidth()];
        int pixel = 0xff000000 | ((red & 0xff) << 16) | ((green & 0xff) << 8) | (blue & 0xff);
        image.setRGB(x, y, pixel);
        pixelPos++;
      }
    }
    return image;
  }
  
  
  public BufferedImage readBiggerFast(int multiple) {
    BufferedImage originalImage = this.read();
    if (multiple > 0) {
      return ImageUtility.fastZoomIn(originalImage, originalImage.getWidth() * multiple, originalImage.getHeight() * multiple);
    } else {
      return originalImage;
    }
  }
  
  
  public BufferedImage readBiggerSlow(int multiple) {
    BufferedImage originalImage = this.read();
    if (multiple > 0) {
      return ImageUtility.bilinearZoomIn(originalImage, originalImage.getWidth() * multiple, originalImage.getHeight() * multiple);
    } else {
      return originalImage;
    }
  }
  
  
  // Skip forward
  public boolean skip(int framesToBeSkipped) {
    int newFramesRead = this.getFramesRead() + framesToBeSkipped;
    if (framesToBeSkipped < 0 || newFramesRead > this.getTotalFrames()) {
      return false;
    }
    try {
      this.rgbStream.skip((long)(MyVideoPlayer.BYTES_PER_FRAME * framesToBeSkipped));
    } catch (IOException e) {
      e.printStackTrace();
    }
    this.framesRead = newFramesRead;
    return true;
  }
  
  
  // Skip to anywhere (frames start from 0)
  public boolean skipTo(int targetFrame) {
    this.close();
    this.initialize();
    return this.skip(targetFrame);
  }

  public byte[] getCurrFrameByteData() {
      return currFrameByteData;
  }
  
}



class ImageUtility {
  
  public static BufferedImage generateBlank() {
    return new BufferedImage(MyVideoPlayer.IMAGE_WIDTH, MyVideoPlayer.IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
  }
  
  
  public static BufferedImage fastZoomIn(BufferedImage inImage, int outWidth, int outHeight) {
    if (inImage == null) {
      return null;
    }
    BufferedImage outImage = new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_INT_RGB);
    double widthScaler = 1.0 * outImage.getWidth() / inImage.getWidth();
    double heightScaler = 1.0 * outImage.getHeight() / inImage.getHeight();
    // Scan from UPPER-LEFT to LOWER-RIGHT.
    for (int y = 0; y < outImage.getHeight(); y++) {
      for (int x = 0; x < outImage.getWidth(); x++) {
        double projectedX = x / widthScaler;
        double projectedY = y / heightScaler;
        int upperLeftX = (int)projectedX;
        int upperLeftY = (int)projectedY;
        // Both x and y start from zero.
        // Repeat the last row and column in case of array index out of bound.
        int upperRightX = upperLeftX + 1 == inImage.getWidth() ? upperLeftX : upperLeftX + 1;
        int upperRightY = upperLeftY;
        int lowerLeftX = upperLeftX;
        int lowerLeftY = upperLeftY + 1 == inImage.getHeight() ? upperLeftY : upperLeftY + 1;
        int lowerRightX = upperRightX;
        int lowerRightY = lowerLeftY;
        double distanceToLeft = projectedX - (double)upperLeftX;
        double distanceToTop = projectedY - (double)upperLeftY;
        int upperLeftRGB = inImage.getRGB(upperLeftX, upperLeftY);
        int upperRightRGB = inImage.getRGB(upperRightX, upperRightY);
        int lowerLeftRGB = inImage.getRGB(lowerLeftX, lowerLeftY);
        int lowerRightRGB = inImage.getRGB(lowerRightX, lowerRightY);
        if (distanceToLeft <= 0.5 && distanceToTop <= 0.5) {
          outImage.setRGB(x, y, upperLeftRGB);
        } else if (distanceToLeft > 0.5 && distanceToTop <= 0.5) {
          outImage.setRGB(x, y, upperRightRGB);
        } else if (distanceToLeft <= 0.5 && distanceToTop > 0.5) {
          outImage.setRGB(x, y, lowerLeftRGB);
        } else if (distanceToLeft > 0.5 && distanceToTop > 0.5) {
          outImage.setRGB(x, y, lowerRightRGB);
        } else {
          System.err.println("[ERROR] It's impossible to be here");
          outImage.setRGB(x, y, upperLeftRGB);
        }
      }
    }
    return outImage;
  }
  
  
  public static BufferedImage bilinearZoomIn(BufferedImage inImage, int outWidth, int outHeight) {
    if (inImage == null) {
      return null;
    }
    BufferedImage outImage = new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_INT_RGB);
    double widthScaler = 1.0 * outImage.getWidth() / inImage.getWidth();
    double heightScaler = 1.0 * outImage.getHeight() / inImage.getHeight();
    // Scan from UPPER-LEFT to LOWER-RIGHT.
    for (int y = 0; y < outImage.getHeight(); y++) {
      for (int x = 0; x < outImage.getWidth(); x++) {
        double projectedX = x / widthScaler;
        double projectedY = y / heightScaler;
        int upperLeftX = (int)projectedX;
        int upperLeftY = (int)projectedY;
        // Both x and y start from zero.
        // Repeat the last row and column in case of array index out of bound.
        int upperRightX = upperLeftX + 1 == inImage.getWidth() ? upperLeftX : upperLeftX + 1;
        int upperRightY = upperLeftY;
        int lowerLeftX = upperLeftX;
        int lowerLeftY = upperLeftY + 1 == inImage.getHeight() ? upperLeftY : upperLeftY + 1;
        int lowerRightX = upperRightX;
        int lowerRightY = lowerLeftY;
        double distanceToLeft = projectedX - (double)upperLeftX;  // i.e. the factor "a"
        double distanceToTop = projectedY - (double)upperLeftY;  // i.e. the factor "b"
        int[] cornerPixels = {inImage.getRGB(upperLeftX, upperLeftY),  // The upper left pixel
          inImage.getRGB(upperRightX, upperRightY),  // The upper right pixel
          inImage.getRGB(lowerLeftX, lowerLeftY),  // The lower left pixel
          inImage.getRGB(lowerRightX, lowerRightY)  // The lower right pixel
        };
        double[] cornerWeights = {
            (1 - distanceToLeft) * (1 - distanceToTop),  // (1 - a) * (1 - b)
            distanceToLeft * (1 - distanceToTop),  // a * (1 - b)
            (1 - distanceToLeft) * distanceToTop,  // (1 - a) * b
            distanceToLeft * distanceToTop  // a * b
        };
        outImage.setRGB(x, y, ImageUtility.interpolatePixel(cornerPixels, cornerWeights));
      }
    }
    return outImage;
  }
  
  
  private static int interpolatePixel(int[] cornerPixels, double[] cornerWeights) {
    // double tempAlpha = 0.0;
    double tempRed = 0.0;
    double tempGreen = 0.0;
    double tempBlue = 0.0;
    for (int i = 0; i < cornerPixels.length; i++) {
      int pixel = cornerPixels[i];
      double weight = cornerWeights[i];
      // int alpha = (pixel & 0xff000000) >>> 24;
      int red = (pixel & 0xff0000) >>> 16;
      int green = (pixel & 0xff00) >>> 8;
      int blue = (pixel & 0xff);
      // tempAlpha += (double)alpha * weight;
      tempRed += (double)red * weight;
      tempGreen += (double)green * weight;
      tempBlue += (double)blue * weight;
    }
    // int newAlpha = (int)Math.round(tempAlpha);
    int newRed = (int)Math.round(tempRed);
    int newGreen = (int)Math.round(tempGreen);
    int newBlue = (int)Math.round(tempBlue);
    // if (newAlpha > 0xff) {newAlpha = 0xff;}
    if (newRed > 0xff) {newRed = 0xff;}
    if (newGreen > 0xff) {newGreen = 0xff;}
    if (newBlue > 0xff) {newBlue = 0xff;}
    // int newPixel = 0x0 | (newAlpha << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
    int newPixel = 0xff000000 | (newRed << 16) | (newGreen << 8) | newBlue;
    return newPixel;
  }
  
}