/**
 * SHA-1 Cracker using bruteforce and Client-Server architecture.
 *
 * Authors:     Adir Biran      308567239
 *              Dekel Levy      204318851
 *
 * Date:        08/01/2020
 */

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// team-name: 32 bytes  :   0-31
// type: 1 byte         :   32
// hash: 40 bytes       :   33-72
// length: 1 byte       :   73
// start-string:        :   74 - (74 + length - 1)
// end-string:          :   (74 + length) - (74 + length + length - 1)


public class Server extends Communication {

    private volatile boolean stop;
    private static volatile DatagramSocket socket;

    public Server() {
        socket = null;
        try
        {
            socket = new DatagramSocket(3117);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        this.stop = false;
    }

    public void start() {


        new Thread(() -> {
            try {
                runServer();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }).start();

    }

    private void runServer() {

        ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        exec.setCorePoolSize(5);
        exec.setMaximumPoolSize(5);

        byte[] inServer = new byte[586];
        DatagramPacket rcvPkt = new DatagramPacket(inServer,inServer.length);

        while (!stop) {
            try {
                rcvPkt = new DatagramPacket(inServer,inServer.length);
                socket.receive(rcvPkt); // blocking call

                DatagramPacket finalRcvPkt = rcvPkt;
                exec.execute(() -> {
                    handleClient(finalRcvPkt);

                });

            } catch (Exception e) {
                e.printStackTrace();
            }


        }

        exec.shutdown();

        try {
            exec.awaitTermination(1, TimeUnit.HOURS);
        }
        catch (Exception e)
        {
            System.out.println("Server Error 1, Class: Server");

        }

    }


    private void handleClient(DatagramPacket clientPacket)
    {
        byte[] dataBytes = clientPacket.getData();

        int type = getType(dataBytes);

        try
        {
            if (type == 1)
                handleDiscoverMessage(clientPacket);

            else if (type == 3)
                handleRequestMessage(clientPacket);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


    }


    private void sendAckMessage(InetAddress address, int port, String originalHash, String foundHash)
    {

        byte[] res = new byte[75 + foundHash.length()];

        res = initTeamName(res);
        res[32] = 4;
        res = initHash(res, originalHash);
        res[73] = (byte)(foundHash.length());
        res = initStartString(res, foundHash);

        DatagramPacket packet = new DatagramPacket(res, res.length, address, port);
        try
        {
            socket.send(packet);
            System.out.println("Server sent ACK");

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    private void sendNAckMessage(InetAddress address, int port, int length, String originalHash)
    {

        byte[] res = new byte[76];

        res = initTeamName(res);
        res[32] = 5;
        res = initHash(res, originalHash);
        res[73] = (byte)length;

        DatagramPacket packet = new DatagramPacket(res, res.length, address, port);
        try
        {
            socket.send(packet);
            System.out.println("Server sent NACK");

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void handleDiscoverMessage(DatagramPacket clientPacket)
    {
        System.out.println("Server received Discover");
        byte[] res = new byte[76];
        res = initTeamName(res);
        res[32] = 2;

        for (int i = 33; i < 76; i++)
            res[i] = 0;

        InetAddress address = clientPacket.getAddress();
        int port = clientPacket.getPort();
        DatagramPacket packet = new DatagramPacket(res, res.length, address, port);
        try
        {
            socket.send(packet);
            System.out.println("Server sent Offer");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void handleRequestMessage(DatagramPacket clientPacket)
    {
        System.out.println("Server received Request");

        byte[] data = clientPacket.getData();
        String hash = getHash(data);
        int length = getLength(data);
        String startString = getStartString(data, length);
        String endString = getEndString(data, length);

        String res = decrypt(startString, endString, hash);

        if (res != null)
            sendAckMessage(clientPacket.getAddress(), clientPacket.getPort(), hash, res);
        else
            sendNAckMessage(clientPacket.getAddress(), clientPacket.getPort(), startString.length(), hash);

    }

    private String decrypt(String startString, String endString, String hash) {
        BigInteger start = convertStringToInt(startString);
        BigInteger end = convertStringToInt(endString);
        int len = startString.length();

        for (BigInteger i = start; i.compareTo(end) <= 0; i = i.add(new BigInteger("1"))) {
            String currStr = convertIntToString(i, len);
            String checkHash = encrypt(currStr);
            if (checkHash.equals(hash))
                return currStr;
        }

        return null;

    }


    public void stop() {
        stop = true;
    }


    public String encrypt(String st)
    {
        String sha1 = "";

        try
        {
            MessageDigest hash = MessageDigest.getInstance("SHA-1");
            hash.reset();
            hash.update(st.getBytes("UTF-8"));
            sha1 = byteToHex(hash.digest());
        }

        catch(NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }

        catch(UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        return sha1;
    }

    private String byteToHex(final byte[] byteArray)
    {
        Formatter formatter = new Formatter();

        for (byte b : byteArray)
            formatter.format("%02x", b);

        String result = formatter.toString();
        formatter.close();

        return result;
    }



}
