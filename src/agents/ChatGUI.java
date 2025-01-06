package agents;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import jade.core.AID;

public class ChatGUI extends JFrame {
	private final ChatAgent agent; // referinta catre agentul asociat GUI-ului

	private JTextArea chatArea; // zona unde sunt afisate mesajele
	private JTextField inputField; // campul pentru introducerea measjului
	private JButton sendButton; // butonul de trimitere a mesajului
	private JComboBox<String> recipientDropdown; // dropdown pentru selectarea destinatarului

	private static final String LOG_FILE = "log_file.txt"; // fisierul de salvare a istoricului

	// constructor pentru initializarea GUI-ului si asocierea cu agentul
	public ChatGUI(ChatAgent agent) {
		this.agent = agent;
		initializeGUI();
	}

	// metoda pentru configurarea GUI-ului
	private void initializeGUI() {
		setTitle("Chat - " + agent.getLocalName());
		setSize(600, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		// zona de afisare a mesajului
		chatArea = new JTextArea(20, 50);
		chatArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(chatArea);
		add(scrollPane, BorderLayout.CENTER);

		// zona de scriere a mesajului
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new BorderLayout());

		// itinializare componente de introducere si timitere a mesajelor
		recipientDropdown = new JComboBox<>();
		inputField = new JTextField();
		sendButton = new JButton("Trimite");

		// adaugam componentele in panou
		inputPanel.add(recipientDropdown, BorderLayout.WEST);
		inputPanel.add(inputField, BorderLayout.CENTER);
		inputPanel.add(sendButton, BorderLayout.EAST);

		add(inputPanel, BorderLayout.SOUTH); // adaugam panoul de introducere a mesajelor

		// adaugam un listener pentru butonul de trimitere
		sendButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String recipient = (String) recipientDropdown.getSelectedItem(); // obtinem destinatarul selectat
				String message = inputField.getText().trim(); // obtinem mesajul introdus

				if (!message.isEmpty() && recipient != null) { // verificam daca exista un destinatar si un mesaj valid
					agent.sendMessageTo(recipient, message); // apelam metoda agentului pentru a trimite mesajul
					inputField.setText(""); // golim campul de introducere
				}
			}
		});
	}

	// metoda pentru afisarea GUI-ului
	public void showGUI() {
		SwingUtilities.invokeLater(() -> setVisible(true));
	}

	// metoda pentru actualizarea listei de destinatari
	public void updateRecipientList(List<AID> peers) {
		SwingUtilities.invokeLater(() -> {
			String currentSelection = (String) recipientDropdown.getSelectedItem(); // salveaza selectia curenta
			recipientDropdown.removeAllItems(); // goleste lista curenta de destinatari

			// adaugam toti agentii descoperiti in lista de destinatari
			for (AID peer : peers) {
				recipientDropdown.addItem(peer.getLocalName());
			}

			// restauram selectia agentilor daca este valida
			if (currentSelection != null
					&& peers.stream().anyMatch(aid -> aid.getLocalName().equals(currentSelection))) {
				recipientDropdown.setSelectedItem(currentSelection);
			}
		});
	}

	// metoda pentru afisarea unui mesaj in zona de text
	public void displayMessage(String sender, String message, String timestamp) {
		SwingUtilities.invokeLater(() -> chatArea.append("[" + timestamp + "] " + sender + ": " + message + "\n"));
	}

	// metoda pentru salvarea unui mesaj in fisier
	public void saveMessage(String sender, String receiver, String message, String timestamp) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
			writer.write("[" + timestamp + "] " + sender + " -> " + receiver + ": " + message);
			writer.newLine();
		} catch (IOException e) {
			System.err.println("Eroare la salvarea mesajului: " + e.getMessage());
		}
	}

	// metoda pentru afisarea unui mesaj de eroare
	public void displayError(String errorMessage) {
		SwingUtilities.invokeLater(() -> chatArea.append("[Eroare]: " + errorMessage + "\n"));
	}
}
