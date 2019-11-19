import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Merge {
    public static void main(String[] args) throws IOException {
        File ancestor = new File(args[0]);
        File current = new File(args[1]);
        File other = new File(args[2]);
        int markerLenth = Integer.parseInt(args[3]);
        File original = new File(args[4]);

        // HOWTO:
        //  1. Add contents of .gitconfig to your local .git/config
        //  2. This script will run for any merge conflicts

        // TODO auto resolve merge conflicts in `current`
        //  - imports

        boolean failed = true;
        if (failed) {
            System.exit(1); // Auto merge failed
        }
    }

    private static String read(File file) throws IOException {
        byte[] encoded = Files.readAllBytes(file.toPath());
        return new String(encoded, StandardCharsets.UTF_8);
    }
}
