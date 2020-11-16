/**
 * SHA-1 Cracker using bruteforce and Client-Server architecture.
 *
 * Authors:     Adir Biran      308567239
 *              Dekel Levy      204318851
 *
 * Date:        08/01/2020
 */


import java.math.BigInteger;
import java.net.DatagramPacket;

public class Communication {

    protected byte[] initHash(byte[] array, String hash)
    {
        byte[] hashBytes = hash.getBytes();

        for (int i = 33, j = 0; i < 73 && j < hash.length(); i++, j++)
            array[i] = hashBytes[j];

        return array;
    }

    protected byte[] initStartString(byte[] array, String startString)
    {
        int len = startString.length();
        byte[] stringBytes = startString.getBytes();

        for (int i = 74, j = 0; i < len+74; i++, j++)
            array[i] = stringBytes[j];

        return array;
    }

    protected byte[] initEndString(byte[] array, String endString)
    {
        int len = endString.length();
        byte[] stringBytes = endString.getBytes();

        for (int i = 74 + len, j = 0; i < (len*2)+74; i++, j++)
            array[i] = stringBytes[j];

        return array;
    }

    protected byte[] initTeamName(byte[] array)
    {
        array[0] = 'm';
        array[1] = 'i';
        array[2] = 'l';
        array[3] = 'u';
        array[4] = 'i';
        array[5] = 'm';

        for (int i = 6; i < 32; i++)
            array[i] = ' ';

        return array;
    }

    protected  String [] divideToDomains (int stringLength, int numOfServers){
        String [] domains = new String[numOfServers * 2];

        StringBuilder first = new StringBuilder(); //aaa
        StringBuilder last = new StringBuilder(); //zzz

        for(int i = 0; i < stringLength; i++){
            first.append("a"); //aaa
            last.append("z"); //zzz
        }

        BigInteger total = convertStringToInt(last.toString());
        BigInteger perServer = total.divide(BigInteger.valueOf(numOfServers));

        domains[0] = first.toString(); //aaa
        domains[domains.length -1 ] = last.toString(); //zzz
        BigInteger summer = new BigInteger("0");

        for(int i = 1; i <= domains.length -2; i += 2){
            summer =  summer.add(perServer);
            domains[i] = convertIntToString(summer, stringLength); //end domain of server
            summer = summer.add(BigInteger.valueOf(1));//++;
            domains[i + 1] = convertIntToString(summer, stringLength); //start domain of next server
        }

        return domains;
    }

    protected String convertIntToString(BigInteger toConvert, int length) {
        StringBuilder s = new StringBuilder(length);
        while (toConvert.compareTo(new BigInteger("0")) > 0 ){
            BigInteger c = toConvert.mod(new BigInteger("26"));
            s.insert(0, (char) (c.intValue() + 'a'));
            toConvert = toConvert.divide(new BigInteger("26"));
            length --;
        }
        while (length > 0){
            s.insert(0, 'a');
            length--;
        }
        return s.toString();
    }

    protected BigInteger convertStringToInt(String toConvert) {
        char[] charArray = toConvert.toCharArray();
        BigInteger num = new BigInteger("0");
        for(char c : charArray){
            if (c != ' ')
                if(c < 'a' || c > 'z'){
                    System.out.println("A"+c+"A");
                    throw new RuntimeException();
            }
            num = num.multiply(new BigInteger("26"));
            int x = c - 'a';
            num = num.add(new BigInteger(Integer.toString(x)));
        }
        return num;
    }

    protected byte getType(byte[] array)
    {
        return array[32];
    }

    protected String getHash(byte[] array)
    {
        byte[] hash = getRangeOfBytes(array, 33, 73);
        return new String(hash);
    }

    protected int getLength(byte[] array)
    {
        return getIntFromByte(array[73]);
    }

    protected String getStartString(byte[] array, int length)
    {

        if (length == 0)
            return "";

        byte[] startString = getRangeOfBytes(array, 74, 74 + length);
        return new String(startString);

    }

    protected String getEndString(byte[] array, int length)
    {

        if (length == 0)
            return "";

        byte[] endString = getRangeOfBytes(array, 74 + length, 74 + (2 * length));
        return new String(endString);

    }

    protected boolean isValidPacket(DatagramPacket packet)
    {
        if (packet == null)
            return false;

        if (packet.getLength() < 76)
            return false;

        if (packet.getData()[32] < 1 || packet.getData()[32] > 5)
            return false;

        return true;

    }

    // including start, not including end
    private byte[] getRangeOfBytes(byte[] array, int start, int end)
    {
        byte[] res = new byte[end - start];

        for (int i = 0; i < res.length; i++, start++)
            res[i] = array[start];

        return res;
    }

    private int getIntFromByte(byte b)
    {
        if (b < 0)
            return b + 256;

        return b;
    }

    protected String getIP(DatagramPacket packet)
    {
        return packet.getAddress().toString().substring(1);
    }
}
