import java.awt.image.*;



public class MyTapestry {

  public static final int VIDEO_WIDTH = 352;
  public static final int VIDEO_HEIGHT = 288;
  public static final int FACTOR = 4;
  public static final int BLOCK_SIZE = 7;
  public static final int KEYFRAME_WIDTH = VIDEO_WIDTH / FACTOR;
  public static final int KEYFRAME_HEIGHT = VIDEO_HEIGHT / FACTOR;
  private int width;
  private int height;
  public int numOfScenes;
  private String videoFileName;
  public BufferedImage image;
  public BufferedImage blendedImage;
  private MyVideoPlayer videoPlayer;
  private float frameDist[];
  public int sceneSwitchFrameIndex[];
  private Blender blender;
  
  
  public MyTapestry(String fileName) {
    width = VIDEO_WIDTH / FACTOR;
    height = VIDEO_HEIGHT / FACTOR;
    image = new BufferedImage(width * 8, height, BufferedImage.TYPE_INT_RGB);
    numOfScenes = 0;
    videoFileName = fileName;
    videoPlayer = new MyVideoPlayer(videoFileName);
    frameDist = new float[videoPlayer.getTotalFrames()];
    sceneSwitchFrameIndex = new int[videoPlayer.getTotalFrames()];
    return;
  }
  

  public void generateTapestry(int startFrameIndex, int endFrameIndex, double threshold, int bounding) {

    int outWidth = width;
    int outHeight = height;
    
    // Must reset them!
    numOfScenes = 0;
    frameDist = new float[videoPlayer.getTotalFrames()];
    sceneSwitchFrameIndex = new int[videoPlayer.getTotalFrames()];
    
    int count = 0;
    for (int frameIndex = startFrameIndex; frameIndex < endFrameIndex + 1 - 10; frameIndex += 10) {
      
      byte[] currFrameByteData;
      byte[] nextFrameByteData;

      if (videoPlayer.skipTo(frameIndex)) {
        videoPlayer.read();
        currFrameByteData = videoPlayer.getCurrFrameByteData();
        videoPlayer.skip(9);
        videoPlayer.read();
        nextFrameByteData = videoPlayer.getCurrFrameByteData();
        byte[] currBytes = avgBytes(currFrameByteData, outWidth, outHeight, FACTOR);
        byte[] nextBytes = avgBytes(nextFrameByteData, outWidth, outHeight, FACTOR);
        int index = 0;
        float frameDistance = 0;
        for (int y = 0; y < outHeight - (BLOCK_SIZE - 1); y+=3) {
          for (int x = 0; x < outWidth - (BLOCK_SIZE - 1); x+=3) {
            // Perform calculation on the current 1 block:
            byte[][] blockR = new byte[BLOCK_SIZE][BLOCK_SIZE];
            byte[][] blockG = new byte[BLOCK_SIZE][BLOCK_SIZE];
            byte[][] blockB = new byte[BLOCK_SIZE][BLOCK_SIZE];
            int currBlockPositionIndex = y * outWidth + x;
            // i j are the offsets.
            for (int j = 0; j < BLOCK_SIZE; j++) {
              for (int i = 0; i < BLOCK_SIZE; i++) {
                int blockIndex = outWidth * j + i;
                blockR[i][j] = currBytes[currBlockPositionIndex + blockIndex];
                blockG[i][j] = currBytes[currBlockPositionIndex + blockIndex + outHeight * outWidth];
                blockB[i][j] = currBytes[currBlockPositionIndex + blockIndex + outHeight * outWidth * 2];
              }
            }
            float minDiff = 9999;
            // Brute Force on next frame:
            for (int n = 0; n < outHeight - (BLOCK_SIZE - 1); n+=3) {
              for (int m = 0; m < outWidth - (BLOCK_SIZE - 1); m+=3) {
                // Check with each block on the next frame:
                int blockPositionIndex = n * outWidth + m;
                float diff = 0;
                for (int j = 0; j < BLOCK_SIZE; j++) {
                  for (int i = 0; i < BLOCK_SIZE; i++) {
                    int blockIndex = outWidth * j + i;
                    int nextR = nextBytes[blockPositionIndex + blockIndex] & 0xff;
                    int nextG = nextBytes[blockPositionIndex + blockIndex + outHeight * outWidth] & 0xff;
                    int nextB = nextBytes[blockPositionIndex + blockIndex + outHeight * outWidth * 2] & 0xff;
                    int diffR = nextR - (blockR[i][j] & 0xff);
                    int diffG = nextG - (blockG[i][j] & 0xff);
                    int diffB = nextB - (blockB[i][j] & 0xff);
                    // diff += (float)(diffR + diffG + diffB) / 3;
                    diff += Math.sqrt(diffR*diffR + diffG*diffG + diffB*diffB);
                  }
                }
                diff /= (float)(BLOCK_SIZE * BLOCK_SIZE);
                if (diff < minDiff) {
                  minDiff = diff;
                }
              }
            }
            frameDistance += minDiff;
            // System.out.println("MinDiff = " + minDiff);
            index++;
          }
        }
        frameDistance /= (float)index;
        frameDist[frameIndex] = frameDistance;
        // System.out.println(frameDistance);
        if (frameDistance > threshold) {
          if (numOfScenes < 1 || frameIndex - sceneSwitchFrameIndex[numOfScenes - 1] > bounding) {
            sceneSwitchFrameIndex[numOfScenes] = frameIndex + 10;  // Next frame will be the first frame of a scene.
            numOfScenes++;
            System.out.println("[DEBUG] Keyframe: " + frameIndex + "; Distance: " + frameDistance);
          }
        }
        count++;
      }
    }

    image = new BufferedImage(outWidth * numOfScenes, height, BufferedImage.TYPE_INT_RGB);
    count = 0;
    for (int sceneIndex = 0; sceneIndex < numOfScenes; sceneIndex++) {
      if (videoPlayer.skipTo(sceneSwitchFrameIndex[sceneIndex])) {
        videoPlayer.read();
        byte[] sceneBytes = avgBytes(videoPlayer.getCurrFrameByteData(), outWidth, outHeight, FACTOR);
        int index = 0;
        for (int y = 0; y < outHeight; y++) {
          for (int x = 0; x < outWidth; x++) {
            byte r = sceneBytes[index];
            byte g = sceneBytes[index + outHeight * outWidth];
            byte b = sceneBytes[index + outHeight * outWidth * 2];
            int pixel = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
            image.setRGB(x + count * outWidth, y, pixel);
            index++;
          }
        }
        count++;
      }
    }
    
    blendedImage = null;
    return;
    
  }
  

  public byte[] avgBytes(byte[] origBytes, int OutWidth, int OutHeight, int factor) {
    int width = OutWidth * factor;
    int height = OutHeight * factor;
    byte[] bytes = new byte[3 * OutWidth * OutHeight];
    // getWeight(r, sigma);
    for (int y = 0; y < OutHeight; y++) {
      for (int x = 0; x < OutWidth; x++) {
        if (y == OutHeight - 1 && x == OutWidth - 1)
          break;
        int xmin = x * factor;
        int xmax = (x+1) * factor;
        int ymin = y * factor;
        int ymax = (y+1) * factor;
        int sumr = 0;
        int sumg = 0;
        int sumb = 0;
        int count = 0;
        for (int i = ymin; i < ymax; i++) {
          for (int j = xmin; j < xmax; j++) {
            count++;
            sumr += origBytes[i * width + j] & 0xff;
            sumg += origBytes[i * width + j + width * height] & 0xff;
            sumb += origBytes[i * width + j + 2 * width * height] & 0xff;
          }
        }
        bytes[y * OutWidth + x] = (byte) (sumr / count);
        bytes[y * OutWidth + x + OutWidth * OutHeight] = (byte) (sumg / count);
        bytes[y * OutWidth + x + 2 * OutWidth * OutHeight] = (byte) (sumb / count);
        // System.out.println((sumr / count) + " " + (sumg / count) + " " + (sumb / count));
      }
    }
    return bytes;
  }
  
  
  /* ---- Added by Shiyu He ---- */
  
  public BufferedImage getImageFromBytes(byte[] imgBytes, int width, int height) {
    int pixelPos = 0;
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        // byte alpha = 0;
        byte red = imgBytes[pixelPos];
        byte green = imgBytes[pixelPos + height * width];
        byte blue = imgBytes[pixelPos + 2 * height * width];
        int pixel = 0xff000000 | ((red & 0xff) << 16) | ((green & 0xff) << 8) | (blue & 0xff);
        img.setRGB(x, y, pixel);
        pixelPos++;
      }
    }
    return img;
  }
  
  
  public void blend(boolean isSeamCarving) {
    int outWidth = VIDEO_WIDTH / FACTOR;
    int outHeight = VIDEO_HEIGHT / FACTOR;
    this.blender = new Blender(this.image, this.numOfScenes, this.sceneSwitchFrameIndex, outWidth, outHeight);
    if (isSeamCarving) {
      this.blender.seamCarvingBlend();
    } else {
      this.blender.alphaBlend();
    }
    this.blendedImage = this.blender.getBlend();
    return;
  }
  
  
  public BufferedImage getTapestryImage() {
    return this.blendedImage;
  }
  
  
  public int getFrameIdx(int x, int y) {
    int[][] keyframeDict = this.blender.getTapestryDict();
    return keyframeDict[y][x];
  }
  
  /* ---- Added by Shiyu He ---- */

}
