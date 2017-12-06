import java.awt.*;
import java.awt.image.*;
import java.lang.*;

public class MyTapestry {

    public static final int VIDEO_WIDTH = 352;
    public static final int VIDEO_HEIGHT = 288;
    public static final int FACTOR = 4;
    public static final int BLOCK_SIZE = 7;

    private int width;
    private int height;
    private float scaleFactor;
    private int numOfScenes;
    private String videoFileName;
    private BufferedImage image;
    private MyVideoPlayer videoPlayer;
    private float frameDist[];
    private int sceneSwitchFrameIndex[];
    private int frameNumPixDict[][];

    MyTapestry(String fileName) {
        width = VIDEO_WIDTH / FACTOR * 6;
        height = VIDEO_HEIGHT / FACTOR;
        scaleFactor = (float)height / (float)VIDEO_HEIGHT;
        numOfScenes = 0;
        videoFileName = fileName;

        //image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        videoPlayer = new MyVideoPlayer(videoFileName);
        frameDist = new float[videoPlayer.getTotalFrames()];
        sceneSwitchFrameIndex = new int[videoPlayer.getTotalFrames()];
        frameNumPixDict = new int[width][height];
    }

    void generateTapestry() {

        int outWidth = VIDEO_WIDTH / FACTOR;
        int outHeight = height;

        int count = 0;
        for (int frameIndex = 0; frameIndex < videoPlayer.getTotalFrames(); frameIndex += 10) {
            byte[] currFrameByteData;
            byte[] nextFrameByteData;
            //BufferedImage currImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            //BufferedImage nextImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

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
                        int nextFrameIndex = 0;
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
                                        //diff += (float)(diffR + diffG + diffB) / 3;

                                        diff += Math.sqrt(diffR*diffR + diffG*diffG + diffB*diffB);

                                    }
                                }

                                //avgDiff += diff / (float)(BLOCK_SIZE * BLOCK_SIZE);
                                diff /= (float)(BLOCK_SIZE * BLOCK_SIZE);

                                if (diff < minDiff) {
                                    minDiff = diff;
                                }

                                nextFrameIndex++;
                            }
                        }

                        frameDistance += minDiff;
                        //System.out.println("MinDiff = " + minDiff);
                        index++;
                    }

                }
                frameDistance /= (float)index;
                frameDist[frameIndex] = frameDistance;

                //System.out.println(frameDistance);

                if (frameDistance > 28.0f) {
                    if (numOfScenes < 1 || frameIndex - sceneSwitchFrameIndex[numOfScenes - 1] > 100) {
                        sceneSwitchFrameIndex[numOfScenes] = frameIndex + 10;    // Next frame will be the first frame of a scene.
                        numOfScenes++;
                        System.out.println(frameIndex + ",    " + frameDistance);
                    }
                }

                count++;
            }

        }



        //for (int u = 0; u < numOfScenes; u++)
        //    System.out.println(sceneSwitchFrameIndex[u]);



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


    }

    public byte[] avgBytes(byte[] origBytes, int OutWidth, int OutHeight, int factor) {
        int width = OutWidth * factor;
        int height = OutHeight * factor;

        byte[] bytes = new byte[3*OutWidth*OutHeight];


        //getWeight(r,sigma);
        for(int y = 0; y < OutHeight; y++) {
            for(int x = 0; x < OutWidth; x++) {

                if(y == OutHeight-1 && x== OutWidth-1)
                    break;
                int xmin = x * factor;
                int xmax = (x+1) * factor;
                int ymin = y * factor;
                int ymax = (y+1) * factor;
                int sumr = 0;
                int sumg = 0;
                int sumb = 0;
                int count = 0;
                for(int i = ymin; i < ymax; i++) {
                    for(int j = xmin; j < xmax; j++) {
                        count++;
                        sumr += origBytes[i*width+j] & 0xff;
                        sumg += origBytes[i*width+j + width * height] & 0xff;
                        sumb += origBytes[i*width+j + 2 * width * height] & 0xff;

                    }

                }
                bytes[y*OutWidth+x] = (byte) (sumr/count);
                bytes[y*OutWidth+x+OutWidth*OutHeight] = (byte) (sumg/count);
                bytes[y*OutWidth+x+2*OutWidth*OutHeight] = (byte) (sumb/count);
                //System.out.println((sumr/count) + " " + (sumg/count) + " " + (sumb/count)) ;

            }
        }
        return bytes;
    }


    BufferedImage getTapestryImage() {
        return image;
    }



}
