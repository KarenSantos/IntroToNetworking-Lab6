package lab6;

import abstractClient.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * This class overrides some of the methods defined in the abstract superclass
 * in order to give more functionality to the client.
 *
 * @author Karen SRocha
 */
public class Client extends AbstractClient {

	final private int EIGHT_BITS = 8;
	final private int MIN_FRAME_LENGTH = 48;
	final private int SIXTY_FOUR_BYTES = 512;
	final private String SERVER_ADDRESS = "00000000";
	final private String BROADCAST = "11111111";
	final private String FLAG = "01111110";
	final private String FCS = "0000000000000000";
	final private String CONTROL_SNRM = "11001001";
	final private String CONTROL_UA = "11001110";
	final private String CONTROL_INFO = "00010000";
	final private String CONTROL_ACK = "10001000";


	/**
	 * The interface type variable. It allows the implementation of the display
	 * method in the client.
	 */
	ChatIF clientUI;

	/**
	 * The login ID used by the client to be identified by other clients and the
	 * server.
	 */
	private String loginID;

	private Map<String, String> addresses;
	private String myAddress;
	private String wholeFrame;
	private int windowFramesReceived;
	private String destination;

	private boolean isEnteringLogin;
	private boolean snrmReceived;
	private boolean connectionON;
	private boolean messageON;

	/**
	 * Constructs an instance of the chat client.
	 *
	 * @param host
	 *            The server to connect to.
	 * @param port
	 *            The port number to connect on.
	 * @param clientUI
	 *            The interface type variable.
	 * @param loginID
	 *            The login ID of the client.
	 * @throws IOException
	 */
	public Client(String loginID, String host, int port, ChatIF clientUI) {
		super(host, port); // Call the superclass constructor
		this.loginID = loginID;
		this.clientUI = clientUI;
		try {
			openConnection();
			sendToServer("login " + loginID);
			addresses = new HashMap<>();
			wholeFrame = "";
			windowFramesReceived = 0;
			destination = "";
			isEnteringLogin = false;
			snrmReceived = false;
			connectionON = false;
			messageON = false;
		} catch (IOException e) {
			clientUI.display("Cannot open connection. Awaiting command.");
		}
	}

	/**
	 * Returns the login ID of the client.
	 * 
	 * @return The login ID of the client.
	 */
	public String getLoginID() {
		return loginID;
	}

	/**
	 * Sets the login ID of the client.
	 */
	public void setLoginID(String loginID) {
		this.loginID = loginID;
	}

	/**
	 * This method handles all data that comes in from the server.
	 *
	 * @param msg
	 *            The message from the server.
	 */
	public void handleMessageFromServer(Object msg) {

		String message = (String) msg;

		if (message.startsWith("Addresses ")) {
			String receivedAddresses = message.substring(10);
			String[] addressesArray = receivedAddresses.split(" ");

			for (int i = 0; i < addressesArray.length - 1; i += 2) {
				if (!addresses.containsKey(addressesArray[i])) {
					addresses.put(addressesArray[i], addressesArray[i + 1]);
				}
			}
			myAddress = addresses.get(loginID);
			System.out
					.println("Enter Addresses to see all stations connected.\n");

		} else if (message.equals("login exists")) {

			System.out.println("The login used already exists.");
			System.out.print("Please enter a new login: ");
			isEnteringLogin = true;

		} else if (isBinary(message)) {
			wholeFrame += message; // continue to receive fragments until...
			windowFramesReceived++;
			if (wholeFrame.length() >= MIN_FRAME_LENGTH && isFlagOK(wholeFrame)) {

				Map<String, String> frameFields = breakFrame(wholeFrame);
				String destinationAddress = frameFields.get("address");

				if (destinationAddress.equals(myAddress)
						|| destinationAddress.equals(BROADCAST)) {
					String controlField = frameFields.get("control");

					if (controlField.equals(CONTROL_SNRM)) {
						snrmReceived = true;
						System.out.println("SNRM message received in "
								+ windowFramesReceived + " window frames.");
						System.out.println("Message frame: " + wholeFrame);
						System.out
								.println("Would you like to send UA now? (y/n)");
						
					} else if (controlField.equals(CONTROL_INFO)){
						System.out.println("Message received in " + windowFramesReceived + " window frames.");
						System.out.println("Message frame: " + wholeFrame);
						String info = binaryToASCII(frameFields.get("info"));
						System.out.println("Information field translated to:");
						System.out.println(info + "\n");
						System.out.println("Sending ACK message.");
						sendMessage(SERVER_ADDRESS, CONTROL_ACK, "");
						
					} else if (controlField.equals(CONTROL_ACK)){
						System.out.println("ACK message received from Primary station.");
						System.out.println("ACK message received in "
								+ windowFramesReceived + " window frames.");
						System.out.println("ACK frame: " + wholeFrame + "\n.");
					}
				} else {
					System.out.println("Message received was not for me.");
				}
				wholeFrame = "";
				windowFramesReceived = 0;
			}
		} else {
			clientUI.display(message);
		}
	}

	/**
	 * This method handles all data coming from the UI
	 *
	 * @param message
	 *            The message from the UI.
	 */
	public void handleMessageFromClientUI(String message) {

		if (isEnteringLogin) {
			if (message.contains(" ")) {
				System.out.println("The login cannot contain spaces.");
				System.out.print("Please enter another login: ");
			} else {
				isEnteringLogin = false;
				try {
					sendToServer("login " + message);
					this.loginID = message;
				} catch (IOException e) {
					noServer();
				}
			}

		} else if (messageON) {
			
			String binaryMessage = englishToBinary(message);
			if (binaryMessage.length() > SIXTY_FOUR_BYTES){
				System.out.println("Message invalid. Information field can only have 64 bytes. Try again.");
			} else {
				sendMessage(destination, CONTROL_INFO, binaryMessage);
				messageON = false;
				connectionON = true;
				System.out
				.println("Enter Addresses to see all stations connected.");
		System.out
				.println("Or enter the destination address or station name to send message.\n");
			}
			
		} else if (message.toLowerCase().equals("addresses")) {
			System.out.println("---- ADDRESSES ----");
			System.out.println(loginID + ": " + addresses.get(loginID)
					+ " <-- My Address");
			for (String key : addresses.keySet()) {
				if (!key.equals(loginID)) {
					System.out.println(key + ": " + addresses.get(key));
				}
			}

		} else if (snrmReceived) {
			if (message.equals("y")) {
				snrmReceived = false;
				connectionON = true;
				System.out.println("Sending UA message to primary station.");
				sendMessage(SERVER_ADDRESS, CONTROL_UA, "");

				System.out.println("Connection established.");
				System.out
						.println("Enter Addresses to see all stations connected.");
				System.out
						.println("Or enter the destination address or station name to send message.\n");

			} else if (!message.equals("n")) {
				System.out
						.println("You cannot communicate until you send UA message.");
				System.out
						.println("Would you like to send UA message now? (y/n)");
			}

		} else if (connectionON) {
			if (isBinary(message)) {
				if (addresses.containsValue(message)) {
					destination = message;
					connectionON = false;
					messageON = true;
					System.out.println("Enter message to be sent:");
				} else {
					System.out
							.println("Binary address entered is not valid. Try again.\n");
				}
			} else {
				if (addresses.keySet().contains(message)) {
					destination = addresses.get(message);
					connectionON = false;
					messageON = true;
					System.out.println("Enter message to be sent:");
				} else {
					System.out
							.println("Station entered is not valid. Try again.\n");
				}
			}
		} else if (message.equals("quit")){
			quit();
		} else {
			System.out.println("Awaiting command from Primary station.\n");
		}
		
	}

	/**
	 * This method terminates the client.
	 */
	public void quit() {
		if (isConnected()) {
			try {
				sendToServer("logoff");
				closeConnection();
			} catch (IOException e1) {
			}
		}
		System.out.println("Terminating client.");
		System.exit(0);
	}

	/**
	 * This method displays a message if the server has shutdown and terminates
	 * the client.
	 */
	@Override
	protected void connectionException(Exception exception) {
		clientUI.display("Server has shutdown. Abnormal termination of connection.");
	}

	/**
	 * This method displays a message if the connection is closed.
	 */
	protected void connectionClosed() {
		System.out.println("You have been logged off.");
	}

	private void noServer() {
		System.out
				.println("Could not send message to server. Terminating client.");
		quit();
	}

	/**
	 * This method tests if a string is an binary message.
	 * 
	 * @param message
	 *            The message to be tested.
	 * @return True if it is and false if it is not.
	 */
	private boolean isBinary(String message) {
		boolean result = true;
		String[] charac = message.split("");
		for (String s : charac) {
			if (!s.equals("0") && !s.equals("1") && !s.equals("+")
					&& !s.equals("-")) {
				result = false;
			}
		}
		return result;
	}

	private boolean isFlagOK(String message) {
		boolean result = false;
		if (message.substring(0, EIGHT_BITS).equals(FLAG)
				&& message.substring(message.length() - EIGHT_BITS)
						.equals(FLAG))
			result = true;
		return result;
	}

	private Map<String, String> breakFrame(String message) {
		Map<String, String> fields = new HashMap<>();

		fields.put("flag1", message.substring(0, EIGHT_BITS));
		message = message.substring(EIGHT_BITS);

		fields.put("address", message.substring(0, EIGHT_BITS));
		message = message.substring(EIGHT_BITS);

		fields.put("control", message.substring(0, EIGHT_BITS));
		message = message.substring(EIGHT_BITS);

		fields.put("flag2", message.substring(message.length() - EIGHT_BITS));
		message = message.substring(0, message.length() - EIGHT_BITS);

		fields.put("FCS",
				message.substring(message.length() - (2 * EIGHT_BITS)));
		message = message.substring(0, message.length() - (2 * EIGHT_BITS));

		fields.put("info", message);

		return fields;
	}

	private String[] getWindowFrames(int windowSize, String wholeFrame) {

		double size = (double) windowSize;
		int numFrames = (int) Math.ceil(wholeFrame.length() / size);

		String[] windowFrames = new String[numFrames];

		for (int i = 0; i < numFrames - 1; i++) {
			windowFrames[i] = wholeFrame.substring(0, windowSize);
			wholeFrame = wholeFrame.substring(windowSize);
		}
		windowFrames[numFrames - 1] = wholeFrame;
		return windowFrames;
	}

	private void sendMessage(String address, String control, String info) {
		String frame = FLAG + address + control + info + FCS + FLAG;
		System.out.println("Message frame: " + frame);

		String[] windowFrames = getWindowFrames(EIGHT_BITS - 1, frame);
		System.out.println("Window size " + (EIGHT_BITS - 1)
				+ ", message split into " + windowFrames.length + " frames");

		for (String s : windowFrames) {
			try {
				sendToServer(s);
				System.out.println("Frame " + s + " sent.");
			} catch (IOException e) {
				noServer();
			}
		}
		System.out.println("\n");
	}

	private String englishToBinary(String characters) {
		byte[] bytes = characters.getBytes();
		StringBuilder binary = new StringBuilder();
		for (byte b : bytes) {
			int val = b;
			for (int i = 0; i < 8; i++) {
				binary.append((val & 128) == 0 ? 0 : 1);
				val <<= 1;
			}
		}
		return binary + "";
	}
	
	private static String binaryToASCII(String b) {
		String result = "";
		char nextCharacter;
		
		for (int i =0; i <= b.length()-8; i+= 8) {
			nextCharacter = (char) Integer.parseInt(b.substring(i,i+8),2);
			result += nextCharacter;
		}
		return result;
	}

}