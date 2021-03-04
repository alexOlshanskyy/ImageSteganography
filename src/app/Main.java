package app;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    private static Scanner userInput = new Scanner(System.in);

    public static void main(String[] args) {

        Scanner userInput = new Scanner(System.in);
        boolean done;

        System.out.println("Welcome To Image Steganography!");
        System.out.println("To quit enter q");
        System.out.println("To encrypt enter e");
        System.out.println("To decrypt enter d");

        while(true) {
            System.out.println("To encrypt enter e");
            System.out.println("To decrypt enter d");
            String mode = userInput.nextLine();
            if (mode.equals("q")) {
                break;
            }
            while (!mode.equals("e") && !mode.equals("d")) {
                System.out.println("Please enter 'e' if you want to encrypt or 'd' if yo want to decrypt");
                System.out.println(mode);
                mode = userInput.nextLine();
            }

            if (mode.equals("e")) {
                done =  encrypt();
            } else {
                done = decrypt();
            }
            if (!done) {
                System.out.println("Failed, try again.");
            } else {
                System.out.println("Done");
            }
        }
        System.out.println("Goodbye");
    }

    private static boolean encrypt() {
        BufferedImage image;
        byte[] messageFile;
        int nBits;
        int imageCapacity;
        double kb;
        double mb;

        while (true) {
            System.out.println("Enter path to png/PNG image at least 100x100 to encrypt or q to quit:");
            String fileLocation = userInput.nextLine();
            if (!fileLocation.endsWith(".png")
            && !fileLocation.endsWith(".PNG")) {
                System.out.println("Please select a .png or .PNG file");
                continue;
            }
            if (fileLocation.equals("q")) {
                return false;
            }
            File file = new File(fileLocation);
            try {
                image = ImageIO.read(file);
            } catch (IOException e) {
                System.out.println("Failed to load image");
                continue;
            }
            if (image.getHeight() < 100 || image.getWidth() < 100) {
                System.out.println("Minimum size of the image is 100 x 100");
                continue;
            }
            System.out.println("Image Loaded!");
            break;
        }
        while (true) {
            System.out.println("Selects number of bits per byte to encode (1,2,3,4,5,6,7,8):");
            String numBits = userInput.nextLine();
            if (numBits.equals("q")) {
                return false;
            }
            try {
                nBits = Integer.parseInt(numBits);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input");
                continue;
            }
            break;
        }

        // calculate the size of max message
        imageCapacity = image.getHeight() * image.getWidth() * 3 * nBits - 8;
        kb = imageCapacity*1.0/8000;
        mb = 0;
        if (kb > 1000) {
            mb = kb/1000;
        }
        if (mb != 0) {
            System.out.println(String.format("Max message that can be encoded is: %.2f MB", mb));
        } else {
            System.out.println(String.format("Max message that can be encoded is: %.2f KB", kb));
        }


        while (true) {
            System.out.println("Enter path to file with the message");
            String fileLocation = userInput.nextLine();

            if (fileLocation.equals("q")) {
                return false;
            }
            Path path = Paths.get(fileLocation);
            try {
                messageFile = Files.readAllBytes(path);
            } catch (IOException e) {
                System.out.println("Failed to load file");
                continue;
            }
            System.out.println(messageFile.length);
            if (messageFile.length >= (imageCapacity/8)) {
                System.out.println("File too big");
                continue;
            }
            System.out.println("File Loaded!");
            break;
        }

        while (true) {
            System.out.println("Enter output file name");
            String fileName = userInput.nextLine();
            if (fileName.equals("q")) {
                return false;
            }
            if (!fileName.equals("")){
                break;
            }
        }
        System.out.println("Starting encryption");
        if (SteganographyUtil.encrypt(image, messageFile, nBits)) {
            System.out.println("Finished encryption");
            return true;
        } else {
            return false;
        }
    }

    private static boolean decrypt() {
        System.out.println("Enter path to PNG file to decrypt:");
        return true;
    }
}
