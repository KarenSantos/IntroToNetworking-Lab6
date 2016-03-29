package lab6;

/**
 * This interface implements the abstract method used to display
 * objects onto the client or server UIs.
 *
 * @author Karen SRocha
 */
public interface ChatIF 
{
  /**
   * Method that when overriden is used to display objects onto
   * a UI.
   */
  public abstract void display(String message);
}
