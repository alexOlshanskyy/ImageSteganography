package app;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
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

    public static boolean encrypt(BufferedImage image, byte[] message, int bits) {
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
        encryptImage((byte)bits, finalMessage);
        File outfile = new File("saved.png");
        try {
            ImageIO.write(image, "png", outfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private static boolean encryptImage(byte bits, byte[] message) {
        System.out.println("This is the length: " + message.length);
        System.out.println("This is array: " + Arrays.toString(message));
        boolean done = false;
        int x = 0;
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
        byte mask = (byte)MASKS[index];

        int i = 0;
        byte b = message[i];
        int pixel = image.getRGB(x, y);
        System.out.println("PixelBefore: " + pixel);
        int shift = 8;
        while (i < message.length) {
            int temp = (byte)((b&(mask>>(8-shift))) >> (shift-bits));
            shift -= bits;

            temp &= MASKS_CLEAR[index];
            System.out.println("Byte: " + b);
            System.out.println("Temp: " + temp);
            pixel &= ~(MASKS_CLEAR[index] << ((remainingP-1)*8));
            pixel |= (temp << ((remainingP-1)*8));
            remainingP--;
            remainingM--;
            if (remainingP == 0) {
                image.setRGB(x,y,pixel);
                System.out.println("PixelAfter: " + pixel);
                if (x >= image.getWidth()-1) {
                    x = 0;
                    y++;
                } else {
                    x++;
                }
                //System.out.println("X: " + x);
                //System.out.println("Y: " + y);

                pixel = image.getRGB(x, y);
                System.out.println("PixelBefore: " + pixel);
                remainingP = 3;
            }
            if (remainingM == 0) {
                i++;
                if (i == message.length) {
                    if (remainingP != 3){
                        image.setRGB(x,y,pixel);
                        System.out.println("PixelAfter: " + pixel);
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
