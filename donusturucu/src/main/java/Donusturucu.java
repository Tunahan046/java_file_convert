import ws.schild.jave.Encoder;
import ws.schild.jave.AudioAttributes;
import ws.schild.jave.EncodingAttributes;
import ws.schild.jave.MultimediaObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.Normalizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Donusturucu extends JFrame {
    private static final Logger logger = Logger.getLogger(Donusturucu.class.getName());
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

        setupLogger();
        logger.info("Dönüştürücü uygulaması başlatıldı.");

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

    private void setupLogger() {
        try {
            FileHandler fileHandler = new FileHandler("donusturucu.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class CreateSectionsAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            mainPanel.removeAll();
            try {
                int count = Integer.parseInt(countField.getText());
                logger.info("Kullanıcı " + count + " dosya bölümü oluşturmak istedi.");
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
            } catch (NumberFormatException ex) {
                logger.severe("Geçersiz giriş: " + ex.getMessage());
                JOptionPane.showMessageDialog(Donusturucu.this, "Lütfen geçerli bir sayı girin!", "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void selectFiles(int index) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            files[index] = fileChooser.getSelectedFiles();
            logger.info("Kullanıcı " + (index + 1) + ". bölüm için dosyaları seçti.");
        }
    }

    private class ConvertAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (files == null || files.length == 0) {
                logger.warning("Dönüştürme işlemi başlatılamadı, dosya seçilmedi.");
                JOptionPane.showMessageDialog(Donusturucu.this, "Lütfen dosyaları seçin!", "Hata", JOptionPane.ERROR_MESSAGE);
                return;
            }

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
        long startTime = System.currentTimeMillis();
        try {
            logger.info("Dönüştürme işlemi başladı: " + file.getName());
            
            // Türkçe karakter dönüşümü
            String sanitizedFileName = sanitizeFileName(file.getName());
            logger.info("Türkçe karakterler temizlendi: " + sanitizedFileName);

            String extension = getFileExtension(file);
            File outputDir = new File(System.getProperty("user.home"), "Desktop/dönüştürüldü");
            if (!outputDir.exists()) {
                outputDir.mkdir();
            }

            String outputFileName = "converted_" + sanitizedFileName.replace("." + extension, ".wav");
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
            logger.info("Dönüştürme işlemi başarıyla tamamlandı: " + file.getName());
        } catch (Exception e) {
            logger.severe("Dönüştürme sırasında hata oluştu: " + e.getMessage());
            e.printStackTrace();
        } finally {
            long endTime = System.currentTimeMillis();
            logger.info("Dönüştürme süresi: " + (endTime - startTime) + " ms");
        }
    }

    private String sanitizeFileName(String fileName) {
        String[][] turkishChars = {
            {"ç", "c"}, {"ğ", "g"}, {"ı", "i"}, {"ö", "o"}, {"ş", "s"}, {"ü", "u"},
            {"Ç", "C"}, {"Ğ", "G"}, {"İ", "I"}, {"Ö", "O"}, {"Ş", "S"}, {"Ü", "U"}
        };
        for (String[] pair : turkishChars) {
            fileName = fileName.replace(pair[0], pair[1]);
        }
        return fileName;
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
