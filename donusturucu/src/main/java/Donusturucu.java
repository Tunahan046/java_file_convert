import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Donusturucu extends JFrame {
    private static final Logger logger = Logger.getLogger(Donusturucu.class.getName());
    private JPanel mainPanel;
    private JTextField countField;
    private JButton createSectionsButton;
    private JButton convertButton;
    private File[][] files;
    private JComboBox<String>[] formatSelectors;
    private ExecutorService executor = Executors.newFixedThreadPool(10);
    private Lock lock = new ReentrantLock();

    private static final Map<String, String[]> FORMAT_OPTIONS = new HashMap<>();
    static {
        FORMAT_OPTIONS.put("image", new String[]{"JPG", "JPEG", "PNG", "BMP", "PDF"});
        FORMAT_OPTIONS.put("video", new String[]{"MP4", "MKV", "MOV"});
        FORMAT_OPTIONS.put("audio", new String[]{"MP3", "WAV", "WMA", "MP4"});
    }

    public Donusturucu() {
        setTitle("Dosya Dönüştürücü");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        setupLogger();
        logger.info("Dönüştürücü uygulaması başlatıldı.");
        //GUI bileşenlerinin oluşturulması ve yerleştirilmesi
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
                formatSelectors = new JComboBox[count];

                for (int i = 0; i < count; i++) {
                    JPanel section = new JPanel();
                    section.setBorder(BorderFactory.createTitledBorder("Bölüm " + (i + 1)));
                    section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
                    JButton selectButton = new JButton("Dosya Seç");
                    int index = i;
                    selectButton.addActionListener(ev -> selectFiles(index, section));
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

    private void selectFiles(int index, JPanel section) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            files[index] = fileChooser.getSelectedFiles();
            logger.info("Kullanıcı " + (index + 1) + ". bölüm için dosyaları seçti.");

            section.removeAll();
            String fileType = getFileType(files[index][0]); 
            String[] formats = FORMAT_OPTIONS.get(fileType);

            if (formats != null) {
                JComboBox<String> formatCombo = new JComboBox<>(formats);
                formatSelectors[index] = formatCombo;
                section.add(new JLabel("Format Seç:"));
                section.add(formatCombo);
            } else {
                section.add(new JLabel("Desteklenmeyen dosya türü"));
            }

            for (File file : files[index]) {
                JPanel fileInfoPanel = new JPanel();
                fileInfoPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

                ImageIcon icon = (ImageIcon) FileSystemView.getFileSystemView().getSystemIcon(file);
                JLabel iconLabel = new JLabel(icon);
                fileInfoPanel.add(iconLabel);

                JLabel nameLabel = new JLabel(file.getName());
                fileInfoPanel.add(nameLabel);

                section.add(fileInfoPanel);
            }
            section.revalidate();
            section.repaint();
        }
    }

    private class ConvertAction implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        if (files == null || files.length == 0) {
            logger.warning("Conversion cannot start, no files selected.");
            JOptionPane.showMessageDialog(Donusturucu.this, "Please select files first!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (int i = 0; i < files.length; i++) {
            if (formatSelectors[i] != null) {
                String selectedFormat = (String) formatSelectors[i].getSelectedItem();
                logger.info("Selected format for conversion: " + selectedFormat);

                if (files[i] != null) {
                    for (File file : files[i]) {
                        int sectionIndex = i; // Bölümün index'ini yakalıyoruz
                        executor.submit(() -> {
                            long startTime = System.currentTimeMillis();
                            convertFile(file, selectedFormat);
                            long endTime = System.currentTimeMillis();
                            long duration = endTime - startTime; // Duration in milliseconds

                            // Lock kullanarak süreyi güvenli şekilde yazdırıyoruz
                            lock.lock();
                            try {
                                SwingUtilities.invokeLater(() -> {
                                    Component[] components = mainPanel.getComponents();
                                    JPanel sectionPanel = (JPanel) components[sectionIndex];
                                    JLabel durationLabel = new JLabel("Completed " + duration + " ms");
                                    sectionPanel.add(durationLabel);
                                    sectionPanel.revalidate();
                                    sectionPanel.repaint();
                                });
                            } finally {
                                lock.unlock();
                            }
                        });
                    }
                }
            }
        }
    }
}


    private void convertFile(File file, String format) {
    long startTime = System.currentTimeMillis(); // Başlangıç zamanı
    try {
        logger.info("Dönüştürme işlemi başladı: " + file.getName());
        System.out.println("Dönüştürme işlemi başladı: " + file.getName());

        String sanitizedFileName = sanitizeFileName(file.getName());
        String inputExtension = getFileExtension(file);
        Path outputDir = Paths.get(System.getProperty("user.home"), "Desktop", "dönüştürüldü");
        Files.createDirectories(outputDir);

        String outputFileName = "converted_" + sanitizedFileName.replace("." + inputExtension, "." + format.toLowerCase());
        Path targetPath = outputDir.resolve(outputFileName);

        if (format.equalsIgnoreCase("PDF") && isImageFile(inputExtension)) {
            convertImageToPdf(file, targetPath.toString());
        } else {
            List<String> command = new ArrayList<>();
            command.add("C:\\ffmpeg\\ffmpeg.exe");
            command.add("-i");
            command.add(file.getAbsolutePath());

            if (isAudioFile(inputExtension)) {
                command.add("-acodec");
                if (format.equalsIgnoreCase("mp3")) {
                    command.add("libmp3lame");
                    command.add("-b:a");
                    command.add("192k");
                } else if (format.equalsIgnoreCase("wav")) {
                    command.add("pcm_s16le");
                } else {
                    command.add("copy");
                }
            } else if (isVideoFile(inputExtension)) {
                command.add("-c:v");
                command.add("libx264");
                command.add("-c:a");
                command.add("aac");
            } else if (isImageFile(inputExtension)) {
                command.add("-vf");
                command.add("scale=iw:ih");
            }

            command.add(targetPath.toString());

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("FFmpeg output: " + line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg işlemi kodla sonlandırıldı " + exitCode);
            }
        }

        if (Files.exists(targetPath)) {
            long endTime = System.currentTimeMillis(); // Bitiş zamanı
            long duration = endTime - startTime;

            logger.info("Dönüştürme işlemi başarıyla tamamlandı: " + targetPath);
            System.out.println("Dönüştürme işlemi tamamlandı: " + file.getName() + " Süre: " + duration + " ms");

           
        } else {
            throw new RuntimeException("Dosya oluşturuldu ancak bulunamadı: " + targetPath);
        }
    } catch (Exception e) {
        logger.severe("Dönüştürme sırasında hata oluştu: " + e.getMessage());
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
            "Dönüştürme sırasında hata oluştu: " + e.getMessage(),
            "Hata", JOptionPane.ERROR_MESSAGE));
        e.printStackTrace();
    }
}


    private boolean isAudioFile(String extension) {
        return extension.equalsIgnoreCase("mp3") || extension.equalsIgnoreCase("wav") ||
               extension.equalsIgnoreCase("mp4") || extension.equalsIgnoreCase("wma");
    }

    private boolean isVideoFile(String extension) {
        return extension.equalsIgnoreCase("mp4") || 
               extension.equalsIgnoreCase("mkv") || extension.equalsIgnoreCase("mov");
    }

    private boolean isImageFile(String extension) {
        return extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg") ||
               extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("pdf") ||
               extension.equalsIgnoreCase("bmp");
    }

    private String getFileType(File file) {
        String extension = getFileExtension(file).toLowerCase();
        if (FORMAT_OPTIONS.get("image") != null && FORMAT_OPTIONS.get("image").length > 0 && 
            FORMAT_OPTIONS.get("image")[0].toLowerCase().contains(extension)) {
            return "image";
        } else if (FORMAT_OPTIONS.get("video") != null && FORMAT_OPTIONS.get("video").length > 0 && 
                   FORMAT_OPTIONS.get("video")[0].toLowerCase().contains(extension)) {
            return "video";
        } else if (FORMAT_OPTIONS.get("audio") != null && FORMAT_OPTIONS.get("audio").length > 0 && 
                   FORMAT_OPTIONS.get("audio")[0].toLowerCase().contains(extension)) {
            return "audio";
        }
        return "unknown";
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

    private void convertImageToPdf(File imageFile, String outputPath) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(outputPath));//Yeni bir PDF dosyası oluşturmak için diskte bir yazma işlemi başlatır.
        document.open();
        Image image = Image.getInstance(imageFile.getAbsolutePath());//Diskten resim dosyasını okur ve belleğe yükler.
        float scaler = ((document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin()) / image.getWidth()) * 100;
        image.scalePercent(scaler);
        document.add(image);//Bellekteki resim içeriği PDF belgesine eklenir.
        document.close();// PDF dosyası sonlandırılır ve diske yazma işlemi tamamlanır.
    }

   public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Donusturucu().setVisible(true));
    }
}