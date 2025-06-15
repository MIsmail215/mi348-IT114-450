package M3;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class MadLibsGenerator extends BaseClass {
    private static final String STORIES_FOLDER = "C:\\Users\\Valery\\NJIT\\Summer2025\\IT114\\mi348-IT114-450\\IT114-2025-Module3-Homework\\IT114-2025-Module3-Homework\\M3\\stories";
    private static String ucid = "mi348"; // <-- change to your ucid
//UCID Mi348---Date 6/15/2025
    public static void main(String[] args) {
        printHeader(ucid, 3,
                "Objective: Implement a Mad Libs generator that replaces placeholders dynamically.");

        Scanner scanner = new Scanner(System.in);
        File folder = new File(STORIES_FOLDER);

        if (!folder.exists() || !folder.isDirectory() || folder.listFiles().length == 0) {
            System.out.println("Error: No stories found in the 'stories' folder.");
            printFooter(ucid, 3);
            scanner.close();
            return;
        }

        // 🟡 Step 1: Load a random story file
        File[] storyFiles = folder.listFiles();
        File storyFile = storyFiles[new Random().nextInt(storyFiles.length)];

        List<String> lines = new ArrayList<>();

        try {
            Scanner fileScanner = new Scanner(storyFile);
            // 🟡 Step 2: Read each line into the ArrayList
            while (fileScanner.hasNextLine()) {
                lines.add(fileScanner.nextLine());
            }
            fileScanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("Error reading story file.");
            printFooter(ucid, 3);
            scanner.close();
            return;
        }

        // 🟡 Step 3: Prompt user for each placeholder
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            while (line.contains("<") && line.contains(">")) {
                int start = line.indexOf("<");
                int end = line.indexOf(">", start);

                if (start >= 0 && end > start) {
                    String placeholder = line.substring(start, end + 1);
                    String prompt = placeholder.substring(1, placeholder.length() - 1).replace("_", " ");
                    System.out.print("Enter a(n) " + prompt + ": ");
                    String userInput = scanner.nextLine();
                    line = line.replaceFirst("<[^>]+>", userInput);
                } else {
                    break;
                }
            }
            lines.set(i, line); // 🟡 Step 4: Replace in the original slot
        }

        // 🟢 Output the final story
        System.out.println("\nYour Completed Mad Libs Story:\n");
        for (String line : lines) {
            System.out.println(line);
        }

        printFooter(ucid, 3);
        scanner.close();
    }
}
