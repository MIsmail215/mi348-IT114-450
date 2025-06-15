package M3;

import java.util.Scanner;
import java.util.Random;

public class SlashCommandHandler extends BaseClass {
    private static String ucid = "mi348"; // <-- change to your UCID if needed

    public static void main(String[] args) {
        printHeader(ucid, 2, "Objective: Implement a simple slash command parser.");

        Scanner scanner = new Scanner(System.in);
        Random rand = new Random();

        while (true) {
            System.out.print("Enter command: ");
            String input = scanner.nextLine().trim();

            // Make it case-insensitive
            String command = input.toLowerCase();

            // /quit command
            if (command.equals("/quit")) {
                System.out.println("Goodbye!");
                break;
            }

            // /greet <name>
            else if (command.startsWith("/greet ")) {
                String[] parts = input.split("\\s+", 2);
                if (parts.length < 2 || parts[1].trim().isEmpty()) {
                    System.out.println("Error: Missing name for /greet");
                } else {
                    System.out.println("Hello, " + parts[1].trim() + "!");
                }
            }

            // /echo <message>
            else if (command.startsWith("/echo ")) {
                String[] parts = input.split("\\s+", 2);
                if (parts.length < 2 || parts[1].trim().isEmpty()) {
                    System.out.println("Error: Missing message for /echo");
                } else {
                    System.out.println(parts[1]);
                }
            }

            // /roll <num>d<sides>
            else if (command.startsWith("/roll ")) {
                String[] parts = input.split("\\s+", 2);
                if (parts.length < 2) {
                    System.out.println("Error: Invalid format. Use /roll <num>d<sides>");
                } else {
                    String[] diceParts = parts[1].toLowerCase().split("d");
                    if (diceParts.length != 2) {
                        System.out.println("Error: Invalid format. Use /roll <num>d<sides>");
                    } else {
                        try {
                            int num = Integer.parseInt(diceParts[0]);
                            int sides = Integer.parseInt(diceParts[1]);

                            if (num <= 0 || sides <= 0) {
                                System.out.println("Error: Dice number and sides must be positive.");
                                continue;
                            }

                            int total = 0;
                            for (int i = 0; i < num; i++) {
                                total += rand.nextInt(sides) + 1;
                            }

                            System.out.println("Rolled " + num + "d" + sides + " and got " + total + "!");
                        } catch (NumberFormatException e) {
                            System.out.println("Error: Dice and sides must be numbers.");
                        }
                    }
                }
            }

            // Invalid command
            else {
                System.out.println("Error: Unrecognized command.");
            }
        }

        printFooter(ucid, 2);
        scanner.close();
    }
}
