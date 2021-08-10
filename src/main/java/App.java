import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

import org.apache.spark.*;

import org.pcap4j.core.*;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.Packet;
import org.pcap4j.util.NifSelector;

public class App {


    public static void main(String[] args) throws PcapNativeException, NotOpenException, UnknownHostException {

        final int[] maxlength = {1073741824}; // вместо базы данных, ее не успел
        final int[] currenttraffic = {0};

        Scanner in = new Scanner(System.in);
        System.out.print("Input an IP address: ");
        String stradr = in.nextLine();
        InetAddress inetAddress = InetAddress.getByName(stradr);
        PcapNetworkInterface address = Pcaps.getDevByAddress(inetAddress);
        MessageProducer messageProducer = new MessageProducer();

        if (address == null) {
            System.out.println("No IP address chosen.");
            System.exit(1);
        }

        int snapshotLength = 65536;
        int readTimeout = 50;
        final PcapHandle handle;
        handle = address.openLive(snapshotLength, PromiscuousMode.PROMISCUOUS, readTimeout);
        PcapDumper dumper = handle.dumpOpen("out.pcap");

        // Create a listener that defines what to do with the received packets
        PacketListener listener = new PacketListener() {
            @Override
            public void gotPacket(Packet packet) {
                // Print packet information to screen
                currenttraffic[0] += packet.length();
                if (currenttraffic[0] > maxlength[0]) {
                    messageProducer.sendMessage("alerts", "key", "traffic-alert");
                }
                // Dump packets to file
                try {
                    dumper.dump(packet, handle.getTimestamp());
                } catch (NotOpenException e) {
                    e.printStackTrace();
                }
            }
        };

        try {
            int maxPackets = -1;
            handle.loop(maxPackets, listener);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Cleanup when complete
        dumper.close();
        handle.close();
    }
}
