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
        
        // Butonlar için olay işleyiciler ekler
        createSectionsButton.addActionListener(new CreateSectionsAction()); 
        // Bölüm oluşturma olay işleyicisini ekler 
        convertButton.addActionListener(new ConvertAction()); // Dönüştürme olay işleyicisini ekler
    }

    private void setupLogger() {
        try {
            //loglama dosyası oluşturur
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
                
                // Her dosya için ayrı bir bölüm oluşturur.
                for (int i = 0; i < count; i++) {
                    JPanel section = new JPanel();
                    section.setBorder(BorderFactory.createTitledBorder("Bölüm " + (i + 1)));
                    section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
                    JButton selectButton = new JButton("Dosya Seç");
                    int index = i;
                    //Düğmeye tıklandığında selectFiles metodunu çağıracak
                    selectButton.addActionListener(ev -> selectFiles(index, section));
                    section.add(selectButton);

                    mainPanel.add(section);// Bölümü ana panele ekler
                }
                //ana paneli yeniler ve yeniden oluşturur
                mainPanel.revalidate();
                mainPanel.repaint();
            } catch (NumberFormatException ex) {
                logger.severe("Geçersiz giriş: " + ex.getMessage());
                JOptionPane.showMessageDialog(Donusturucu.this, "Lütfen geçerli bir sayı girin!", "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

private void selectFiles(int index, JPanel section) {
    JFileChooser fileChooser = new JFileChooser(); // Dosya seçici bileşeni oluşturur
    fileChooser.setMultiSelectionEnabled(true); 
    int result = fileChooser.showOpenDialog(this); // Dosya seçici diyalog penceresini aç
    if (result == JFileChooser.APPROVE_OPTION) { // Kullanıcı dosya seçimini onayladıysa
        files[index] = fileChooser.getSelectedFiles(); // Seçilen dosyaları saklar
        logger.info("Kullanıcı " + (index + 1) + ". bölüm için dosyaları seçti."); 
        section.removeAll(); 
        // Dosya türünü belirler
        String fileType = getFileType(files[index][0]);
        // Dosya türüne göre uygun formatları alır
        String[] formats = FORMAT_OPTIONS.get(fileType);

        if (formats != null) {
            JComboBox<String> formatCombo = new JComboBox<>(formats); // Format seçim kutusu oluşturur
            formatSelectors[index] = formatCombo; // Format seçim kutusunu kaydeder
            section.add(new JLabel("Format Seç:")); // Format seç etiketi ekler
            section.add(formatCombo); // Format seçim kutusunu bölüme ekler
        } else {
            section.add(new JLabel("Desteklenmeyen dosya türü")); // Desteklenmeyen dosya türü etiketi ekler
        }
        // Seçilen dosya bilgilerini ve simgelerini gösterir
        for (File file : files[index]) {
            JPanel fileInfoPanel = new JPanel();
            fileInfoPanel.setLayout(new FlowLayout(FlowLayout.LEFT)); 

            ImageIcon icon = (ImageIcon) FileSystemView.getFileSystemView().getSystemIcon(file); 
            JLabel iconLabel = new JLabel(icon); 
            fileInfoPanel.add(iconLabel);

            JLabel nameLabel = new JLabel(file.getName()); 
            fileInfoPanel.add(nameLabel);

            section.add(fileInfoPanel); // Dosya bilgilerini bölüme ekler
        }
        section.revalidate(); // Bölümü yeniden oluşturur
        section.repaint(); // Bölümü yeniden çizer
    }
}

    
    // Dosya dönüştürme işlemini gerçekleştiren iç sınıf
private class ConvertAction implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        
        if (files == null || files.length == 0) {
            logger.warning("Dönüştürme başlatılamıyor, hiçbir dosya seçilmedi."); 
            JOptionPane.showMessageDialog(Donusturucu.this, "Lütfen bir dosya seçiniz!", "Hata", JOptionPane.ERROR_MESSAGE); 
            return;
        }
        // Dosya dönüştürme işlemini başlatır
        for (int i = 0; i < files.length; i++) {
            if (formatSelectors[i] != null) {
                String selectedFormat = (String) formatSelectors[i].getSelectedItem(); // Seçilen formatı al
                logger.info("Dönüştürme için seçilen format: " + selectedFormat); 

                if (files[i] != null) {
                    for (File file : files[i]) {
                        int sectionIndex = i; // Bölümün indeksini yakalar
                        // İş parçacığı oluşturur ve işlemi başlatır
                        executor.submit(() -> {
                            long startTime = System.currentTimeMillis(); 
                            convertFile(file, selectedFormat);
                            long endTime = System.currentTimeMillis(); 
                            long duration = endTime - startTime; 

                            // Lock kullanarak süreyi güvenli şekilde günceller
                            lock.lock();
                            try {
                                SwingUtilities.invokeLater(() -> {
                                    // Ana paneldeki tüm bileşenleri bir dizi olarak alır
                                    Component[] components = mainPanel.getComponents();
                                    JPanel sectionPanel = (JPanel) components[sectionIndex];
                                    JLabel durationLabel = new JLabel("Tamamlandı " + duration + " ms"); 
                                    sectionPanel.add(durationLabel);
                                    // Panelin yerleşimini yeniden hesaplar
                                    sectionPanel.revalidate();
                                    // Paneli yeniden çizer
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


     
    // Dosyayı belirtilen formata dönüştüren metot
private void convertFile(File file, String format) {
    long startTime = System.currentTimeMillis(); // Başlangıç zamanını al
    try {
        logger.info("Dönüştürme işlemi başladı: " + file.getName()); // Loglama yap

        String sanitizedFileName = sanitizeFileName(file.getName()); 
        String inputExtension = getFileExtension(file); 
        Path outputDir = Paths.get(System.getProperty("user.home"), "Desktop", "dönüştürüldü"); // Çıktı dizinini belirle
        Files.createDirectories(outputDir); // Çıktı dizinini oluştur

        String outputFileName = "converted_" + sanitizedFileName.replace("." + inputExtension, "." + format.toLowerCase()); // Çıktı dosya adını oluştur
        Path targetPath = outputDir.resolve(outputFileName); // Çıktı dosyasının yolunu belirle

        // Eğer dosya PDF formatına dönüştürülüyorsa ve resim dosyasıysa
        if (format.equalsIgnoreCase("PDF") && isImageFile(inputExtension)) {
            convertImageToPdf(file, targetPath.toString()); // Resmi PDF'ye dönüştür
        } else {
            List<String> command = new ArrayList<>(); // Komut listesi oluştur
            command.add("C:\\ffmpeg\\ffmpeg.exe"); // FFmpeg yürütülebilir dosyasını ekle
            command.add("-i"); // Girdi dosyasını belirle
            command.add(file.getAbsolutePath()); // Girdi dosyasının yolunu ekle

            // Eğer ses dosyasıysa
            if (isAudioFile(inputExtension)) {
                command.add("-acodec"); // Ses kodlayıcıyı belirle
                if (format.equalsIgnoreCase("mp3")) {
                    command.add("libmp3lame"); // MP3 kodlayıcı
                    command.add("-b:a");
                    command.add("192k"); // Bit hızı
                } else if (format.equalsIgnoreCase("wav")) {
                    command.add("pcm_s16le"); // WAV kodlayıcı
                } else {
                    command.add("copy"); 
                }
            // Eğer video dosyasıysa
            } else if (isVideoFile(inputExtension)) {
                command.add("-c:v"); // Video kodlayıcıyı belirle
                command.add("libx264"); 
                command.add("-c:a"); // Ses kodlayıcıyı belirle
                command.add("aac"); 
            // Eğer resim dosyasıysa
            } else if (isImageFile(inputExtension)) {
                command.add("-vf"); // Video filtresi belirle
                command.add("scale=iw:ih"); // Ölçeklendirme filtresi
            }

            command.add(targetPath.toString()); // Çıktı dosyasının yolunu ekle

            ProcessBuilder builder = new ProcessBuilder(command); // ProcessBuilder oluştur
            builder.redirectErrorStream(true); 
            Process process = builder.start(); // FFmpeg işlemini başlat

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())); // Standart çıktıyı oku
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("FFmpeg output: " + line); 
            }

            int exitCode = process.waitFor(); // FFmpeg işleminin bitmesini bekle
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg işlemi kodla sonlandırıldı " + exitCode); // Hata durumunda istisna fırlat
            }
        }

        // Dönüştürme başarılı olduğunda kullanıcıya bildirim göster
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, 
            "Başarıyla dönüştürüldü: " + file.getName(), 
            "Dönüştürme Başarılı", JOptionPane.INFORMATION_MESSAGE));
    } catch (Exception e) {
        logger.severe("Dönüştürme sırasında hata oluştu: " + e.getMessage()); // Hata logla
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
            "Dönüştürme sırasında hata oluştu: " + e.getMessage(),
            "Hata", JOptionPane.ERROR_MESSAGE)); // Hata durumunda kullanıcıya bildirim göster
        e.printStackTrace();
    }
}


// Ses dosyası olup olmadığını kontrol eder
private boolean isAudioFile(String extension) {
    // Uzantı "mp3" veya "wav" veya "mp4" veya "wma" ise true döner
    return extension.equalsIgnoreCase("mp3") || extension.equalsIgnoreCase("wav") ||
           extension.equalsIgnoreCase("mp4") || extension.equalsIgnoreCase("wma");
}

// Video dosyası olup olmadığını kontrol eder
private boolean isVideoFile(String extension) {
    // Uzantı "mp4" veya "mkv" veya "mov" ise true döner
    return extension.equalsIgnoreCase("mp4") || 
           extension.equalsIgnoreCase("mkv") || extension.equalsIgnoreCase("mov");
}

// Resim dosyası olup olmadığını kontrol eder
private boolean isImageFile(String extension) {
    // Uzantı "jpg" veya "jpeg" veya "png" veya "pdf" veya "bmp" ise true döner
    return extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg") ||
           extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("pdf") ||
           extension.equalsIgnoreCase("bmp");
}


    // Dosyanın türünü belirleyen metot
private String getFileType(File file) {
    // Dosyanın uzantısını küçük harflerle al
    String extension = getFileExtension(file).toLowerCase();
    
    // Eğer dosya türü resimse "image" döner
    if (FORMAT_OPTIONS.get("image") != null && FORMAT_OPTIONS.get("image").length > 0 && 
        FORMAT_OPTIONS.get("image")[0].toLowerCase().contains(extension)) {
        return "image";
    // Eğer dosya türü videoysa "video" döner
    } else if (FORMAT_OPTIONS.get("video") != null && FORMAT_OPTIONS.get("video").length > 0 && 
               FORMAT_OPTIONS.get("video")[0].toLowerCase().contains(extension)) {
        return "video";
    // Eğer dosya türü ses dosyasıysa "audio" döner
    } else if (FORMAT_OPTIONS.get("audio") != null && FORMAT_OPTIONS.get("audio").length > 0 && 
               FORMAT_OPTIONS.get("audio")[0].toLowerCase().contains(extension)) {
        return "audio";
    }
    // Eğer dosya türü belirlenemiyorsa "unknown" döner
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
    //uzantı belirleme
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