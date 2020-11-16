/**
 * SHA-1 Cracker using bruteforce and Client-Server architecture.
 *
 * Authors:     Adir Biran      308567239
 *              Dekel Levy      204318851
 *
 * Date:        08/01/2020
 */


import java.util.Scanner;

public class Main
{

    public static void main(String[] args) {

        Server server = new Server();
        server.start();

        try
        {
            Thread.sleep(2000);

        }
        catch (Exception e)
        {

        }

        init();

    }


    private static void init()
    {
        Scanner in = new Scanner(System.in, "UTF-8");

        System.out.println("Please enter hash to crack:");
        String hashSt = in.nextLine();

        if (!hashSt.matches("[a-f0-9]*") || hashSt.length() != 40) {
            System.out.println("Hash is invalid.");
            return;
        }

        System.out.println("Print enter the input string length:");

        String lenSt = in.nextLine();

        if (!lenSt.matches("[0-9]*")) {
            System.out.println("String length is invalid.");
            return;
        }

        int len = Integer.parseInt(lenSt);

        if (len < 1 || len > 255)
        {
            System.out.println("String length is invalid.");
            return;
        }

        Client cl = new Client(hashSt, len, 50000);
        if (cl.isValid())
            cl.startCracking();
    }







}
