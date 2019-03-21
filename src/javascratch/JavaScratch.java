/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javascratch;

import java.math.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;

import javax.xml.bind.DatatypeConverter;

/*
 *
 * @author chris
 */
public class JavaScratch
{
    private final static int PAYLOAD_BEACON_1_0  = 0x00;
    private final static int PAYLOAD_BEACON_2_0  = 0x01;
    private final static int PAYLOAD_BEACON_3_0  = 0x03;
    private final static int PAYLOAD_ANALOG      = 0x07;
    private final static int PAYLOAD_PUSHBUTTON  = 0x06;

    private final static String TEST_DATA1 = "0171969CDEA02400000889";
    private final static String TEST_DATA2 = "0101151F3922310007DED6";

    /*
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        // sample Tracker pkt
        byte[] samplePkt = javax.xml.bind.DatatypeConverter.parseHexBinary(TEST_DATA1);

        parseTrackerPacket(samplePkt);
    }

    private static void parseTrackerPacket(byte[] inPacket)
    {
        byte[] bufTemp;

        // grab the portion of the pkt which contains the Message Type Flag
        bufTemp = GetSubArray(inPacket, inPacket.length - 11, 1);
        int iMsgTypeFlag = bufTemp[0];

        // determine which device family sent this message
        switch (iMsgTypeFlag) {
            case PAYLOAD_BEACON_2_0:
                parsePayload2_0(inPacket);
                break;

            default:    // all other trackers - for now
                System.out.printf("Unknown Payload type %n");
                break;
        }
    }

    /*
     * parse the tracker packet according to the 2.0 spec which defines a 10-byte payload
     * @param inPacket the advertisement payload
     *
     * Example:
     *      Input: "0171969CDEA02400000889"
     *      Output:
     *              Battery Flag: false
     *              TxID: 1626 (0x65a)
     *              S/N: 484352036 (0x1cdea024)
     *              Cum Hours: 0.6 Secs: 2185 (0x00000889)
     */
    private static void parsePayload2_0(byte[] inPacket)
    {
        byte[] bufTemp;
        BTPacketContents parseResults = new BTPacketContents();

        /*
         * Battery Flag parsing
         *
         * grab the portion of the pkt which contains the Battery Flag
         */
        bufTemp = GetSubArray(inPacket, inPacket.length - 10, 1);

        // NOTE the Battery Flag is only 1 bit! Lop off the 15 LSBs and pack right
        int iBattFlag = (bufTemp[0] >>> 15) & 0x01;

        boolean bBattFlag = BooleanUtils.toBoolean(iBattFlag);
        System.out.printf("Battery Flag: %b %n", bBattFlag);
        parseResults.setbBattFlag(bBattFlag);

        /*
         * Transmission ID parsing
         *
         * grab the portion of the pkt which contains the Tx ID
         * get 2 bytes, but the Tx ID is actually only 13 bits which straddles 2 byte boundaries
         */
        bufTemp = GetSubArray(inPacket, inPacket.length - 10, 4);

//        ArrayUtils.reverse(bufTemp);

        long ltemp = unsignedIntToLong(bufTemp);

        // NOTE the Tx ID is only 13 bits! Lop off 1 MSB and 6 LSBs and pack right
        long TxId = (ltemp >>> 14) & 0x1FFF;
        System.out.printf("TxID: %s (0x%s) %n", TxId, Long.toHexString(TxId));
        parseResults.setlTxId(TxId);

        /*
         *  Serial Number parsing
         *
         * grab the portion of the pkt which contains the Serial Number (get 4 bytes, but the S/N is actually only 30 bits)
         */
        bufTemp = GetSubArray(inPacket, inPacket.length - 8, 4);

//        ArrayUtils.reverse(bufTemp);

        long serialNum = unsignedIntToLong(bufTemp);

        // NOTE: the Serial Number is only 30 bits! Lop off the 2 MSBs
        serialNum &= 0x3FFFFFFF;
        System.out.printf("S/N: %s (0x%s) %n", serialNum, Long.toHexString(serialNum));
        parseResults.setlSerialNum(serialNum);

        /*
         *  Cumulative Machine Hours parsing
         *
         * grab the portion of the pkt which contains the Machine Runtime Hours
         */
        bufTemp = GetSubArray(inPacket, inPacket.length - 4, 4);
        String secsStr = DatatypeConverter.printHexBinary(bufTemp);

        long lSecs = unsignedIntToLong(bufTemp);
        BigDecimal seconds = new BigDecimal(lSecs);
        BigDecimal secsPerHour = new BigDecimal(3600.0);

        // calc machine hours to 1 decimal place. Round DOWN to be conservative...
        BigDecimal hours = seconds.divide(secsPerHour, 1, RoundingMode.HALF_DOWN);
        System.out.printf("Cum Hours: %s Secs: %s (0x%s) %n", hours, seconds, secsStr);
        parseResults.setlCumMachineHrs(hours.longValue());
        parseResults.setlCumMachineSecs(seconds.longValue());

        // TODO: get the data below at trhe App Level
//        t.LastCaptured = DateTime.Now;
//        t.Rssi = rssi;
//        t.MacAddress = BitConverter.ToString(source.Reverse().ToArray());

    }

    private static byte[] GetSubArray(byte[] source, int srcBegin, int length) {

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

    /**
     * BTPacketContents
     * help class contains the fields which were parsed from the 10-byte Tracker 2.0 payload
     */
    static class BTPacketContents {
        private boolean bBattFlag;
        private long lTxId;
        private long lSerialNum;
        private long lCumMachineHrs;
        private long lCumMachineSecs;

        BTPacketContents(){}

        public void setbBattFlag(boolean bBattFlag) {
            this.bBattFlag = bBattFlag;
        }

        public boolean isbBattFlag() {
            return bBattFlag;
        }

        public long getlTxId() {
            return lTxId;
        }

        public void setlTxId(long lTxId) {
            this.lTxId = lTxId;
        }

        public void setlSerialNum(long lSerialNum) {
            this.lSerialNum = lSerialNum;
        }

        public long getlSerialNum() {
            return lSerialNum;
        }

        public void setlCumMachineHrs(long lCumMachineHrs) {
            this.lCumMachineHrs = lCumMachineHrs;
        }

        public long getlCumMachineHrs() {
            return lCumMachineHrs;
        }

        public void setlCumMachineSecs(long lCumMachineSecs) {
            this.lCumMachineSecs = lCumMachineSecs;
        }

        public long getlCumMachineSecs() {
            return lCumMachineSecs;
        }
    }
}

 
 
/*
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

