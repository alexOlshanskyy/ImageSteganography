package app;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.util.Arrays;

public class SteganographyUtil {

    private final static byte[] MASKS = new byte[]{(byte)0b10000000, (byte)0b11000000, (byte)0b11110000, (byte)0b11111111};
    private final static int[] MASKS_CLEAR = new int[]{0b00000001, 0b00000011, 0b00001111, 0b11111111};

    private static BufferedImage image;
    private static int bits;

    /*
      ###############################################
      # Message Header format                       #
      #  _________________________________________  #
      # |                               |        |  #
      # | num bits in the message (int) | Message|  #
      # |_______________________________|________|  #
      ###############################################
      */

    public static boolean encrypt(BufferedImage image, byte[] message, int bits, String filename) {
        SteganographyUtil.image = image;
        SteganographyUtil.bits = bits;
        byte[] finalMessage = new byte[message.length+4];
        finalMessage[0] = (byte)(message.length>>24 & MASKS[3]);
        finalMessage[1] = (byte)(message.length>>16 & MASKS[3]);
        finalMessage[2] = (byte)(message.length>>8 & MASKS[3]);
        finalMessage[3] = (byte)(message.length & MASKS[3]);
        for (int i = 4; i < finalMessage.length; i++) {
            finalMessage[i] = message[i-4];
        }
        System.out.println("Final Messsage: " + Arrays.toString(finalMessage));
        encryptImage((byte)bits, finalMessage);
        File outfile = new File(filename);
        try {
            ImageIO.write(image, "png", outfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean decrypt(BufferedImage image, String filename) {
        SteganographyUtil.image = image;
        byte[] res;
        int bits = 0;
        int firstPixel = image.getRGB(0,0);
        int x = 1;
        int y = 0;

        bits = firstPixel & MASKS_CLEAR[1];
        int index = bits;
        if (bits == 0) {
            bits = 1;
        } else if (bits == 1) {
            bits = 2;
        } else if (bits==2) {
            bits = 4;
        } else if (bits == 3) {
            bits = 8;
        }
        int i = 0;
        int remainingP = 3;
        int remainingM = 8/bits;
        int pixel = image.getRGB(x,y);
        System.out.println("Pixel: " + pixel);
        int messageSize = 0;
        while (true) {
            byte val = (byte)((pixel >> ((remainingP - 1) *8))&MASKS_CLEAR[index]);
            System.out.println("Val: " + val);
            messageSize = (messageSize | ((int)val&MASKS_CLEAR[index])); // have to clean again 8 bit edge case
            remainingP--;
            if (i+bits == 32){
                break;
            }
            messageSize = (messageSize << bits);
            i += bits;

            if (remainingP == 0) {
                if (x >= image.getWidth()-1) {
                    x = 0;
                    y++;
                } else {
                    x++;
                }
                pixel = image.getRGB(x, y);
                remainingP = 3;
            }
        }
        System.out.println("This is message size: " + messageSize);
        res = new byte[messageSize];
        int resI = 0;
        byte m = 0;
        int j = 0;
        if (remainingP == 0) {
            pixel = image.getRGB(x, y);
        }
        while (!(resI >= messageSize)) {
            byte val = (byte)((pixel >> ((remainingP - 1) *8))&MASKS_CLEAR[index]);
            System.out.println("VAl: " + val);
            System.out.println("Pixel: " + pixel);
            m = (byte)(m | val);
            //temp = (byte)(temp & MASKS_CLEAR[index]);
            remainingP--;
            if (remainingP == 0) {
                if (x >= image.getWidth()-1) {
                    x = 0;
                    y++;
                } else {
                    x++;
                }
                pixel = image.getRGB(x, y);
                remainingP = 3;
            }
            if (j+bits == 8){
                System.out.println("Here");
                res[resI] = m;
                resI++;
                j = 0;
                m = 0;
                continue;
            }
            m = (byte)(m << bits);
            j += bits;



        }
        System.out.println("Res: " + Arrays.toString(res));
        File ff = new File(filename);
        try {

            OutputStream os = new FileOutputStream(ff);
            os.write(res);
            System.out.println("Write bytes to file.");
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    private static boolean encryptImage(byte bits, byte[] message) {
        System.out.println("This is the length: " + message.length);
        System.out.println("This is array: " + Arrays.toString(message));
        boolean done = false;
        int x = 1;
        int y = 0;
        int remainingP = 3;
        int remainingM = 8/bits;
        int index = 0;
        if (bits == 1){
            index = 0;
        } else if (bits == 2) {
            index = 1;
        } else if (bits == 4) {
            index = 2;
        } else if (bits == 8) {
            index = 3;
        }

        byte mask = MASKS[index];
        int firstPixel = image.getRGB(0,0);
        System.out.println("First Before: " + firstPixel);
        firstPixel = (firstPixel & (~(MASKS_CLEAR[1])));
        firstPixel |= index;
        image.setRGB(0,0, firstPixel);
        System.out.println("First After: " + firstPixel);

        int i = 0;
        byte b = message[i];
        int pixel = image.getRGB(x, y);
        //System.out.println("PixelBefore: " + pixel);
        int shift = 8;
        while (i < message.length) {
            int temp = (byte)((b&(mask>>(8-shift))) >> (shift-bits));
            shift -= bits;

            temp &= MASKS_CLEAR[index];
            //System.out.println("Byte: " + b);
            //System.out.println("Temp: " + temp);
            pixel &= ~(MASKS_CLEAR[index] << ((remainingP-1)*8));
            pixel |= (temp << ((remainingP-1)*8));
            remainingP--;
            remainingM--;
            if (remainingP == 0) {
                image.setRGB(x,y,pixel);
                //System.out.println("PixelAfter: " + pixel);
                if (x >= image.getWidth()-1) {
                    x = 0;
                    y++;
                } else {
                    x++;
                }
                //System.out.println("X: " + x);
                //System.out.println("Y: " + y);

                pixel = image.getRGB(x, y);
                //System.out.println("PixelBefore: " + pixel);
                remainingP = 3;
            }
            if (remainingM == 0) {
                i++;
                if (i == message.length) {
                    if (remainingP != 3){
                        image.setRGB(x,y,pixel);
                        //System.out.println("PixelAfter: " + pixel);
                    }
                    break;
                }
                b = message[i];
                remainingM = 8/bits;
                shift = 8;
            }
        }
        return true;
    }
}
