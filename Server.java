package ChatApp;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class Server extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton sendImageButton;
    private JButton sendFileButton;
    private JButton createGroupButton;

    private ServerSocket serverSocket;
    private ArrayList<ClientHandler> clients = new ArrayList<>();
    private ArrayList<Group> groups = new ArrayList<>();

    public Server() {
        setTitle("Server");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendImageButton = new JButton("Send Image");
        sendFileButton = new JButton("Send File");
        createGroupButton = new JButton("Create Group");

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(sendImageButton, BorderLayout.WEST);
        inputPanel.add(sendFileButton, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(inputPanel, BorderLayout.SOUTH);
        getContentPane().add(createGroupButton, BorderLayout.NORTH);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessageToAll(messageField.getText());
                messageField.setText("");
            }
        });

        sendImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Choose an image to send");
                int result = fileChooser.showOpenDialog(Server.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    sendImageToAll(selectedFile);
                }
            }
        });

        sendFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Choose a file to send");
                int result = fileChooser.showOpenDialog(Server.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    sendFileToAll(selectedFile);
                }
            }
        });

        createGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createGroup();
            }
        });

        setVisible(true);

        startServer();
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(12345);
            appendMessage("Server started on port 12345");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
                appendMessage("New client connected: " + clientSocket.getInetAddress());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToAll(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
        appendMessage("Server: " + message);
    }

    private void sendImageToAll(File imageFile) {
        for (ClientHandler client : clients) {
            client.sendImage(imageFile);
        }
        appendMessage("Server: [Image] " + imageFile.getName() + " sent to all clients");
    }

    private void sendFileToAll(File file) {
        for (ClientHandler client : clients) {
            client.sendFile(file);
        }
        appendMessage("Server: [File] " + file.getName() + " sent to all clients");
    }

    private void appendMessage(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                chatArea.append(message + "\n");
            }
        });
    }

    private void createGroup() {
        String groupName = JOptionPane.showInputDialog(Server.this, "Enter group name:");
        Group group = new Group(groupName);
        groups.add(group);
        appendMessage("New group created: " + groupName);
    }

    public static void main(String[] args) {
        new Server();
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private ObjectOutputStream outputStream;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("[Image]")) {
                        // Handle image data
                        String imageName = inputLine.substring(7);
                        BufferedImage image = ImageIO.read(clientSocket.getInputStream());
                        // Do something with the image
                        appendMessage("Client: [Image] " + imageName);
                    } else if (inputLine.startsWith("[File]")) {
                        // Handle file data
                        String fileName = inputLine.substring(6);
                        FileOutputStream fileOutputStream = new FileOutputStream(fileName);
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = clientSocket.getInputStream().read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0, bytesRead);
                        }
                        fileOutputStream.close();
                        appendMessage("Client: [File] " + fileName);
                    } else {
                        appendMessage("Client: " + inputLine);
                    }
                }
                in.close();
                clientSocket.close();
                clients.remove(this);
                appendMessage("Client disconnected: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            try {
                outputStream.writeObject(message);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendImage(File imageFile) {
            try {
                outputStream.writeUTF("[Image] " + imageFile.getName());
                outputStream.flush();

                BufferedImage image = ImageIO.read(imageFile);
                ImageIO.write(image, "PNG", outputStream);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendFile(File file) {
            try {
                outputStream.writeUTF("[File] " + file.getName());
                outputStream.flush();

                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class Group {
        private String name;

        public Group(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}