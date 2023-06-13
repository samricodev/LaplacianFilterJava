package filtrolaplaciano;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.*;

public class WindowMain extends JFrame {

    JLabel title, upload, originalImg, modifiedImg, timeSeq, timeFork, timeExec, times;
    JButton btnSequential, btnForkJoin, btnExecutor, btnClear, btnUpload;
    JFileChooser chooser;
    Laplace laplacian;

    public WindowMain() {

        //Window
        setLayout(null);
        setSize(700, 600);
        setResizable(false);
        setLocationRelativeTo(null);
        setBackground(Color.lightGray);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        //Components
        laplacian = new Laplace();

        //Top
        title = new JLabel("Filtro Laplaciano");
        title.setBounds(300, 5, 200, 20);
        add(title);

        upload = new JLabel("Upload image ");
        upload.setBounds(20, 50, 100, 20);
        add(upload);

        //Center
        originalImg = new JLabel();
        originalImg.setBounds(10, 150, 300, 300);
        add(originalImg);

        modifiedImg = new JLabel();
        modifiedImg.setBounds(375, 150, 300, 300);
        add(modifiedImg);

        //Execution times
        times = new JLabel("Times: ");
        times.setBounds(10, 100, 50, 30);
        add(times);

        timeSeq = new JLabel();
        timeSeq.setBounds(80, 100, 150, 30);
        add(timeSeq);

        timeFork = new JLabel();
        timeFork.setBounds(250, 100, 150, 30);
        add(timeFork);

        timeExec = new JLabel();
        timeExec.setBounds(400, 100, 150, 30);
        add(timeExec);

        //Buttons
        //Button UpLoad
        btnUpload = new JButton("Upload");
        btnUpload.setBounds(120, 50, 80, 20);
        btnUpload.addActionListener(e -> upLoadImage());
        add(btnUpload);

        //Button Sequential
        btnSequential = new JButton("Sequential");
        btnSequential.setBounds(20, 500, 100, 30);
        btnSequential.addActionListener(e -> funcSequential());
        add(btnSequential);

        //Button ForkJoin
        btnForkJoin = new JButton("ForkJoin");
        btnForkJoin.setBounds(200, 500, 100, 30);
        btnForkJoin.addActionListener(e -> funcFork());
        add(btnForkJoin);

        //Button Executor
        btnExecutor = new JButton("Executor");
        btnExecutor.setBounds(400, 500, 100, 30);
        btnExecutor.addActionListener(e -> {
            try {
                funcExec();
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(WindowMain.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        add(btnExecutor);

        //Button Clear
        btnClear = new JButton("Clear");
        btnClear.setBounds(550, 500, 100, 30);
        btnClear.addActionListener(e -> clear());
        add(btnClear);

    }

    //ActionListener Methods
    public void upLoadImage() {
        String directorioActual = System.getProperty("user.dir");
        chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(directorioActual+"/src/images"));
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            ImageIcon image = new ImageIcon("src/images/" + chooser.getSelectedFile().getName());
            originalImg.setIcon(new ImageIcon(image.getImage().getScaledInstance(300, 300, Image.SCALE_SMOOTH)));
        }
        
    }

    public void funcSequential() {
        String url = "src/images/" + chooser.getSelectedFile().getName();
        try {
            long inicio = System.currentTimeMillis();
            laplacian.imageConvert(url);
            ImageIcon imgM = new ImageIcon("src/images/newImage.jpg");
            long fin = System.currentTimeMillis();
            timeSeq.setText("Sequential: " + (fin - inicio) + " ms");
            modifiedImg.setIcon(new ImageIcon(imgM.getImage().getScaledInstance(300, 300, Image.SCALE_SMOOTH)));
        } catch (IOException ex) {
            Logger.getLogger(WindowMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void funcFork() {
        try {
            String url = "src/images/" + chooser.getSelectedFile().getName();
            BufferedImage imageFork = ImageIO.read(new File(url));
            BufferedImage filteredImageFork = new BufferedImage(imageFork.getWidth(), imageFork.getHeight(), BufferedImage.TYPE_INT_RGB);
            ForkJoinPool forkJoinPool = new ForkJoinPool(2);
            LaplaceForkJoin task = new LaplaceForkJoin(imageFork, filteredImageFork, 1, imageFork.getWidth() - 1, 1, imageFork.getHeight() - 1);
            long inicio = System.currentTimeMillis();
            forkJoinPool.invoke(task);
            long fin = System.currentTimeMillis();
            timeFork.setText("ForkJoin: " + (fin - inicio) + " ms");
            File archivoSalida = new File("src/images/newImage.jpg");
            ImageIO.write(filteredImageFork, "jpg", archivoSalida);
            ImageIcon imgM = new ImageIcon("src/images/newImage.jpg");
            modifiedImg.setIcon(new ImageIcon(imgM.getImage().getScaledInstance(300, 300, Image.SCALE_SMOOTH)));
        } catch (IOException ex) {
            Logger.getLogger(WindowMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void funcExec() throws IOException, InterruptedException {
        String url = "src/images/" + chooser.getSelectedFile().getName();
        BufferedImage image = ImageIO.read(new File(url));
        BufferedImage filteredImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        // Crear ExecutorService con el número de núcleos del procesador
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        // Crear un conjunto de tareas y enviarlas a ExecutorService
        int tileSize = 16;
        for (int y = 1; y < image.getHeight() - 1; y += tileSize) {
            for (int x = 1; x < image.getWidth() - 1; x += tileSize) {
                int xStart = x;
                int xEnd = Math.min(x + tileSize, image.getWidth() - 1);
                int yStart = y;
                int yEnd = Math.min(y + tileSize, image.getHeight() - 1);
                executorService.execute(new LaplaceExecutor(image, filteredImage, xStart, xEnd, yStart, yEnd));
            }
        }
        
        long inicio = System.currentTimeMillis();
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        long fin = System.currentTimeMillis();
        timeExec.setText("Executor: " + (fin - inicio) + " ms");

        // Guardar la imagen filtrada
        File archivoSalida = new File("src/images/newImage.jpg");
        ImageIO.write(filteredImage, "jpg", archivoSalida);

        //Asignarla al label
        ImageIcon imgM = new ImageIcon("src/images/newImage.jpg");
        modifiedImg.setIcon(new ImageIcon(imgM.getImage().getScaledInstance(300, 300, Image.SCALE_SMOOTH)));
    }

    public void clear() {
        timeSeq.setText(null);
        timeFork.setText(null);
        timeExec.setText(null);
        originalImg.setIcon(null);
        modifiedImg.setIcon(null);
        File fileImage = new File("src/images/newImage.jpg");
        fileImage.delete();
    }
}
