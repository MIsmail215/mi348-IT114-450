package M2;

public class Problem4 extends BaseClass {
    private static String[] array1 = { "hello world!", "java programming", "special@#$%^&characters", "numbers 123 456",
            "mIxEd CaSe InPut!" };
    private static String[] array2 = { "hello world", "java programming", "this is a title case test",
            "capitalize every word", "mixEd CASE input" };
    private static String[] array3 = { "  hello   world  ", "java    programming  ",
            "  extra    spaces  between   words   ",
            "      leading and trailing spaces      ", "multiple      spaces" };
    private static String[] array4 = { "hello world", "java programming", "short", "a", "even" };

    private static void transformText(String[] arr, int arrayNumber) {
        // Only make edits between the designated "Start" and "End" comments
        printArrayInfoBasic(arr, arrayNumber);

        // Challenge 1: Remove non-alphanumeric characters except spaces
        // Challenge 2: Convert text to Title Case
        // Challenge 3: Trim leading/trailing spaces and remove duplicate spaces
        // Result 1-3: Assign final phrase to `placeholderForModifiedPhrase`
        // Challenge 4 (extra credit): Extract middle 3 characters (beginning starts at middle of phrase),
        // assign to 'placeholderForMiddleCharacters'
        // if not enough characters assign "Not enough characters"

        // Step 1: sketch out plan using comments (include ucid and date)
        // UCID: mi348 | Date: 06/08/2025
        // Plan:
        // 1. Loop through each string.
        // 2. Remove non-alphanumeric characters using regex (except spaces).
        // 3. Trim and replace multiple spaces with one.
        // 4. Convert each word to title case (capitalize first letter).
        // 5. Assign cleaned result to `placeholderForModifiedPhrase`.
        // 6. Get middle 3 characters from modified phrase, if length >= 3.
        //    - Start from middle index (length / 2 - 1).
        //    - Else assign "Not enough characters".

        for(int i = 0; i < arr.length; i++){
            String str = arr[i];

            // Remove non-alphanumeric characters except space
            str = str.replaceAll("[^a-zA-Z0-9 ]", "");

            // Trim and remove extra spaces
            str = str.trim().replaceAll(" +", " ");

            // Convert to Title Case
            String[] words = str.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (word.length() > 0) {
                    sb.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) {
                        sb.append(word.substring(1).toLowerCase());
                    }
                    sb.append(" ");
                }
            }
            String placeholderForModifiedPhrase = sb.toString().trim();

            // Middle 3 characters extraction (extra credit)
            String placeholderForMiddleCharacters;
            int len = placeholderForModifiedPhrase.length();
            if (len >= 3) {
                int mid = len / 2;
                if (len % 2 == 0) mid--;
                if (mid - 1 >= 0 && mid + 2 <= len)
                    placeholderForMiddleCharacters = placeholderForModifiedPhrase.substring(mid - 1, mid + 2);
                else
                    placeholderForMiddleCharacters = "Not enough characters";
            } else {
                placeholderForMiddleCharacters = "Not enough characters";
            }

            // Output results
            System.out.println(String.format("Index[%d] \"%s\" | Middle: \"%s\"", i, placeholderForModifiedPhrase, placeholderForMiddleCharacters));
        }

        System.out.println("\n______________________________________");
    }

    public static void main(String[] args) {
        final String ucid = "mi348"; // <-- change to your UCID
        // No edits below this line
        printHeader(ucid, 4);

        transformText(array1, 1);
        transformText(array2, 2);
        transformText(array3, 3);
        transformText(array4, 4);
        printFooter(ucid, 4);
    }
}
