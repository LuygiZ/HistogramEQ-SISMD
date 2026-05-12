package sismd.utils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class Utils {

  Utils() {}

  /**
   * Loads image from filename into a Color (pixels decribed with rgb values) matrix.
   */
  public static Color[][] loadImage(String filename) {
    BufferedImage buffImg = loadImageFile(filename);
    Color[][] colorImg = convertTo2DFromBuffered(buffImg);
    return colorImg;
  }

  /**
   * Converts image from a Color matrix to a .jpg file.
   */
  public static void writeImage(Color[][] image, String filename) {
    File outputfile = new File(filename);
    var bufferedImage = Utils.matrixToBuffered(image);
    try {
      ImageIO.write(bufferedImage, "jpg", outputfile);
    } catch (IOException e) {
      System.out.println("Could not write image " + filename + " !");
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Copy a Color matrix to another Color matrix.
   */
  public static Color[][] copyImage(Color[][] image) {
    Color[][] copy = new Color[image.length][image[0].length];
    for (int i = 0; i < image.length; i++)
      for (int j = 0; j < image[i].length; j++)
        copy[i][j] = image[i][j];
    return copy;
  }

  /** Returns {width, height} by reading only the image header — does not load pixels. */
  public static int[] readDimensions(String filename) {
    try (ImageInputStream iis = ImageIO.createImageInputStream(new File(filename))) {
      Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
      if (readers.hasNext()) {
        ImageReader reader = readers.next();
        try {
          reader.setInput(iis);
          return new int[]{ reader.getWidth(0), reader.getHeight(0) };
        } finally {
          reader.dispose();
        }
      }
    } catch (IOException e) {
      // fall through to fallback
    }
    return new int[]{ 0, 0 };
  }

  private static BufferedImage loadImageFile(String filename) {
    BufferedImage img = null;
    try {
      img = ImageIO.read(new File(filename));
    } catch (IOException e) {
      System.out.println("Could not load image " + filename + " !");
      e.printStackTrace();
      System.exit(1);
    }
    return img;
  }

  private static BufferedImage matrixToBuffered(Color[][] image) {
    int width = image.length;
    int height = image[0].length;
    BufferedImage bImg = new BufferedImage(width, height, 1);
    for (int x = 0; x < width; x++)
      for (int y = 0; y < height; y++)
        bImg.setRGB(x, y, image[x][y].getRGB());
    return bImg;
  }

  private static Color[][] convertTo2DFromBuffered(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    Color[][] result = new Color[width][height];
    for (int x = 0; x < width; x++)
      for (int y = 0; y < height; y++) {
        int pixel = image.getRGB(x, y);
        int red   = (pixel >> 16) & 0xFF;
        int green = (pixel >>  8) & 0xFF;
        int blue  =  pixel        & 0xFF;
        result[x][y] = new Color(red, green, blue);
      }
    return result;
  }
}
