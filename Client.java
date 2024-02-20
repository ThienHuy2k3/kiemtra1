package ChatApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Client extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton sendImageButton;
    private JButton sendFileButton;

    private Socket socket;
    private PrintWriter out;
    private ObjectInputStream inputStream;

    public Client(String serverAddress, int port) {
        setTitle("Client");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendImageButton = new JButton("Send Image");
        sendFileButton = new JButton("Send File");

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(sendImageButton, BorderLayout.WEST);
        inputPanel.add(sendFileButton, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(messageField.getText());
                messageField.setText("");
            }
        });

        sendImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Choose an image to send");
                int result = fileChooser.showOpenDialog(Client.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    sendImage(selectedFile);
                }
            }
        });

        sendFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Choose a file to send");
                int result = fileChooser.showOpenDialog(Client.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    sendFile(selectedFile);
                }
            }
        });

        setVisible(true);

        try {
            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            inputStream = new ObjectInputStream(socket.getInputStream());

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.equals("[Image]")) {
                    receiveImage();
                } else {
                    chatArea.append("Server: " + inputLine + "\n");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void sendMessage(String message) {
        out.println(message);
        chatArea.append("Me: " + message + "\n");
    }

    private void receiveImage() {
        try {
            byte[] imageData = (byte[]) inputStream.readObject();
            ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
            BufferedImage image = ImageIO.read(bais);

            JLabel imageLabel = new JLabel(new ImageIcon(image));
            JOptionPane.showMessageDialog(this, imageLabel, "Received Image", JOptionPane.PLAIN_MESSAGE);
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String serverAddress = "localhost"; // Change this to your server address
        int port = 12345; // Change this to your server port
        new Client(serverAddress, port);
    }

    private void sendFile(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                //out.write(buffer, 0, bytesRead);
            }
            out.flush();
            fileInputStream.close();
            chatArea.append("Me: [File] " + file.getName() + " sent to server\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

   private void sendImage(File imageFile) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            byte[] imageData = baos.toByteArray();
            out.println("[Image]");
            //out.write(imageData);
            out.flush();
            chatArea.append("Me: [Image] " + imageFile.getName() + " sent to server\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}