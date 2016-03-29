package lab6;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * This class constructs the UI for a chat client. It implements the chat
 * interface in order to activate the display() method. Warning: Some of the
 * code here is cloned in ServerConsole
 *
 * @author Karen SRocha
 */
public class ClientConsole implements ChatIF {
	
	final public static String LOGIN_ID = "Karen"; // LoginID cannot have spaces

	/**
	 * The default port to connect on.
	 */
	final public static int DEFAULT_PORT = 5555;
	
	/**
	 * The default host to connect on.
	 */
	final public static String DEFAULT_HOST = "localhost";

	/**
	 * The instance of the client that created this ConsoleChat.
	 */
	Client client;

	/**
	 * Constructs an instance of the ClientConsole UI.
	 *
	 * @param host
	 *            The host to connect to.
	 * @param port
	 *            The port to connect on.
	 * @param loginID
	 *            The login ID of the client.
	 */
	public ClientConsole(String loginID, String host, int port) {
		client = new Client(loginID, host, port, this);
	}

	/**
	 * This method waits for input from the console. Once it is received, it
	 * sends it to the client's message handler.
	 */
	public void accept() {
		try {
			BufferedReader fromConsole = new BufferedReader(
					new InputStreamReader(System.in));
			String message;

			while (true) {
				message = fromConsole.readLine();
				client.handleMessageFromClientUI(message);
			}
		} catch (Exception ex) {
			System.out.println("Unexpected error while reading from console!");
		}
	}

	/**
	 * This method overrides the method in the ChatIF interface. It displays a
	 * message onto the screen.
	 *
	 * @param message
	 *            The string to be displayed.
	 */
	public void display(String message) {
		System.out.println("> " + message);
	}

	/**
	 * This method is responsible for the creation of the Client UI.
	 *
	 */
	public static void main(String[] args) {
		String loginID = "";
		String host = DEFAULT_HOST;
		int port = DEFAULT_PORT;

		if (args.length > 0 && isLoginOk(args[0])){
			loginID = args[0];
		} else {
			loginID = LOGIN_ID;
		}

		System.out.println("Connecting...");

		ClientConsole chat = new ClientConsole(loginID, host, port);
		chat.accept(); // Wait for console data
	}
	
	private static boolean isLoginOk(String login){
		boolean result = true;
		if (login.contains(" ")){
			result = false;
		}
		return result;
	}
}
