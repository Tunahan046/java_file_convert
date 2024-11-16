import ws.schild.jave.Encoder;
import ws.schild.jave.AudioAttributes;
import ws.schild.jave.EncodingAttributes;
import ws.schild.jave.MultimediaObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Donusturucu extends JFrame {
    private JPanel mainPanel;
    private JTextField countField;
    private JButton createSectionsButton;
    private JButton convertButton;
    private File[][] files;  
    private ExecutorService executor = Executors.newFixedThreadPool(10); 
    public Donusturucu() {
        setTitle("Dosya Dönüştürücü");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

      
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Kaç dosya bölümü açılacak:"));
        countField = new JTextField(5);
        topPanel.add(countField);
        createSectionsButton = new JButton("Bölüm Oluştur");
        topPanel.add(createSectionsButton);
        add(topPanel, BorderLayout.NORTH);

       
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(0, 1)); 
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        add(scrollPane, BorderLayout.CENTER);

       
        convertButton = new JButton("Dönüştür");
        add(convertButton, BorderLayout.SOUTH);

 
        createSectionsButton.addActionListener(new CreateSectionsAction());
        convertButton.addActionListener(new ConvertAction());
    }

   
    private class CreateSectionsAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            mainPanel.removeAll();  
            int count = Integer.parseInt(countField.getText());
            files = new File[count][];  

            for (int i = 0; i < count; i++) {
                JPanel section = new JPanel();
                section.setBorder(BorderFactory.createTitledBorder("Bölüm " + (i + 1)));
                JButton selectButton = new JButton("Dosya Seç");
                int index = i; 
                selectButton.addActionListener(ev -> selectFiles(index));
                section.add(selectButton);
                mainPanel.add(section);
            }
            mainPanel.revalidate();
            mainPanel.repaint();
        }
    }  
    private void selectFiles(int index) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            files[index] = fileChooser.getSelectedFiles();
        }
    }

    // Dönüştürme işlemi
    private class ConvertAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            for (File[] fileArray : files) {
                if (fileArray != null) {
                    for (File file : fileArray) {
                        executor.submit(() -> convertFile(file));
                    }
                }
            }
        }
    }

private void convertFile(File file) {
    try {
        String extension = getFileExtension(file);
        File outputDir = new File(System.getProperty("user.home"), "Desktop/dönüştürüldü");
        if (!outputDir.exists()) {
            outputDir.mkdir(); 
        }

       
        String outputFileName = "converted_" + file.getName().replace("."+extension, ".wav");
        File target = new File(outputDir, outputFileName);

        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("pcm_s16le"); 
        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setFormat("wav");
        attrs.setAudioAttributes(audio);

     
        new Encoder().encode(new MultimediaObject(file), target, attrs);

        
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, 
            file.getName() + " başarıyla dönüştürüldü ve " + outputFileName + " olarak kaydedildi!", 
            "Dönüştürme Tamamlandı", JOptionPane.INFORMATION_MESSAGE));

    } catch (Exception e) {
        e.printStackTrace(); 
    }
}


    
    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndex = name.lastIndexOf(".");
        return (lastIndex == -1) ? "" : name.substring(lastIndex + 1);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Donusturucu app = new Donusturucu();
            app.setVisible(true);
        });
    }
} 
