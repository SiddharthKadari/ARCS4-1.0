import java.io.*;
import java.util.*;
import java.util.concurrent.TimeoutException;

import threephase.FullCube;
import threephase.Search;
import threephase.Tools;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_videoio.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_videoio.*;

public class Main {

	public static final Stopwatch stopWatch = new Stopwatch();

	private static final boolean USING_SOLVER = true;
	private static final boolean USING_ARDUINO = true;
	private static final boolean USING_WEBCAM = false;

	private static final boolean TESTING_SOLVER = false && USING_SOLVER;
	private static final boolean TESTING_ARDUINO = false && USING_ARDUINO;
	private static final boolean TESTING_WEBCAM = false && USING_WEBCAM;
	private static final boolean TESTING_SOLVER_ARDUINO = true && USING_SOLVER && USING_ARDUINO;

	private static final int CAMERA_WIDTH = 640;
	private static final int CAMERA_HEIGHT = 480;
	private static final int DISPLAY_WIDTH = 200;
	private static final int DISPLAY_HEIGHT = 200;
	

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
					}
				};
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			for(int i = 0; arduino.numMessagesReceived() == 0; i++){
				delay(100);
	
				//10 second timeout
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
			capture.set(CAP_PROP_FRAME_WIDTH, CAMERA_WIDTH);
			capture.set(CAP_PROP_FRAME_HEIGHT, CAMERA_HEIGHT);

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
			//create datastructures needed for camera test
			Mat frame = new Mat();
			Mat displayGrid = new Mat(DISPLAY_HEIGHT * 3, DISPLAY_WIDTH * 4, CV_8UC3, new Scalar(255, 255, 255, 255));
			
			OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
			// Define the positions for each image
			int[][] positions = {
				{1, 0}, // image1
				{0, 1}, // image2
				{1, 1}, // image3
				{2, 1}, // image4
				{3, 1}, // image5
				{1, 2}  // image6
			};

			Mat[] frameCopies = new Mat[6];
			Mat[] croppedFrames = new Mat[6];
			Rect roi = new Rect((CAMERA_WIDTH - DISPLAY_WIDTH) / 2, (CAMERA_HEIGHT - DISPLAY_HEIGHT) / 2, DISPLAY_WIDTH, DISPLAY_HEIGHT);
			int ind = 0;

			byte[] bgr = new byte[3];

			//collect images
			for(int i = 0; i < 6; i++){
				if (capture.read(frame)) {
					frameCopies[ind++] = frame.clone();

					//get pixels with: frame.ptr(x, y).get(bgr);, bgr will store the pixel data in blue,green,red format
					frame.ptr(frame.rows()/2, frame.cols()/2).get(bgr);
					System.out.println(byteArrayToString(bgr));
				}
	
				delay(1000);
			}

			// crop all images
			for (int i = 0; i < 6; i++) {
				croppedFrames[i] = new Mat(frameCopies[i], roi);
			}

			// Place each image in its position
			for (int i = 0; i < 6; i++) {
				int x = positions[i][0] * DISPLAY_WIDTH;
				int y = positions[i][1] * DISPLAY_HEIGHT;
				Mat insert = displayGrid.apply(new Rect(x, y, DISPLAY_WIDTH, DISPLAY_HEIGHT));
				croppedFrames[i].copyTo(insert);
			}

			//Display grid
			CanvasFrame canvas = new CanvasFrame("Cube Map", 1.0);
			canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
			canvas.showImage(converter.convert(displayGrid));
			converter.close();
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
		}
		printStatusUpdate("MAIN TERMINATED");
	}

	public static String byteArrayToString(byte[] arr){
		String str = "[" + Byte.toUnsignedInt(arr[0]);
		for(int i = 1; i < arr.length; i++) str += ", " + Byte.toUnsignedInt(arr[i]);
		return str + "]";
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
