package M2;

public class Problem3 extends BaseClass {
    private static Integer[] array1 = {42, -17, 89, -256, 1024, -4096, 50000, -123456};
    private static Double[] array2 = {3.14159265358979, -2.718281828459, 1.61803398875, -0.5772156649, 0.0000001, -1000000.0};
    private static Float[] array3 = {1.1f, -2.2f, 3.3f, -4.4f, 5.5f, -6.6f, 7.7f, -8.8f};
    private static String[] array4 = {"123", "-456", "789.01", "-234.56", "0.00001", "-99999999"};
    private static Object[] array5 = {-1, 1, 2.0f, -2.0d, "3", "-3.0"};

    private static void bePositive(Object[] arr, int arrayNumber) {
        // Only make edits between the designated "Start" and "End" comments
        printArrayInfo(arr, arrayNumber);

        // UCID: mi348 | Date: 06/08/2025
        // Step 1: Loop through each item in the array
        // Step 2: Use instanceof to check the object's type
        // Step 3: Convert each value to its absolute value
        // Step 4: Cast the value back to its original type
        // Step 5: Assign the converted value to the correct position in the `output` array

        Object[] output = new Object[arr.length];

        // Start Solution Edits
        for (int i = 0; i < arr.length; i++) {
            Object value = arr[i];

            if (value instanceof Integer) {
                output[i] = Math.abs((Integer) value);
            } else if (value instanceof Double) {
                output[i] = Math.abs((Double) value);
            } else if (value instanceof Float) {
                output[i] = Math.abs((Float) value);
            } else if (value instanceof String) {
                try {
                    double num = Double.parseDouble((String) value);
                    num = Math.abs(num);
                    if (((String) value).contains(".")) {
                        output[i] = String.format("%.5f", num).replaceAll("0+$", "").replaceAll("\\.$", "");
                    } else {
                        output[i] = String.format("%.0f", num);
                    }
                } catch (NumberFormatException e) {
                    output[i] = value;
                }
            } else {
                output[i] = value;
            }
        }
        // End Solution Edits

        System.out.println("Output: ");
        printOutputWithType(output);
        System.out.println("");
        System.out.println("______________________________________");
    }

    public static void main(String[] args) {
        final String ucid = "mi348"; // <-- change to your UCID
        // no edits below this line
        printHeader(ucid, 3);
        bePositive(array1, 1);
        bePositive(array2, 2);
        bePositive(array3, 3);
        bePositive(array4, 4);
        bePositive(array5, 5);
        printFooter(ucid, 3);
    }
}
