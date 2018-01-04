import java.awt.image.BufferedImage;



public class Blender {
  
  public static final int OVERLAP_PERCENTAGE = 25;
  private final double overlapFactor = Blender.OVERLAP_PERCENTAGE / 100.0;
  private final int overlapWidth;
  private final int singleNonOverlapWidth;
  private final int doubleNonOverlapWidth;
  private final BufferedImage[] keyframes;
  private final int width;
  private final int height;
  private final int[] keyframeMap;
  private BufferedImage blendedTapestry;
  private int[][] blendedTapestryMap;
  
  
  public Blender(BufferedImage keyframesInOne, int numKeyframes, int[] keyframeIdx, int frameWidth, int frameHeight) {
    
    this.keyframes = new BufferedImage[numKeyframes];
    this.keyframeMap = keyframeIdx;
    this.width = frameWidth;
    this.height = frameHeight;
    
    this.overlapWidth = (int)(this.width * this.overlapFactor);
    this.singleNonOverlapWidth = this.width - this.overlapWidth;
    this.doubleNonOverlapWidth = this.width - this.overlapWidth - this.overlapWidth;
    
    for (int i = 0; i < numKeyframes; i++) {
      this.keyframes[i] = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
      int offsetX = this.width * i;
      for (int y = 0; y < this.height; y++) {
        for (int x = 0; x < this.width; x++) {
          int globalX = offsetX + x;
          int pixel = keyframesInOne.getRGB(globalX, y);
          this.keyframes[i].setRGB(x, y, pixel);
        }
      }
    }
    
    return;
    
  }
  
  
  public BufferedImage getBlend() {
    return this.blendedTapestry;
  }
  
  
  public int[][] getTapestryDict() {
    return this.blendedTapestryMap;
  }
  
  
  public void alphaBlend() {
    
    if (this.keyframes.length < 2) {
      this.blendedTapestry = this.keyframes[0];
      this.blendedTapestryMap = new int[this.height][this.width];
      for (int y = 0; y < this.height; y++) {
        for (int x = 0; x < this.width; x++) {
          this.blendedTapestryMap[y][x] = this.keyframeMap[0];
        }
      }
      return;
    }
    if (this.keyframes.length < 3) {
      // First frame + last frame
      int tapestryWidth = this.width + this.singleNonOverlapWidth;
      this.blendedTapestry = new BufferedImage(tapestryWidth, this.height, BufferedImage.TYPE_INT_RGB);
      this.blendedTapestryMap = new int[this.height][tapestryWidth];
      // Current coordinate x to be filled in the tapestry
      int tapestryX = 0;
      int frameIndexA = this.keyframeMap[0];  // Initially set to the first frame
      // Process the first frame
      for (int y = 0; y < this.height; y++) {
        for (int x = 0; x < this.singleNonOverlapWidth; x++) {
          int globalX = x + tapestryX;
          this.blendedTapestry.setRGB(globalX, y, this.keyframes[0].getRGB(x, y));
          this.blendedTapestryMap[y][globalX] = frameIndexA;
        }
      }
      tapestryX += this.singleNonOverlapWidth;
      BufferedImage overlapA = this.getSubarea(this.keyframes[0], this.singleNonOverlapWidth, this.width, this.height);
      // Process the last frame
      int frameIndexB = this.keyframeMap[1];
      BufferedImage lastFrame = this.keyframes[1];
      BufferedImage overlapB = this.getSubarea(lastFrame, 0, this.overlapWidth, this.height);
      BufferedImage blendedOverlap = alphaBlendOverlaps(overlapA, overlapB);
      for (int y = 0; y < blendedOverlap.getHeight(); y++) {
        for (int x = 0; x < blendedOverlap.getWidth(); x++) {
          int globalX = x + tapestryX;
          this.blendedTapestry.setRGB(globalX, y, blendedOverlap.getRGB(x, y));
          if (x < blendedOverlap.getWidth() / 2) {
            this.blendedTapestryMap[y][globalX] = frameIndexA;
          } else {
            this.blendedTapestryMap[y][globalX] = frameIndexB;
          }
        }
      }
      tapestryX += this.overlapWidth;
      for (int y = 0; y < lastFrame.getHeight(); y++) {
        for (int x = 0; x < this.singleNonOverlapWidth; x++) {
          int nonOverlapX = x + this.overlapWidth;
          int globalX = x + tapestryX;
          this.blendedTapestry.setRGB(globalX, y, lastFrame.getRGB(nonOverlapX, y));
          this.blendedTapestryMap[y][globalX] = frameIndexB;
        }
      }
      return;
    }
    
    // First frame + key frames + last frame
    int tapestryWidth = this.width + (this.keyframes.length - 1) * this.singleNonOverlapWidth;
    this.blendedTapestry = new BufferedImage(tapestryWidth, this.height, BufferedImage.TYPE_INT_RGB);
    this.blendedTapestryMap = new int[this.height][tapestryWidth];
    
    // Current coordinate x to be filled in the tapestry
    int tapestryX = 0;
    int frameIndexA = this.keyframeMap[0];  // Initially set to the first frame
    
    // Process the first frame
    for (int y = 0; y < this.height; y++) {
      for (int x = 0; x < this.singleNonOverlapWidth; x++) {
        int globalX = x + tapestryX;
        this.blendedTapestry.setRGB(globalX, y, this.keyframes[0].getRGB(x, y));
        this.blendedTapestryMap[y][globalX] = frameIndexA;
      }
    }
    tapestryX += this.singleNonOverlapWidth;
    BufferedImage overlapA = this.getSubarea(this.keyframes[0], this.singleNonOverlapWidth, this.width, this.height);
    
    // Process key frames
    for (int i = 1; i < this.keyframes.length - 1; i++) {
      int frameIndexB = this.keyframeMap[i];
      BufferedImage keyframe = this.keyframes[i];
      BufferedImage overlapB = this.getSubarea(keyframe, 0, this.overlapWidth, this.height);
      BufferedImage blendedOverlap = alphaBlendOverlaps(overlapA, overlapB);
      for (int y = 0; y < blendedOverlap.getHeight(); y++) {
        for (int x = 0; x < blendedOverlap.getWidth(); x++) {
          int globalX = x + tapestryX;
          this.blendedTapestry.setRGB(globalX, y, blendedOverlap.getRGB(x, y));
          if (x < blendedOverlap.getWidth() / 2) {
            this.blendedTapestryMap[y][globalX] = frameIndexA;
          } else {
            this.blendedTapestryMap[y][globalX] = frameIndexB;
          }
        }
      }
      tapestryX += this.overlapWidth;
      for (int y = 0; y < keyframe.getHeight(); y++) {
        for (int x = 0; x < this.doubleNonOverlapWidth; x++) {
          int nonOverlapX = x + this.overlapWidth;
          int globalX = x + tapestryX;
          this.blendedTapestry.setRGB(globalX, y, keyframe.getRGB(nonOverlapX, y));
          this.blendedTapestryMap[y][globalX] = frameIndexB;
        }
      }
      tapestryX += this.doubleNonOverlapWidth;
      frameIndexA = frameIndexB;
      overlapA = this.getSubarea(keyframe, this.singleNonOverlapWidth, this.width, this.height);
    }
    
    // Process the last frame
    int frameIndexB = this.keyframeMap[this.keyframes.length - 1];
    BufferedImage lastFrame = this.keyframes[this.keyframes.length - 1];
    BufferedImage overlapB = this.getSubarea(lastFrame, 0, this.overlapWidth, this.height);
    BufferedImage blendedOverlap = alphaBlendOverlaps(overlapA, overlapB);
    for (int y = 0; y < blendedOverlap.getHeight(); y++) {
      for (int x = 0; x < blendedOverlap.getWidth(); x++) {
        int globalX = x + tapestryX;
        this.blendedTapestry.setRGB(globalX, y, blendedOverlap.getRGB(x, y));
        if (x < blendedOverlap.getWidth() / 2) {
          this.blendedTapestryMap[y][globalX] = frameIndexA;
        } else {
          this.blendedTapestryMap[y][globalX] = frameIndexB;
        }
      }
    }
    tapestryX += this.overlapWidth;
    for (int y = 0; y < lastFrame.getHeight(); y++) {
      for (int x = 0; x < this.singleNonOverlapWidth; x++) {
        int nonOverlapX = x + this.overlapWidth;
        int globalX = x + tapestryX;
        this.blendedTapestry.setRGB(globalX, y, lastFrame.getRGB(nonOverlapX, y));
        this.blendedTapestryMap[y][globalX] = frameIndexB;
      }
    }
    
    return;
    
  }
  
  
  public void seamCarvingBlend() {
    
    if (this.keyframes.length < 2) {
      this.blendedTapestry = this.keyframes[0];
      this.blendedTapestryMap = new int[this.height][this.width];
      for (int y = 0; y < this.height; y++) {
        for (int x = 0; x < this.width; x++) {
          this.blendedTapestryMap[y][x] = this.keyframeMap[0];
        }
      }
      return;
    }
    if (this.keyframes.length < 3) {
      // First frame + last frame
      int tapestryWidth = 2 * this.width;
      this.blendedTapestry = new BufferedImage(tapestryWidth, this.height, BufferedImage.TYPE_INT_RGB);
      this.blendedTapestryMap = new int[this.height][tapestryWidth];
      // Current coordinate x to be filled in the tapestry
      int tapestryX = 0;
      int frameIndexA = this.keyframeMap[0];  // Initially set to the first frame
      // Process the first frame
      for (int y = 0; y < this.height; y++) {
        for (int x = 0; x < this.singleNonOverlapWidth; x++) {
          int globalX = x + tapestryX;
          this.blendedTapestry.setRGB(globalX, y, this.keyframes[0].getRGB(x, y));
          this.blendedTapestryMap[y][globalX] = frameIndexA;
        }
      }
      tapestryX += this.singleNonOverlapWidth;
      BufferedImage overlapA = this.getSubarea(this.keyframes[0], this.singleNonOverlapWidth, this.width, this.height);
      // Process the last frame
      int frameIndexB = this.keyframeMap[1];
      BufferedImage lastFrame = this.keyframes[1];
      BufferedImage overlapB = this.getSubarea(lastFrame, 0, this.overlapWidth, this.height);
      BufferedImage blendedOverlap = seamCarvingBlendOverlaps(overlapA, overlapB);
      for (int y = 0; y < blendedOverlap.getHeight(); y++) {
        for (int x = 0; x < blendedOverlap.getWidth(); x++) {
          int globalX = x + tapestryX;
          this.blendedTapestry.setRGB(globalX, y, blendedOverlap.getRGB(x, y));
          if (x < blendedOverlap.getWidth() / 2) {
            this.blendedTapestryMap[y][globalX] = frameIndexA;
          } else {
            this.blendedTapestryMap[y][globalX] = frameIndexB;
          }
        }
      }
      tapestryX += blendedOverlap.getWidth();
      for (int y = 0; y < lastFrame.getHeight(); y++) {
        for (int x = 0; x < this.singleNonOverlapWidth; x++) {
          int nonOverlapX = x + this.overlapWidth;
          int globalX = x + tapestryX;
          this.blendedTapestry.setRGB(globalX, y, lastFrame.getRGB(nonOverlapX, y));
          this.blendedTapestryMap[y][globalX] = frameIndexB;
        }
      }
      return;
    }
    
    // First frame + key frames + last frame
    int tapestryWidth = this.keyframes.length * this.width;
    this.blendedTapestry = new BufferedImage(tapestryWidth, this.height, BufferedImage.TYPE_INT_RGB);
    this.blendedTapestryMap = new int[this.height][tapestryWidth];
    
    // Current coordinate x to be filled in the tapestry
    int tapestryX = 0;
    int frameIndexA = this.keyframeMap[0];  // Initially set to the first frame
    
    // Process the first frame
    for (int y = 0; y < this.height; y++) {
      for (int x = 0; x < this.singleNonOverlapWidth; x++) {
        int globalX = x + tapestryX;
        this.blendedTapestry.setRGB(globalX, y, this.keyframes[0].getRGB(x, y));
        this.blendedTapestryMap[y][globalX] = frameIndexA;
      }
    }
    tapestryX += this.singleNonOverlapWidth;
    BufferedImage overlapA = this.getSubarea(this.keyframes[0], this.singleNonOverlapWidth, this.width, this.height);
    
    // Process key frames
    for (int i = 1; i < this.keyframes.length - 1; i++) {
      int frameIndexB = this.keyframeMap[i];
      BufferedImage keyframe = this.keyframes[i];
      BufferedImage overlapB = this.getSubarea(keyframe, 0, this.overlapWidth, this.height);
      BufferedImage blendedOverlap = seamCarvingBlendOverlaps(overlapA, overlapB);
      for (int y = 0; y < blendedOverlap.getHeight(); y++) {
        for (int x = 0; x < blendedOverlap.getWidth(); x++) {
          int globalX = x + tapestryX;
          this.blendedTapestry.setRGB(globalX, y, blendedOverlap.getRGB(x, y));
          if (x < blendedOverlap.getWidth() / 2) {
            this.blendedTapestryMap[y][globalX] = frameIndexA;
          } else {
            this.blendedTapestryMap[y][globalX] = frameIndexB;
          }
        }
      }
      tapestryX += blendedOverlap.getWidth();
      for (int y = 0; y < keyframe.getHeight(); y++) {
        for (int x = 0; x < this.doubleNonOverlapWidth; x++) {
          int nonOverlapX = x + this.overlapWidth;
          int globalX = x + tapestryX;
          this.blendedTapestry.setRGB(globalX, y, keyframe.getRGB(nonOverlapX, y));
          this.blendedTapestryMap[y][globalX] = frameIndexB;
        }
      }
      tapestryX += this.doubleNonOverlapWidth;
      frameIndexA = frameIndexB;
      overlapA = this.getSubarea(keyframe, this.singleNonOverlapWidth, this.width, this.height);
    }
    
    // Process the last frame
    int frameIndexB = this.keyframeMap[this.keyframes.length - 1];
    BufferedImage lastFrame = this.keyframes[this.keyframes.length - 1];
    BufferedImage overlapB = this.getSubarea(lastFrame, 0, this.overlapWidth, this.height);
    BufferedImage blendedOverlap = seamCarvingBlendOverlaps(overlapA, overlapB);
    for (int y = 0; y < blendedOverlap.getHeight(); y++) {
      for (int x = 0; x < blendedOverlap.getWidth(); x++) {
        int globalX = x + tapestryX;
        this.blendedTapestry.setRGB(globalX, y, blendedOverlap.getRGB(x, y));
        if (x < blendedOverlap.getWidth() / 2) {
          this.blendedTapestryMap[y][globalX] = frameIndexA;
        } else {
          this.blendedTapestryMap[y][globalX] = frameIndexB;
        }
      }
    }
    tapestryX += blendedOverlap.getWidth();
    for (int y = 0; y < lastFrame.getHeight(); y++) {
      for (int x = 0; x < this.singleNonOverlapWidth; x++) {
        int nonOverlapX = x + this.overlapWidth;
        int globalX = x + tapestryX;
        this.blendedTapestry.setRGB(globalX, y, lastFrame.getRGB(nonOverlapX, y));
        this.blendedTapestryMap[y][globalX] = frameIndexB;
      }
    }
    
    return;
    
  }
  
  
  // Range: [start, end)
  private BufferedImage getSubarea(BufferedImage source, int start, int end, int height) {
    int subWidth = end - start;
    BufferedImage subarea = new BufferedImage(subWidth, height, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < subarea.getHeight(); y++) {
      for (int x = 0; x < subarea.getWidth(); x++) {
        int offsetX = x + start;
        subarea.setRGB(x, y, source.getRGB(offsetX, y));
      }
    }
    return subarea;
  }
  
  
  // A and B must have the same width and height
  private BufferedImage alphaBlendOverlaps(BufferedImage overlapA, BufferedImage overlapB) {
    BufferedImage blended = new BufferedImage(overlapA.getWidth(), overlapA.getHeight(), BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < blended.getHeight(); y++) {
      for (int x = 0; x < blended.getWidth(); x++) {
        double alpha = (double)x / (double)blended.getWidth();
        int pixelA = overlapA.getRGB(x, y);
        int rA = PixelUtil.getR(pixelA);
        int gA = PixelUtil.getG(pixelA);
        int bA = PixelUtil.getB(pixelA);
        int pixelB = overlapB.getRGB(x, y);
        int rB = PixelUtil.getR(pixelB);
        int gB = PixelUtil.getG(pixelB);
        int bB = PixelUtil.getB(pixelB);
        int newR = (int)((double)rA * (1.0 - alpha) + (double)rB * alpha);
        int newG = (int)((double)gA * (1.0 - alpha) + (double)gB * alpha);
        int newB = (int)((double)bA * (1.0 - alpha) + (double)bB * alpha);
        int newPixel = PixelUtil.getPixel(newR, newG, newB);
        blended.setRGB(x, y, newPixel);
      }
    }
    return blended;
  }
  
  
  // A and B must have the same width and height
  private BufferedImage seamCarvingBlendOverlaps(BufferedImage overlapA, BufferedImage overlapB) {
    
    BufferedImage blended = new BufferedImage(overlapA.getWidth() + overlapB.getWidth(), overlapA.getHeight(), BufferedImage.TYPE_INT_RGB);
    SeamCarver scA = new SeamCarver(overlapA);
    SeamCarver scB = new SeamCarver(overlapB);
    int[] vSeamA = scA.findVerticalSeam();
    int[] vSeamB = scB.findVerticalSeam();
    
    for (int y = 0; y < blended.getHeight(); y++) {
      int pixelA = overlapA.getRGB(vSeamA[y], y);
      int rA = PixelUtil.getR(pixelA);
      int gA = PixelUtil.getG(pixelA);
      int bA = PixelUtil.getB(pixelA);
      int pixelB = overlapB.getRGB(vSeamB[y], y);
      int rB = PixelUtil.getR(pixelB);
      int gB = PixelUtil.getG(pixelB);
      int bB = PixelUtil.getB(pixelB);
      int vacancyLeft = overlapA.getWidth() - vSeamA[y];
      int vacencyRight = vSeamB[y];
      int vacencyLength = vacancyLeft + vacencyRight;
      for (int x = 0; x < vSeamA[y]; x++) {
        blended.setRGB(x, y, overlapA.getRGB(x, y));
      }
      for (int x = 0; x < vacencyLength; x++) {
        double weight = (double)x / (double)vacencyLength;
        int globalX = x + vSeamA[y];
        int newR = (int)((double)rA + (double)(rB - rA) * weight);
        int newG = (int)((double)gA + (double)(gB - gA) * weight);
        int newB = (int)((double)bA + (double)(bB - bA) * weight);
        int newPixel = PixelUtil.getPixel(newR, newG, newB);
        blended.setRGB(globalX, y, newPixel); 
      }
      for (int x = vSeamA[y] + vacencyLength; x < blended.getWidth(); x++) {
        int overlapLocalX = x - overlapA.getWidth();
        blended.setRGB(x, y, overlapB.getRGB(overlapLocalX, y));
      }
    }
    
    return blended;
    
  }
  
}



class PixelUtil {
  
  public static int getR(int pixel) {
    return (pixel & 0xff0000) >>> 16;
  }
  
  public static int getG(int pixel) {
    return (pixel & 0xff00) >>> 8;
  }
  
  public static int getB(int pixel) {
    return (pixel & 0xff);
  }
  
  public static int getPixel(int red, int green, int blue) {
    if (red < 0) {red = 0;}
    if (green < 0) {green = 0;}
    if (blue < 0) {blue = 0;}
    if (red > 255) {red = 255;}
    if (green > 255) {green = 255;}
    if (blue > 255) {blue = 255;}
    return 0xff000000 | ((red & 0xff) << 16) | ((green & 0xff) << 8) | (blue & 0xff);
  }
  
}
