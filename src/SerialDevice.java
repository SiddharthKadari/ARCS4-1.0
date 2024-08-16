import com.fazecast.jSerialComm.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

public abstract class SerialDevice {

	private SerialPort port;

	private volatile ArrayList<byte[]> receivedMessages = new ArrayList<>();
	private volatile byte[] buffer = null;
	private volatile int dataIndex = 0;

	private boolean waitingForResetFlag = false;

	public SerialDevice(String portDescriptor, int baud) throws IOException, TimeoutException{
		port = SerialPort.getCommPort(portDescriptor);

		port.setComPortParameters(baud, 8, 1, 0);
		port.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

		port.addDataListener(new SerialPortDataListener() { //Listener for data recieved
			@Override
			public int getListeningEvents() {
				return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
			}
			@Override
			public void serialEvent(SerialPortEvent serialPortEvent) {
				processData(serialPortEvent.getReceivedData());
			}

			private void processData(byte[] data){
				if(data.length == 0) return;

				int i = 0;
				if(buffer == null){
					if(data[i] == -1){
						resetDevice();
						processData(Arrays.copyOfRange(data, 1, data.length));
						return;
					}

					buffer = new byte[data[i++]];
				}

				while(i < data.length && dataIndex < buffer.length){
					if(data[i] == -1){
						resetDevice();
						processData(Arrays.copyOfRange(data, i+1, data.length));
						return;
					}
					buffer[dataIndex++] = data[i++];
				}

				if(dataIndex >= buffer.length){
					receivedMessages.add(buffer);
					messageReceived(buffer);
					dataIndex = 0;
					buffer = null;
					if(i != data.length)
						processData(Arrays.copyOfRange(data, i, data.length));
				}
			}

			private void resetDevice(){
				receivedMessages = new ArrayList<>();
				dataIndex = 0;
				buffer = null;
				waitingForResetFlag = false;
			}
		});

		if(!port.openPort(1)){ //this line resets the arduino
			throw new IOException("Serial Port " + portDescriptor + " not Available.");
		}

		waitForReset(10);
	}

	public void waitForReset(double seconds) throws TimeoutException{
		waitingForResetFlag = true;

		for (int i = 0; waitingForResetFlag; i++){
			if(i >= seconds * 10){
				throw new TimeoutException("Reset did not occur in the given waiting period");
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isPortOpen(){
		return port.isOpen();
	}

	public void send(byte data){
		try {
			port.getOutputStream().write(1);
			port.getOutputStream().write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void send(byte[] data){
		try {
			port.getOutputStream().write(data.length);
			port.getOutputStream().write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void send(Byte[] data){
		byte[] dataBytes = new byte[data.length];
		int i = 0;
		for(Byte b : data) dataBytes[i++] = b;

		try {
			port.getOutputStream().write(dataBytes.length);
			port.getOutputStream().write(dataBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public abstract void messageReceived(byte[] msg);

	public byte[] getMessage(int ind){
		return receivedMessages.get(receivedMessages.size() - ind - 1);
	}
	
	public int numMessagesReceived(){
		return receivedMessages.size();
	}

	public void printAllMessagesDebug(){
		System.out.println("\nNum Messages Recieved: " + receivedMessages.size());
		int i = 0;
		for(byte[] arr : receivedMessages){
			System.out.print(i++ + ") ");
			if(arr[0] > 46)
				for(byte b : arr) System.out.print((char)b);
			else
				for(byte b : arr) System.out.print(b + " ");
			System.out.println('\n');
		}
	}

	public void closeDevice(){
		port.closePort();
	}
}
