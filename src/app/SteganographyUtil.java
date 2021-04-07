package app;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SteganographyUtil {

    // int size in bits
    private final static int INT_SIZE = 32;

    private final static int NUM_RGB_VALUES = 3;

    // rgb size in bits
    private final static int RGB_SIZE = 8;

    // byte size in bits
    private final static int BYTE_SIZE = 8;

    // masks to help manipulate pixels
    private final static byte[] MASKS = new byte[]{(byte)0b10000000, (byte)0b11000000, (byte)0b11110000, (byte)0b11111111};
    private final static int[] MASKS_CLEAR = new int[]{0b00000001, 0b00000011, 0b00001111, 0b11111111};

    // image to be encoded/decoded
    private static BufferedImage image;

    /*
      ###############################################
      # Message Header format: First pixel has the  #
      # information about num bits encoded          #
      #  _________________________________________  #
      # |                               |        |  #
      # | num bits in the message (int) | Message|  #
      # |_______________________________|________|  #
      ###############################################
      */

    public static boolean encrypt(BufferedImage image, byte[] message, int bits, String filename) {
        SteganographyUtil.image = image;

        // set header
        byte[] finalMessage = new byte[message.length+4];
        finalMessage[0] = (byte)(message.length>>(RGB_SIZE*3) & MASKS[3]);
        finalMessage[1] = (byte)(message.length>>(RGB_SIZE*2) & MASKS[3]);
        finalMessage[2] = (byte)(message.length>>(RGB_SIZE) & MASKS[3]);
        finalMessage[3] = (byte)(message.length & MASKS[3]);
        for (int i = 4; i < finalMessage.length; i++) {
            finalMessage[i] = message[i-4];
        }
        if (!encryptImage((byte)bits, finalMessage)){
            return false;
        }
        File outfile = new File(filename);
        try {
            ImageIO.write(image, "png", outfile);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean decrypt(BufferedImage image, String filename) {
        SteganographyUtil.image = image;
        byte[] res;
        int bits;
        int firstPixel = image.getRGB(0,0);
        int x = 1;
        int y = 0;

        // get number of bits encoded per pixel
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
        int remainingP = NUM_RGB_VALUES;  // 3 rgb values per pixel
        int pixel;
        try {
            pixel = image.getRGB(x, y);
        } catch (Exception e) {
            // ran out of pixels
            return false;
        }
        int messageSize = 0;

        // get the size of the message from header
        while (true) {
            byte val = (byte)((pixel >> ((remainingP - 1) *RGB_SIZE))&MASKS_CLEAR[index]);
            messageSize = (messageSize | ((int)val&MASKS_CLEAR[index])); // have to clean again 8 bit edge case
            remainingP--;
            if (i+bits == INT_SIZE){
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
                remainingP = NUM_RGB_VALUES;
            }
        }

        if (messageSize < 0) {
            return false;
        }

        res = new byte[messageSize];
        int resI = 0;
        byte m = 0;
        int j = 0;

        if (remainingP == 0) {
            try {
                pixel = image.getRGB(x, y);
            } catch (Exception e) {
                // ran out of pixels
                return false;
            }
        }

        // decode the message
        while (!(resI >= messageSize)) {
            byte val = (byte)((pixel >> ((remainingP - 1) *RGB_SIZE))&MASKS_CLEAR[index]);
            m = (byte)(m | val);
            remainingP--;
            if (remainingP == 0) {
                if (x >= image.getWidth()-1) {
                    x = 0;
                    y++;
                } else {
                    x++;
                }

                try {
                    pixel = image.getRGB(x, y);
                } catch (Exception e) {
                    // ran out of pixels
                    return false;
                }
                remainingP = NUM_RGB_VALUES;
            }

            // decoded whole byte, write to the result array
            if (j+bits == BYTE_SIZE){
                res[resI] = m;
                resI++;
                j = 0;
                m = 0;
                continue;
            }
            m = (byte)(m << bits);
            j += bits;
        }
        File file = new File(filename);
        try {
            OutputStream os = new FileOutputStream(file);
            os.write(res);
            os.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private static boolean encryptImage(byte bits, byte[] message) {

        // bits has to be 1,2,4, or 8
        if (bits != 1 && bits != 2 && bits != 4 && bits != 8) {
            return false;
        }

        int x = 1;
        int y = 0;
        int remainingP = NUM_RGB_VALUES;
        int remainingM = BYTE_SIZE/bits;
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
        firstPixel = (firstPixel & (~(MASKS_CLEAR[1])));
        firstPixel |= index;
        image.setRGB(0,0, firstPixel);


        int i = 0;
        byte b = message[i];
        int pixel;
        try {
            pixel = image.getRGB(x, y);
        } catch (Exception e) {
            // ran out of pixels
            return false;
        }

        int shift = BYTE_SIZE;

        // encode the message
        while (i < message.length) {
            int temp = (byte)((b&(mask>>(RGB_SIZE-shift))) >> (shift-bits));
            shift -= bits;

            temp &= MASKS_CLEAR[index];
            pixel &= ~(MASKS_CLEAR[index] << ((remainingP-1)*RGB_SIZE));
            pixel |= (temp << ((remainingP-1)*RGB_SIZE));
            remainingP--;
            remainingM--;
            if (remainingP == 0) {
                image.setRGB(x,y,pixel);
                if (x >= image.getWidth()-1) {
                    x = 0;
                    y++;
                } else {
                    x++;
                }

                try {
                    pixel = image.getRGB(x, y);
                } catch (Exception e) {
                    // ran out of pixels
                    return false;
                }
                remainingP = NUM_RGB_VALUES;
            }
            if (remainingM == 0) {
                i++;
                if (i == message.length) {
                    if (remainingP != NUM_RGB_VALUES){
                        image.setRGB(x,y,pixel);
                    }
                    break;
                }
                b = message[i];
                remainingM = BYTE_SIZE/bits;
                shift = BYTE_SIZE;
            }
        }
        return true;
    }
}
