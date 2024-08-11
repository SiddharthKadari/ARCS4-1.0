import java.io.*;
import java.util.*;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import threephase.FullCube;
import threephase.Search;
import threephase.Tools;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_videoio.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_videoio.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;


public class Main {

	public static final Stopwatch stopWatch = new Stopwatch();

	private static final boolean USING_SOLVER = false;
	private static final boolean USING_ARDUINO = false;
	private static final boolean USING_WEBCAM = true;

	private static final boolean TESTING_SOLVER = false && USING_SOLVER;
	private static final boolean TESTING_ARDUINO = false && USING_ARDUINO;
	private static final boolean TESTING_WEBCAM = true && USING_WEBCAM;
	private static final boolean TESTING_SOLVER_ARDUINO = false && USING_SOLVER && USING_ARDUINO;

	static {
		System.setProperty("org.bytedeco.openblas.load", "mkl");
	}

	public static void main(String[] args) throws TimeoutException {
		printStatusUpdate("PROGRAM START");

		SerialDevice arduino;
		VideoCapture capture;

		if(USING_SOLVER){
			printStatusUpdate(">>> INITIALIZE SOLVER");
			//Initializing .data files
			initializeSolveData();
			printStatusUpdate("<<< SOLVER INITIALIZED");;
		}

		if(USING_ARDUINO){
			printStatusUpdate(">>> CONNECT ARDUINO");
			System.out.println("Connecting to Arduino...");

			try {
				//use {cd /dev} in terminal to get list of all ports
				arduino = new SerialDevice("tty.usbmodem1101", 115200){
					@Override
					public void messageReceived(byte[] msg){
						Main.stopWatch.stop();
					}
				};
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			for(int i = 0; arduino.numMessagesReceived() == 0; i++){
				delay(100);
	
				//5 second timeout
				if(i == 10*10){
					throw new TimeoutException("Arduino failed to connect, try again");
				}
			}

			printStatusUpdate("<<< ARDUINO CONNECTED");
			delay(1000);
		}

		if(USING_WEBCAM){
			printStatusUpdate(">>> CONNECT WEBCAM");

			System.out.println("Connecting to webcam...");
			capture = new VideoCapture(0);
			capture.set(CAP_PROP_FRAME_WIDTH, 640);
			capture.set(CAP_PROP_FRAME_HEIGHT, 480);

			if (!capture.isOpened()) {
				System.out.println("Error opening webcam");
				capture.close();
				capture.release();
				return;
			}

			delay(100);

			printStatusUpdate("<<< WEBCAM CONNECTED");
		}
	
		printStatusUpdate(" ## SYSTEM INIT COMPLETE ##");

		if(TESTING_SOLVER){
			Random r = new Random(System.nanoTime());
			FullCube cube = new FullCube();
			for(int i = 0; i < 4; i++){
				int x = r.nextInt(36);
				cube.execMove(x);
			}
			System.out.println(cube);
			System.out.println(cube.getExecMoveBuffer());
		}

		if(TESTING_ARDUINO){
			stopWatch.start();
			arduino.send(new byte[]{101, 101, 101, 101});
	
			delay(500);

			arduino.printAllMessagesDebug();
		}
	
		if(TESTING_WEBCAM){
			Mat frame = new Mat();
			if (capture.read(frame)) {
				byte[] bgr = new byte[frame.channels()];
				int[] bgrUnsigned = new int[frame.channels()];

				//get pixels with: frame.ptr(x, y).get(rgb);, rgb will be stored in array pixels
				frame.ptr(frame.rows()/2, frame.cols()/2).get(bgr);
				for(int i = 0; i < 3; i++){
					bgrUnsigned[i] = bgr[i] & 0xFF;
				}
				System.out.println(Arrays.toString(bgrUnsigned));
			}
		}



		if(TESTING_SOLVER_ARDUINO){
			Random r = new Random(System.nanoTime());
			FullCube cube = new FullCube();
			for(int i = 0; i < 4; i++){
				int x = r.nextInt(36);
				cube.execMove(x);
			}
			System.out.println(cube);
			System.out.println("Scramble Sequence: " + cube.getExecMoveBuffer());
			Search search = new Search();
			search.with_rotation = false;

			Byte[] sol = search.byteSolve(cube);
			String solStr = search.solve(cube);

			System.out.println("\nSolve Sequence (Length = " + sol.length + "): ");
			for(int i = 0; i < sol.length; i++){
				System.out.print(sol[i].toString() + " ");
			}
			System.out.println("\n\nSolve Sequence (Length = " + sol.length + "): " + solStr);

			System.out.println();
			arduino.send(sol);
		}






		printStatusUpdate("PROGRAM CLEANUP");
		delay(10);
		if(USING_ARDUINO){
			arduino.printAllMessagesDebug();
			arduino.closeDevice();
		}
		if(USING_WEBCAM){
			capture.close();
			capture.release();
		}
		printStatusUpdate("MAIN TERMINATED");
	}

	public static void printByteArrayAsString(byte[] arr){
		for(byte b : arr) System.out.print((char)b);
	}

	private static void initializeSolveData(){
		try {
			DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream("twophase.data")));
			cs.min2phase.Tools.initFrom(dis);
			dis.close();
		} catch (IOException e) {
			e.printStackTrace();
			try {
				DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("twophase.data")));
				cs.min2phase.Tools.saveTo(dos);
				dos.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}

		try {
			DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream("threephase.data")));
			Tools.initFrom(dis);
			dis.close();
		} catch (IOException e) {
			e.printStackTrace();
			try {
				DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("threephase.data")));
				Tools.saveTo(dos);
				dos.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
	}

	private static void printStatusUpdate(String str){
		delay(100);
		System.out.println("\n======================== " + str + " ========================\n");
		delay(100);
	}

	public static void delay(int ms){
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
