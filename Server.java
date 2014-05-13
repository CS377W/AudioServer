/*
 * Source: http://stackoverflow.com/questions/15349987/stream-live-android-audio-to-server
 */

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

class Server {

	AudioInputStream audioInputStream;
	static AudioInputStream ais;
	static boolean status = true;
	static int port = 50005;
	static int sampleRate = 44100;

	public static void main(String args[]) throws Exception {

    // Create a DatagramSocket able to receive broadcasts on UDP port `port`
		DatagramSocket serverSocket = new DatagramSocket(port);

		/**
		 * Formula for lag = (byte_size/sample_rate)*2 Byte size 9728 will
		 * produce ~ 0.45 seconds of lag. Voice slightly broken. Byte size 1400
		 * will produce ~ 0.06 seconds of lag. Voice extremely broken. Byte size
		 * 4000 will produce ~ 0.18 seconds of lag. Voice slightly more broken
		 * then 9728.
		 */

		byte[] receiveData = new byte[8448];

		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		while (status == true) {
			serverSocket.receive(receivePacket);
			baos.write(receivePacket.getData());

			if (baos.size() >= 4096 * 100) {
				System.out.println("Recognizing!");
				byte[] data = baos.toByteArray();

				String hexStr = bytesToHex(data);
				System.out.println(hexStr.length());
				PrintWriter out = new PrintWriter("./input.txt");
				out.print(hexStr);
				out.close();

				String[] cmd = { "/bin/bash", "-c", "python recognize_hex.py" };
				Process proc = Runtime.getRuntime().exec(cmd);

				BufferedReader stdInput = new BufferedReader(
						new InputStreamReader(proc.getInputStream()));

				BufferedReader stdError = new BufferedReader(
						new InputStreamReader(proc.getErrorStream()));

				// read the output from the command
				System.out.println("Here is the standard output of the command:\n");
				String s = null;
				while ((s = stdInput.readLine()) != null) {
					System.out.println(s);

          byte[] bytes = s.getBytes();
          serverSocket.send(new DatagramPacket(bytes, bytes.length, receivePacket.getSocketAddress()));
				}

				// read any errors from the attempted command
				System.out.println("Here is the standard error of the command (if any):\n");
				while ((s = stdError.readLine()) != null) {
					System.out.println(s);
				}

				baos = new ByteArrayOutputStream();
			}
		}

		serverSocket.close();
	}

	final protected static char[] hexArray = "0123456789abcdef".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 4];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 4 + 0] = '\\';
			hexChars[j * 4 + 1] = 'x';
			hexChars[j * 4 + 2] = hexArray[v >>> 4];
			hexChars[j * 4 + 3] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
}

