import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Server {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new Server().start());
	}

	void start() {
		String portStr = JOptionPane.showInputDialog(null, "Port to listen on:", "5000");
		if (portStr == null) return;
		int port;
		try { port = Integer.parseInt(portStr.trim()); } catch (Exception e) { return; }
		final String name = JOptionPane.showInputDialog(null, "Your display name:", "Server");
		// ensure a non-null display name
		final String displayName = (name == null) ? "Server" : name;

		JFrame waiting = new JFrame("Waiting");
		waiting.setSize(360, 120);
		waiting.setLocationRelativeTo(null);
		JLabel l = new JLabel("Waiting for client on port " + port + "...");
		l.setBorder(new EmptyBorder(18, 18, 18, 18));
		waiting.add(l, BorderLayout.CENTER);
		waiting.setVisible(true);

		Thread accepter = new Thread(() -> {
			try (ServerSocket ss = new ServerSocket(port)) {
				Socket s = ss.accept();
				SwingUtilities.invokeLater(() -> waiting.dispose());
				ChatWindow w = new ChatWindow(s, displayName, false);
				w.setTitle(displayName + " - Server");
				w.setVisible(true);
			} catch (Exception e) {
				SwingUtilities.invokeLater(() -> {
					waiting.dispose();
					JOptionPane.showMessageDialog(null, "Server error: " + e.getMessage());
				});
			}
		});
		accepter.setDaemon(true);
		accepter.start();
	}

	static class ChatWindow extends JFrame {
		private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
		JPanel messages = new JPanel();
		JScrollPane scroll;
		PrintWriter out;

		ChatWindow(Socket socket, String name, boolean isClient) {
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			setSize(540, 640);
			setLocationRelativeTo(null);
			messages.setLayout(new BoxLayout(messages, BoxLayout.Y_AXIS));
			messages.setBackground(new Color(0xF2F2F2));
			scroll = new JScrollPane(messages, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			scroll.getVerticalScrollBar().setUnitIncrement(16);

			JPanel inputBar = new JPanel(new BorderLayout(8, 8));
			JTextField input = new JTextField();
			input.setFont(new Font("Segoe UI", Font.PLAIN, 14));
			JButton send = new JButton("Send");
			inputBar.setBorder(new EmptyBorder(10, 10, 10, 10));
			inputBar.add(input, BorderLayout.CENTER);
			inputBar.add(send, BorderLayout.EAST);

			add(scroll, BorderLayout.CENTER);
			add(inputBar, BorderLayout.SOUTH);

			try {
				out = new PrintWriter(socket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				Thread reader = new Thread(() -> {
					try {
						String line;
						while ((line = in.readLine()) != null) {
							addMessage(line, false);
						}
					} catch (Exception ignored) {}
				});
				reader.setDaemon(true);
				reader.start();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "Connection error: " + e.getMessage());
				dispose();
				return;
			}

			Action sendAction = new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					String text = input.getText().trim();
					if (text.isEmpty()) return;
					String outText = name + ": " + text;
					addMessage(outText, true);
					out.println(outText);
					input.setText("");
				}
			};

			send.addActionListener(sendAction);
			input.addActionListener(sendAction);
			input.addKeyListener(new KeyAdapter() { public void keyPressed(KeyEvent e) { if (e.getKeyCode()==KeyEvent.VK_ENTER) sendAction.actionPerformed(null); } });
		}

		void addMessage(String text, boolean own) {
			String time = LocalTime.now().format(TIME_FMT);
			JPanel wrapper = new JPanel(new FlowLayout(own ? FlowLayout.LEFT : FlowLayout.RIGHT, 0, 0));
			wrapper.setOpaque(false);
			wrapper.setBorder(new EmptyBorder(0, 8, 0, 8));
			Bubble b = new Bubble(text, time, own ? new Color(0xD1E7DD) : new Color(0xE9ECEF));
			wrapper.add(b);
			// Prevent wrapper from stretching vertically in the BoxLayout
			Dimension pref = wrapper.getPreferredSize();
			wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
			wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
			SwingUtilities.invokeLater(() -> {
				messages.add(wrapper);
				messages.revalidate();
				JScrollBar bar = scroll.getVerticalScrollBar();
				int value = bar.getValue();
				int extent = bar.getVisibleAmount();
				int maximum = bar.getMaximum();
				// Only auto-scroll if user is already near the bottom
				if (value + extent + 32 >= maximum) {
					bar.setValue(maximum);
				}
			});
		}

		static class Bubble extends JPanel {
			Bubble(String text, String time, Color bg) {
				setLayout(new BorderLayout());
				JTextArea t = new JTextArea(text);
				t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
				t.setLineWrap(true);
				t.setWrapStyleWord(true);
				t.setEditable(false);
				t.setOpaque(false);
				t.setBorder(new EmptyBorder(6,8,4,8));
				setBackground(new Color(0,0,0,0));
				add(t, BorderLayout.CENTER);
				setBorder(new EmptyBorder(0,4,0,4));
				setOpaque(false);
				final int maxWidth = 360; // limit bubble width for better wrapping
				t.setSize(maxWidth, Short.MAX_VALUE);
				Dimension preferred = t.getPreferredSize();
				preferred.width = Math.min(preferred.width, maxWidth);
				setPreferredSize(new Dimension(preferred.width + 16, preferred.height + 18));
				JLabel timeLabel = new JLabel(time);
				timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
				timeLabel.setForeground(new Color(0x6C757D));
				timeLabel.setBorder(new EmptyBorder(2,6,4,6));
				add(timeLabel, BorderLayout.SOUTH);
				putClientProperty("bg", bg);
			}

			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Color bg = (Color) getClientProperty("bg");
				g2.setColor(bg);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
				g2.dispose();
				super.paintComponent(g);
			}
		}
	}
}

