import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

class Server {

	AudioInputStream audioInputStream;
	static AudioInputStream ais;
	static AudioFormat format;
	static boolean status = true;
	static int port = 50005;
	static int sampleRate = 44100;

	static DataLine.Info dataLineInfo;
	static SourceDataLine sourceDataLine;

	public static void main(String args[]) throws Exception {

		DatagramSocket serverSocket = new DatagramSocket(port);

		/**
		 * Formula for lag = (byte_size/sample_rate)*2 Byte size 9728 will
		 * produce ~ 0.45 seconds of lag. Voice slightly broken. Byte size 1400
		 * will produce ~ 0.06 seconds of lag. Voice extremely broken. Byte size
		 * 4000 will produce ~ 0.18 seconds of lag. Voice slightly more broken
		 * then 9728.
		 */

		byte[] receiveData = new byte[3584];

		format = new AudioFormat(sampleRate, 16, 1, true, false);
		dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
		sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
		sourceDataLine.open(format);
		sourceDataLine.start();

		FloatControl volumeControl = (FloatControl) sourceDataLine
				.getControl(FloatControl.Type.MASTER_GAIN);
		volumeControl.setValue(1.00f);

		DatagramPacket receivePacket = new DatagramPacket(receiveData,
				receiveData.length);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		while (status == true) {
			serverSocket.receive(receivePacket);
			baos.write(receivePacket.getData());
			toSpeaker(receivePacket.getData());

			if (baos.size() >= 4096 * 100) {
				System.out.println("Recognizing!");
				// ByteArrayInputStream baiss = new
				// ByteArrayInputStream(baos.toByteArray());
				// ais = new AudioInputStream(baiss, format,
				// receivePacket.getLength());
				// writeFile(ais);

				//
				byte[] data = baos.toByteArray();

				// for (int i = 0; i < data.length; i++) {
				// System.out.print(data[i] + " ");
				// }
				// System.out.println();

				// System.out.println(bytesToHex(data));
				
				String hexStr = bytesToHex(data);
				System.out.println(hexStr.length());
				PrintWriter out = new PrintWriter("/home/bbunge/Downloads/dejavu-master/input.txt");
				out.print(hexStr);
				out.close();

				String[] cmd = {
						"/bin/bash",
						"-c",
						"cd /home/bbunge/Downloads/dejavu-master/ && python recognize_hex.py" };
				Process proc = Runtime.getRuntime().exec(cmd);

				BufferedReader stdInput = new BufferedReader(
						new InputStreamReader(proc.getInputStream()));

				BufferedReader stdError = new BufferedReader(
						new InputStreamReader(proc.getErrorStream()));

				// read the output from the command
				System.out
						.println("Here is the standard output of the command:\n");
				String s = null;
				while ((s = stdInput.readLine()) != null) {
					System.out.println(s);
				}

				// read any errors from the attempted command
				System.out
						.println("Here is the standard error of the command (if any):\n");
				while ((s = stdError.readLine()) != null) {
					System.out.println(s);
				}

				baos = new ByteArrayOutputStream();
			}
		}

		sourceDataLine.drain();
		sourceDataLine.close();
		serverSocket.close();
	}

	public static void toSpeaker(byte soundbytes[]) {
		try {
			sourceDataLine.write(soundbytes, 0, soundbytes.length);
//			for (int i = 0; i < 10; i++) {
//				System.out.print(soundbytes[i] + " ");
//			}
//			System.out.println();
		} catch (Exception e) {
			System.out.println("Not working in speakers...");
			e.printStackTrace();
		}
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

	public static void writeFile(AudioInputStream stream) {
		try {
			File fileOut = new File("test.wav");
			AudioSystem.write(stream, AudioFileFormat.Type.WAVE, fileOut);
			System.out.println(stream.getFrameLength()); // test statement
			System.out.println(AudioFileFormat.Type.WAVE); // test statement
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}