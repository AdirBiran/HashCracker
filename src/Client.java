/**
 * SHA-1 Cracker using bruteforce and Client-Server architecture.
 *
 * Authors:     Adir Biran      308567239
 *              Dekel Levy      204318851
 *
 * Date:        08/01/2020
 */


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client extends Communication {

    private DatagramSocket socket;
    private HashSet<String> serversIPs;
    private int port;
    private String hashToCrack;
    private int stringLength;
    private Lock lock;
    private boolean isValid;
    private int timeOut;

    public Client (String hashToCrack, int stringLength, int timeOut)
    {
        if (hashToCrack.length() != 40)
        {
            System.out.println("Hash length is invalid");
            return;
        }

        if (stringLength > 255 || stringLength < 0)
        {
            System.out.println("String length is invalid");
            return;
        }

        this.isValid = true;
        this.port = 3118;
        this.socket = null;
        this.serversIPs = new HashSet<>();
        this.hashToCrack = hashToCrack;
        this.stringLength = stringLength;
        this.lock = new ReentrantLock();

        try
        {
            this.socket = new DatagramSocket(port);
            socket.setSoTimeout(this.timeOut);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    public boolean isValid()
    {
        return this.isValid;
    }

    public void startCracking()
    {
        sendDiscoverMessage();

        int serversSize = serversIPs.size();

        if (serversSize == 0)
            {
                System.out.println("No servers available");
                return;
            }

        String[] domains = divideToDomains(this.stringLength, serversSize);
        int mainIndex = 0;

        ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        exec.setCorePoolSize(5);
        exec.setMaximumPoolSize(5);

        for (String serverIP : serversIPs)
        {
            int index = mainIndex;
            int index2 = mainIndex + 1;
            try {

                exec.execute(() -> {
                    sendRequestMessage(serverIP, domains[index], domains[index2]);

                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            mainIndex = mainIndex + 2;


        }


        byte[] inServer = new byte[586];
        DatagramPacket packet = new DatagramPacket(inServer,inServer.length);

        try
        {
            while (true)
            {
                socket.setSoTimeout(this.timeOut);
                socket.receive(packet);

                if (isAckMessage(packet))
                {
                    String res = showAckedHash(packet);
                    System.out.println("The input string is " + res);
                    this.socket.close();
                    return;
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("Hash not found in time.");
            this.socket.close();
        }

    }

    private String showAckedHash(DatagramPacket packet)
    {
        if (!isValidPacket(packet))
            return "";

        byte[] dataBytes = packet.getData();
        int len = getLength(dataBytes);
        return getStartString(dataBytes, len);
    }

    private boolean isAckMessage(DatagramPacket packet)
    {

        if (!isValidPacket(packet))
            return false;

        byte[] packetBytes = packet.getData();

        if (packetBytes.length < 32)
            return false;

        if (packetBytes[32] == 4)
            return true;

        return false;
    }

    private void sendRequestMessage(String serverIP, String startString, String endString)
    {

        byte[] res = new byte[74 + (this.stringLength*2)];
        res = initTeamName(res);
        res[32] = 3;
        res = initHash(res, this.hashToCrack);
        res[73] = (byte)this.stringLength;
        res = initStartString(res, startString);
        res = initEndString(res, endString);

        try {
            DatagramPacket packet = new DatagramPacket(res, res.length, InetAddress.getByName(serverIP), 3117);
            lock.lock();
            socket.send(packet);
            System.out.println("Client sent Request to "+serverIP);
            lock.unlock();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    public void sendDiscoverMessage()
    {

        byte[] res = new byte[76];
        res = initTeamName(res);
        res[32] = 1;


        try
        {
            DatagramPacket packet = new DatagramPacket(res, res.length, InetAddress.getByName("255.255.255.255"), 3117);

            socket.send(packet);
            System.out.println("Client sent Discover");

            socket.setSoTimeout(10000); // implement

            while (true)
            {
                socket.receive(packet);
                if (isOfferMessage(packet))
                {
                    System.out.println("Client received Offer");
                    if (!serversIPs.contains(getIP(packet)))
                        serversIPs.add(getIP(packet));
                }

            }
        }
        catch (Exception e)
        {
        }

    }

    private boolean isOfferMessage(DatagramPacket packet)
    {
        byte[] packetBytes = packet.getData();

        if (packetBytes.length < 32)
            return false;

        if (packetBytes[32] == 2)
            return true;

        return false;
    }




}
