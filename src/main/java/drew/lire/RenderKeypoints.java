package drew.lire;

import net.semanticmetadata.lire.utils.FileUtils;
import nu.pattern.OpenCV;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

@SpringBootApplication
public class RenderKeypoints {

    private List<String> imageFiles;
    private int pos = 0;
    private JFrame displayFrame;
    private boolean keypointsEnabled = true;

    public RenderKeypoints(String path) throws IOException {
        OpenCV.loadShared();
        this.imageFiles = FileUtils.getAllImages(new File(path), false);
        displayCurrentImage();
    }

    /** Load, Render keypoints on an image and display that image */
    protected void renderKeypointsAndDisplay(String path) throws IOException {
        BufferedImage image = renderKeypoints(path);
        showImage(image);
    }

    /** Load and display the specified image */
    protected void simplyDisplay(String path) throws IOException {
        BufferedImage image = loadImage(path);
        showImage(image);
    }


    /** display the current image in a JFrame */
    protected void displayCurrentImage() {
        try {
            if (keypointsEnabled) {
                renderKeypointsAndDisplay(imageFiles.get(pos));
            }
            else {
                simplyDisplay(imageFiles.get(pos));
            }
        }
        catch (IOException ioe) {
            System.err.println("Error loading next image");
        }
    }

    /** toggle whether we're just showing the image or generating and displaying keypoints */
    protected void toggleKeypoints() {
        this.keypointsEnabled = ! keypointsEnabled;
    }

    /** switch to the next image */
    protected void switchImage() {
        pos++;
        if (pos >= imageFiles.size()) {
            pos = 0;
        }
    }

    /** create the JFrame used to display images */
    protected void createFrame() {
        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.displayFrame = frame;
    }

    /** show the specified impage, generate the image icon and render to the JFrame,
     *  add handlers for interactivity
     * @param image
     */
    protected void showImage(BufferedImage image) {
        if (displayFrame == null) {
            createFrame();
        }

        final JLabel textLabel = new JLabel("Press space to toggle keypoints, click to go to next image");
        final JLabel imageLabel = new JLabel(new ImageIcon(image));
        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                switchImage();
                displayCurrentImage();
            }
        });
        imageLabel.getInputMap().put(KeyStroke.getKeyStroke("SPACE"),"spacebar");
        imageLabel.getActionMap().put("spacebar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleKeypoints();
                displayCurrentImage();
            }
        });

        displayFrame.setVisible(false);
        displayFrame.getContentPane().removeAll();;
        displayFrame.getContentPane().add(imageLabel);
        displayFrame.getContentPane().add(textLabel);
        displayFrame.pack();
        displayFrame.setVisible(true);
    }

    public BufferedImage loadImage(String path) throws IOException {
        BufferedImage img = ImageIO.read(new FileInputStream(path));
        return img;
    }

    public BufferedImage renderKeypoints(String path) throws IOException {
        BufferedImage img = loadImage(path);

        FeatureDetector detector = FeatureDetector.create(4);
        DescriptorExtractor extractor = DescriptorExtractor.create(2);

        // convert to greyscale
        byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        Mat matRGB = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
        matRGB.put(0, 0, data);
        Mat matGray = new Mat(img.getHeight(),img.getWidth(),CvType.CV_8UC1);
        Imgproc.cvtColor(matRGB, matGray, Imgproc.COLOR_BGR2GRAY);

        // detect the interesting features.
        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        detector.detect(matGray, keypoints);

        // draw the features on the original image
        Features2d.drawKeypoints(matGray, keypoints, matRGB, Scalar.all(-1), Features2d.DRAW_RICH_KEYPOINTS);

        // convert the Mat to a Buffered Image
        BufferedImage output = new BufferedImage(matRGB.width(), matRGB.height(), BufferedImage.TYPE_3BYTE_BGR);
        byte[] outputData = ((DataBufferByte) output.getRaster().getDataBuffer()).getData();
        matRGB.get(0, 0, outputData);

        return output;
    }

    public static void main(String[] args) throws IOException {
        //ApplicationContext ctx = new SpringApplicationBuilder(RenderKeypoints.class).headless(false).run(args);
        String path = "src/main/data/photos/testimages/";
        RenderKeypoints k = new RenderKeypoints(path);
    }
}
