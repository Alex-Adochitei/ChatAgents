package agents;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class ChatAgent extends Agent {
	private ChatGUI chatGUI;
	private List<AID> peers = new ArrayList<>(); // lista care stocheaza referintele catre alti agenti cunoscuti
	private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Override
	protected void setup() {
		registerService(); // inregistram serviciul oferit de agenti in registrul agentilor

		chatGUI = new ChatGUI(this); // initializare GUI
		chatGUI.showGUI(); // afisare GUI

		// TickerBehaviour pentru descoperirea agentilor
		addBehaviour(new TickerBehaviour(this, 5000) { // executare la fiecare 5s
			@Override
			protected void onTick() {
				discoverPeers(); // descoperim alti agenti disponibili in retea
				chatGUI.updateRecipientList(peers); // actualizam lista de destinatari din GUI
			}
		});

		// CyclicBehaviour pentru primirea mesajelor
		addBehaviour(new CyclicBehaviour() {
			@Override
			public void action() {
				ACLMessage msg = receive(); // primim mesaj daca exista

				if (msg != null) { // daca mesajul exista obtinem
					String sender = msg.getSender().getLocalName(); // numele expediatorului
					String content = msg.getContent(); // continutul mesajului
					String timestamp = getCurrentDateTime(); // timpul curent

					chatGUI.displayMessage(sender, content, timestamp); // afisam mesajul in GUI
					chatGUI.saveMessage(sender, getLocalName(), content, timestamp); // salvam mesajul
				} else {
					block(); // daca nu exista mesajul, blocam comportamentul pana la primirea unui mesaj
				}
			}
		});
	}

	// metoda pentru inregistrarea serviciului oferit de agent
	private void registerService() {
		try {
			DFAgentDescription dfd = new DFAgentDescription(); // cream o descriere a agentului
			dfd.setName(getAID()); // numele agentului

			ServiceDescription sd = new ServiceDescription(); // cream o descriere a serviciului oferit de agent
			sd.setType("chat-service"); // tipul serviciului
			sd.setName("ChatService-" + getLocalName()); // numele serviciului

			dfd.addServices(sd); // adaugam descrierea serviciului la descrierea agentului
			DFService.register(this, dfd); // inregistram agentul si serviciul sau in registrul agentilor
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	// metoda pentru descoperirea altor agenti care ofera acelasi tip de serviciu
	private void discoverPeers() {
		try {
			DFAgentDescription template = new DFAgentDescription(); // template pentru cautarea agentilor

			// specificam tipul serviciului cautat
			ServiceDescription sd = new ServiceDescription();
			sd.setType("chat-service");
			template.addServices(sd);

			// cautam agenti integistrati care corespund template-ului
			DFAgentDescription[] results = DFService.search(this, template);

			peers.clear(); // golim lista existenta de agenti cunoscuti

			for (DFAgentDescription result : results) { // parcurgem rezultatele cautarii
				if (!result.getName().equals(getAID())) { // excludem propriul agent
					peers.add(result.getName()); // adaugam agentul in lista
				}
			}
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	// metoda pentru trimiterea unui mesaj catre un anumit agent.
	public void sendMessageTo(String recipientName, String message) {
		// gasim referinta agentului destinatar dupa numele sau
		AID recipient = peers.stream().filter(aid -> aid.getLocalName().equals(recipientName)).findFirst().orElse(null);

		if (recipient != null) { // daca agentul destinatar este gasit
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM); // cream un mesaj ACL de tip INFORM
			msg.addReceiver(recipient); // adaugam destinatarul
			msg.setContent(message); // setam continutul mesajului
			send(msg); // trimitem mesajul

			String timestamp = getCurrentDateTime(); // obtinem timpul curent pentru afisare

			chatGUI.displayMessage("Eu", message, timestamp); // afisam mesajul trimis in interfata grafica
		} else {
			chatGUI.displayError("Agentul " + recipientName + " nu este disponibil."); // daca destinatarul nu exista
		}
	}

	// metoda apelata la terminarea agentului
	@Override
	protected void takeDown() {
		try {
			DFService.deregister(this); // deregistram serviciul agentului din registrul agentilor
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	// metoda pentru obtinerea timpului curent
	private String getCurrentDateTime() {
		return LocalDateTime.now().format(DATETIME_FORMAT);
	}
}
