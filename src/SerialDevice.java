import com.fazecast.jSerialComm.*;

import java.io.IOException;
import java.util.ArrayList;

public abstract class SerialDevice {

	private SerialPort port;

	private volatile ArrayList<byte[]> receivedMessages = new ArrayList<>();
	private volatile byte[] buffer = null;
	private volatile int dataIndex = 0;

	public SerialDevice(String portDescriptor, int baud) throws IOException{
		port = SerialPort.getCommPort(portDescriptor);

		port.setComPortParameters(baud, 8, 1, 0);
		port.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

		if(!port.openPort(10)){
			throw new IOException("Serial Port " + portDescriptor + " not Available.");
		}


		port.addDataListener(new SerialPortDataListener() { //Listener for data recieved
			@Override
			public int getListeningEvents() {
				return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
			}
			@Override
			public void serialEvent(SerialPortEvent serialPortEvent) {
				byte[] data = serialPortEvent.getReceivedData();
				int i = 0;
				if(buffer == null){
					buffer = new byte[data[0]];
					i++;
				}

				while(i < data.length){
					buffer[dataIndex++] = data[i++];
				}

				if(dataIndex >= buffer.length){
					receivedMessages.add(buffer);
					messageReceived(buffer);
					dataIndex = 0;
					buffer = null;
				}
			}
		});

		Main.delay(1000);
	}

	public boolean isPortOpen(){
		return port.isOpen();
	}

	public void send(byte b){
		try {
			port.getOutputStream().write(1);
			port.getOutputStream().write(b);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void send(byte[] b){
		try {
			port.getOutputStream().write(b.length);
			port.getOutputStream().write(b);
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
		System.out.println();
		for(byte[] arr : receivedMessages){
			System.out.print(arr.length + " - ");
			for(byte b : arr) System.out.print((char)b);
			System.out.println();
			for(byte b : arr) System.out.print(b + " ");
			System.out.println('\n');
		}
	}
}
