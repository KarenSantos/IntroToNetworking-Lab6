package lab6;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import abstractServer.AbstractServer;
import abstractServer.ConnectionToClient;
import lab6.ChatIF;

/**
 * This class overrides some of the methods in the abstract superclass in order
 * to give more functionality to the server.
 *
 * @author Karen SRocha
 */
public class Server extends AbstractServer {

	final private int EIGHT_BITS = 8;
	final private int MIN_FRAME_LENGTH = 48;
	final private int SIXTY_FOUR_BYTES = 512;
	final private String SERVER_ID = "Master";

	final private String SERVER_ADDRESS = "00000000";
	final private String BROADCAST = "11111111";
	final private String FLAG = "01111110";
	final private String FCS = "0000000000000000";
	final private String CONTROL_SNRM = "11001001";
	final private String CONTROL_UA = "11001110";
	final private String CONTROL_INFO = "00010000";
	final private String CONTROL_ACK = "10001000";

	private Map<String, String> addresses;
	private Map<String, Integer> snrmStatus;
	private String wholeFrame;
	private int windowFramesReceived;
	private String destination;

	private boolean snrmON;
	private boolean connectionON;
	private boolean connectionInON;
	private boolean messageON;

	/**
	 * The interface type variable. It allows the implementation of the display
	 * method in the server.
	 */
	ChatIF serverUI;

	/**
	 * Constructs an instance of the echo server.
	 *
	 * @param port
	 *            The port number to connect on.
	 * @param serverUI
	 *            The interface type variable.
	 */
	public Server(int port, ChatIF serverUI) {
		super(port);
		this.serverUI = serverUI;
		addresses = new HashMap<>();
		addresses.put(SERVER_ID, SERVER_ADDRESS);
		snrmStatus = new HashMap<>();
		wholeFrame = "";
		windowFramesReceived = 0;
		destination = "";

		snrmON = false;
		connectionON = false;
		connectionInON = false;
		messageON = false;
	}

	/**
	 * This method handles any messages received from the client.
	 *
	 * @param msg
	 *            The message received from the client.
	 * @param client
	 *            The connection from which the message originated.
	 */
	public void handleMessageFromClient(Object msg, ConnectionToClient client) {

		String message = (String) msg;

		if (connectionInON) {
			wholeFrame += message; // continue to receive fragments until...
			windowFramesReceived++;
			if (wholeFrame.length() >= MIN_FRAME_LENGTH && isFlagOK(wholeFrame)) {

				Map<String, String> frameFields = breakFrame(wholeFrame);
				String destinationAddress = frameFields.get("address");
				String controlField = frameFields.get("control");

				if (destinationAddress.equals(SERVER_ADDRESS)
						|| destinationAddress.equals(BROADCAST)) {

					if (controlField.equals(CONTROL_INFO)) {
						System.out.println("Message received in "
								+ windowFramesReceived + " window frames.");
						System.out.println("Message frame: " + wholeFrame);
						String info = binaryToASCII(frameFields.get("info"));
						System.out.println("Information field translated to:");
						System.out.println(info + "\n");
						System.out.println("Sending ACK message.");
						sendMessage(addresses.get(client.getInfo("LoginID")), CONTROL_ACK, "");

						if (destinationAddress.equals(BROADCAST)) {
							System.out
									.println("Message was sent as broadcast. Forwarding message.");
							forwardMessage(wholeFrame);
						}

					} else if (controlField.equals(CONTROL_ACK)) {
						System.out.println("ACK message received from station "
								+ client.getInfo("LoginID"));
						System.out.println("ACK message received in "
								+ windowFramesReceived + " window frames.");
						System.out.println("ACK frame: " + wholeFrame + "\n.");
					}
				} else {
					System.out.println("Message received was not for me.");
					forwardMessage(wholeFrame);
				}
				wholeFrame = "";
				windowFramesReceived = 0;

				System.out
						.println("Enter Addresses to see all stations connected.");
				System.out
						.println("Or enter the destination address or station name to send message.\n");
			}
		} else if (snrmON) {
			wholeFrame += message; // continue to receive fragments until...
			windowFramesReceived++;
			if (wholeFrame.length() >= MIN_FRAME_LENGTH && isFlagOK(wholeFrame)) {

				Map<String, String> frameFields = breakFrame(wholeFrame);
				String destinationAddress = frameFields.get("address");

				if (destinationAddress.equals(SERVER_ADDRESS)) {
					String controlField = frameFields.get("control");

					if (controlField.equals(CONTROL_UA)) {
						String station = (String) client.getInfo("LoginID");
						snrmStatus.put(station, 1);
						System.out.println("UA message received from station "
								+ station + ".");
						snrmON = false;
						connectionInON = true;
						connectionON = true;
						System.out
								.println("Enter Addresses to see all stations connected.");
						System.out
								.println("Or enter the destination address or station name to send message.\n");
					}
				}
				wholeFrame = "";
				windowFramesReceived = 0;
			}

		} else if (message.startsWith("login")) {
			if (client.getInfo("LoginID") == null) {

				String loginID = message.split(" ")[1];

				if (loginIDAlreadyExists(loginID)) {
					try {
						client.sendToClient("login exists");
					} catch (IOException e) {
						noClient(client);
					}
				} else {
					client.setInfo("LoginID", loginID);

					String ad = generateAddress();
					addresses.put(loginID, ad);
					snrmStatus.put(loginID, 0);
					try {
						client.sendToClient("Connected as --- " + loginID
								+ " ---");
					} catch (IOException e) {
						noClient(client);
					}
					sendToAllClients(loginID + " has connected.");
					sendToAllClients(getAllAddresses());
					System.out.println(loginID + " has connected.");
					System.out
							.println("Enter Addresses to see all users connected.");
					System.out.println("Enter snrm to start transmission\n");
				}
			}
		}
	}

	/**
	 * This method handles all data coming from the UI
	 *
	 * @param message
	 *            The message from the UI.
	 */
	public void handleMessageFromServerUI(String message) {

		if (message.toLowerCase().equals("addresses")) {
			System.out.println("---- ADDRESSES ----");
			System.out.println(SERVER_ID + ": " + SERVER_ADDRESS
					+ " <-- My Address");
			for (String key : addresses.keySet()) {
				if (!key.equals(SERVER_ID)) {
					System.out.println(key + ": " + addresses.get(key));
				}
			}

		} else if (messageON) {
			String binaryMessage = englishToBinary(message);
			if (binaryMessage.length() > SIXTY_FOUR_BYTES) {
				System.out
						.println("Message invalid. Information field can only have 64 bytes. Try again.");
			} else {
				sendMessage(destination, CONTROL_INFO, binaryMessage);
				messageON = false;
				connectionON = true;
				System.out
						.println("Enter Addresses to see all stations connected.");
				System.out
						.println("Or enter the destination address or station name to send message.\n");
			}

		} else if (connectionON) {
			if (isBinary(message)) { // message here is address
				if (addresses.containsValue(message)) {
					if (snrmStatus.get(findKey(message)) == 1) {
						destination = message;
						connectionON = false;
						messageON = true;
						System.out.println("Enter message to be sent:");
					} else {
						System.out
								.println("This station did not send UA. Try another address.");
					}
				} else {
					System.out
							.println("Binary address entered is not valid. Try again.");
				}
			} else {
				if (addresses.keySet().contains(message)) { // message here is
															// LoginID
					if (snrmStatus.get(message) == 1) {
						destination = addresses.get(message);
						connectionON = false;
						messageON = true;
						System.out.println("Enter message to be sent:");
					} else {
						System.out
								.println("This station did not send UA. Try another address.");
					}
				} else {
					System.out
							.println("Station entered is not valid. Try again.");
				}
			}

		} else if (snrmON) {
			System.out.println("Still awaiting for UA message.\n");

		} else if (message.equals("snrm")) {
			snrmON = true;
			System.out.println("\n--- Mode HDLC ON ---");
			System.out.println("Sending SNRM message to all users connected.");

			sendMessage(BROADCAST, CONTROL_SNRM, "");
		} else {
			System.out.println("Enter snrm to start transmission\n");
		}
	}

	/**
	 * This method overrides the one in the superclass. Called when the server
	 * starts listening for connections.
	 */
	protected void serverStarted() {
		System.out.println("Server listening for connections on port "
				+ getPort());
	}

	/**
	 * This method overrides the one in the superclass. Called when the server
	 * stops listening for connections.
	 */
	protected void serverStopped() {
		sendToAllClients("WARNING - The server has stopped listening for connections");
		System.out.println("Server has stopped listening for connections.");
	}

	/**
	 * This method is called each time a new client connection is accepted.
	 * 
	 * @param client
	 *            the connection connected to the client.
	 */
	@Override
	protected void clientConnected(ConnectionToClient client) {
		System.out.println("A station has connected.");

	}

	/**
	 * This method is called each time a client is disconnection from the
	 * server.
	 *
	 * @param client
	 *            the connection with the client.
	 */
	@Override
	synchronized protected void clientDisconnected(ConnectionToClient client) {
		String login = (String) client.getInfo("LoginID");
		addresses.remove(login);
		System.out.println(login + " has disconnected.");
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

	private String generateAddress() {
		String address = "";
		for (int i = 0; i < EIGHT_BITS; i++) {
			address += (int) Math.round(Math.random());
		}
		while (addresses.containsValue(address)) {
			address = "";
			for (int i = 0; i < EIGHT_BITS; i++) {
				address += (int) Math.round(Math.random());
			}
		}
		return address;
	}

	private String getAllAddresses() {
		String result = "Addresses ";
		for (String key : addresses.keySet()) {
			result += key + " " + addresses.get(key) + " ";
		}
		return result;
	}

	private boolean loginIDAlreadyExists(String login) {
		return addresses.containsKey(login);
	}

	private void noClient(ConnectionToClient client) {
		System.out.println("Could not send message to station "
				+ client.getInfo("LoginID"));
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
			sendToAllClients(s);
			System.out.println("Frame " + s + " sent.");
		}
		System.out.println("\n");
	}

	private void forwardMessage(String frame) {
		String[] windowFrames = getWindowFrames(EIGHT_BITS - 1, frame);
		System.out.println("Window size " + (EIGHT_BITS - 1)
				+ ", message split into " + windowFrames.length + " frames");

		for (String s : windowFrames) {
			sendToAllClients(s);
		}
		System.out.println("Message was forwarded.");
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

	private String findKey(String value) {
		String key = "";
		for (String s : addresses.keySet()) {
			if (addresses.get(s).equals(value)) {
				key = s;
			}
		}
		return key;
	}

	private static String binaryToASCII(String b) {
		String result = "";
		char nextCharacter;

		for (int i = 0; i <= b.length() - 8; i += 8) {
			nextCharacter = (char) Integer.parseInt(b.substring(i, i + 8), 2);
			result += nextCharacter;
		}
		return result;
	}
}