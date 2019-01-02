/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javascratch;

import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author chris
 */
public class JavaScratch {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // sample Tracker pkt
//        byte[] samplePkt = { 01, 0c, ee, 1f, 8f, 02, a5, 04, 6e, 35, fa };
        byte[] samplePkt = javax.xml.bind.DatatypeConverter.parseHexBinary("010CEE1F8F02A5046E35FA");

        byte[] snBuf = GetSubArray(samplePkt, samplePkt.length - 8, 4);
        
        System.out.println("S/N before: " + snBuf);
        ArrayUtils.reverse(snBuf);
        System.out.println("S/N AFTER: " + snBuf);
        
        long serialNum = unsignedIntToLong(snBuf);
        System.out.println("S/N Before mask: " + serialNum);
        serialNum &= 0x3FFFFFFF;
        System.out.println("S/N AFTER mask: " + serialNum);
    }
    
    private static byte[] GetSubArray(byte[] source, int srcBegin, int length) {
//        int srcEnd = srcBegin + length - 1;
        
        byte[] destination = new byte[length]; // alloc storage for the result
        System.arraycopy(source, srcBegin, destination, 0, length);

        return destination;
    }
    
    private static long unsignedIntToLong(byte[] b) {
        long l = 0;
        l |= b[0] & 0xFF;   // MSB 
        l <<= 8;
        l |= b[1] & 0xFF;
        l <<= 8;
        l |= b[2] & 0xFF;
        l <<= 8;
        l |= b[3] & 0xFF;
        return l;
  }

}

 
 
/**
    Here is some C# code that parses our “01” advertisement message type:

    Given an array of bytes like this:

    byte[] packet01 = { 0x01, 0x0c, 0xee, 0x1f, 0x8f, 0x02, 0xa5, 0x04, 0x6e, 0x35, 0xfa };

    This method will parse the respective fields according to the specification:

    private static Tracker Parse01Packet(byte[] packet, sbyte rssi, byte[] source, TrackerFirmwareType type)
    {
        Tracker t = new Tracker(packet, type);

        byte[] tsnBytes = packet.GetSubArray(packet.Length - 8, 4);
        uint tsn = BitConverter.ToUInt32(tsnBytes.Reverse().ToArray(), 0) & 0x3FFFFFFF;
        t.SerialNumber = tsn;

        byte[] secBytes = packet.GetSubArray(packet.Length - 4, 4);
        uint seconds = BitConverter.ToUInt32(secBytes.Reverse().ToArray(), 0);
        t.Seconds = seconds;
        t.Hours = Math.Round((double)seconds / (float)3600, 1);

        byte b = packet[packet.Length - 10];
        BitArray ba = new BitArray(new byte[] { b });
        t.BatteryLow = ba[7] ? "Yes" : "No";

        byte configByte = (byte)((b & 0x78) >> 3);
        t.ConfigId = configByte;

        byte[] sourceBytes = packet.GetSubArray(packet.Length - 10, 3);
        byte[] byteBuffer = new byte[4];
        Array.Copy(sourceBytes, 0, byteBuffer, 1, sourceBytes.Length);
        uint number = BitConverter.ToUInt32(byteBuffer.Reverse().ToArray(), 0);
        uint transmissionId = (number & 0x7FFC0) >> 6;
        t.TransmissionId = transmissionId;

        t.LastCaptured = DateTime.Now;
        t.Rssi = rssi;

        t.MacAddress = BitConverter.ToString(source.Reverse().ToArray());

        return t;
    }
**/

