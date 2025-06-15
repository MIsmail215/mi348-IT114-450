package M3;

public class CommandLineCalculator extends BaseClass {
    private static String ucid = "mi348"; // <-- change to your UCID if needed
//UCID-Mi348 Date:6/15/2025
//
    public static void main(String[] args) {
        printHeader(ucid, 1, "Objective: Implement a calculator using command-line arguments.");

        if (args.length != 3) {
            System.out.println("Usage: java M3.CommandLineCalculator <num1> <operator> <num2>");
            printFooter(ucid, 1);
            return;
        }

        try {
            // Parse command-line arguments
            String num1Str = args[0];
            String operator = args[1];
            String num2Str = args[2];

            // Detect number of decimal places
            int decimals1 = countDecimalPlaces(num1Str);
            int decimals2 = countDecimalPlaces(num2Str);
            int maxDecimals = Math.max(decimals1, decimals2);

            // Parse as doubles
            double num1 = Double.parseDouble(num1Str);
            double num2 = Double.parseDouble(num2Str);

            double result;

            if (operator.equals("+")) {
                result = num1 + num2;
            } else if (operator.equals("-")) {
                result = num1 - num2;
            } else {
                System.out.println("Unsupported operator: " + operator);
                printFooter(ucid, 1);
                return;
            }

            // Format output with correct precision
            String format = "%." + maxDecimals + "f";
            System.out.println("Result: " + String.format(format, result));

        } catch (NumberFormatException e) {
            System.out.println("Invalid number format. Please enter valid numeric values.");
        } catch (Exception e) {
            System.out.println("Invalid input. Please ensure correct format and valid numbers.");
        }

        printFooter(ucid, 1);
    }

    // Helper method to count decimal places
    private static int countDecimalPlaces(String numStr) {
        if (numStr.contains(".")) {
            return numStr.length() - numStr.indexOf('.') - 1;
        }
        return 0;
    }
}
