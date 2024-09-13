package Hoechst_KI67_Tools;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.ImageIcon;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.plugins.util.ImageProcessorReader;
import mcib3d.geom2.Object3DInt;
import mcib3d.image3d.ImageHandler;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import org.apache.commons.io.FilenameUtils;


/**
 * @author ORION-CIRB
 */ 
public class Tools {
    private final ImageIcon icon = new ImageIcon(this.getClass().getResource("/Orion_icon.png"));
    private final String helpUrl = "https://github.com/orion-cirb/Hoechst_KI67";
    private CLIJ2 clij2 = CLIJ2.getInstance();
    
    private String[] chDialog = {"Hoechst", "KI67"};
    private Calibration cal;
    private double pixVol;
    
    // Hoechst segmentation
    public String nucThMethod = "Huang";
    
    // KI67 segmentation
    public String ki67ThMethod = "Otsu";
    
    // Estimation of nuclei number
    public double nucMeanVol = 125;

    
    /**
     * Display a message in the ImageJ console and status bar
     */
    public void print(String log) {
        System.out.println(log);
        IJ.showStatus(log);
    }
    
    
    /**
     * Flush and close an image
     */
    public void closeImage(ImagePlus img) {
        img.flush();
        img.close();
    }
    
    
    /**
     * Check that needed modules are installed
     */
    public boolean checkInstalledModules() {
        ClassLoader loader = IJ.getClassLoader();
        try {
            loader.loadClass("mcib3d.geom2.Object3DInt");
        } catch (ClassNotFoundException e) {
            IJ.log("3D ImageJ Suite not installed, please install from update site");
            return false;
        }
        try {
            loader.loadClass("net.haesleinhuepf.clij2.CLIJ2");
        } catch (ClassNotFoundException e) {
            IJ.log("CLIJ not installed, please install from update site");
            return false;
        }
        return true;
    }
    
    
    /**
     * Get extension of the first image found in the folder
     */
    public String findImageType(File imagesFolder) {
        String ext = "";
        String[] files = imagesFolder.list();
        for (String name : files) {
            String fileExt = FilenameUtils.getExtension(name);
            switch (fileExt) {
                case "nd" :
                   ext = fileExt;
                   break;
                case "nd2" :
                   ext = fileExt;
                   break;
                case "lif"  :
                    ext = fileExt;
                    break;
                case "czi" :
                   ext = fileExt;
                   break;
                case "ics" :
                    ext = fileExt;
                    break;
                case "ics2" :
                    ext = fileExt;
                    break;
                case "lsm" :
                    ext = fileExt;
                    break;
                case "tif" :
                    ext = fileExt;
                    break;
                case "tiff" :
                    ext = fileExt;
                    break;
            }
        }
        return(ext);
    }
    
    
    /**
     * Get images with given extension in folder
     */
    public ArrayList<String> findImages(String imagesFolder, String imageExt) {
        File inDir = new File(imagesFolder);
        String[] files = inDir.list();
        ArrayList<String> images = new ArrayList();
        for (String f : files) {
            String fileExt = FilenameUtils.getExtension(f);
            if (fileExt.equals(imageExt) && !f.startsWith("."))
                images.add(imagesFolder + f);
        }
        Collections.sort(images);
        return(images);
    }
       
    
    /**
     * Get image calibration
     */
    public void findImageCalib(IMetadata meta) {
        cal = new Calibration();
        cal.pixelWidth = meta.getPixelsPhysicalSizeX(0).value().doubleValue();
        cal.pixelHeight = cal.pixelWidth;
        if (meta.getPixelsPhysicalSizeZ(0) != null)
            cal.pixelDepth = meta.getPixelsPhysicalSizeZ(0).value().doubleValue();
        else
            cal.pixelDepth = 1;
        cal.setUnit("microns");
        System.out.println("XY calibration = " + cal.pixelWidth + ", Z calibration = " + cal.pixelDepth);
    }
    
    
    /**
     * Get channels name and add None at the end of channels list
     * @throws loci.common.services.DependencyException
     * @throws loci.common.services.ServiceException
     * @throws loci.formats.FormatException
     * @throws java.io.IOException
     */
    public String[] findChannels(String imageName, IMetadata meta, ImageProcessorReader reader) throws DependencyException, ServiceException, FormatException, IOException {
        int chs = reader.getSizeC();
        String[] channels = new String[chs];
        String imageExt =  FilenameUtils.getExtension(imageName);
        switch (imageExt) {
            case "nd" :
                for (int n = 0; n < chs; n++) 
                    channels[n] = (meta.getChannelName(0, n).toString().equals("")) ? Integer.toString(n) : meta.getChannelName(0, n).toString();
                break;
            case "nd2" :
                for (int n = 0; n < chs; n++) 
                    channels[n] = (meta.getChannelName(0, n).toString().equals("")) ? Integer.toString(n) : meta.getChannelName(0, n).toString();
                break;
            case "lif" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null || meta.getChannelName(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelName(0, n).toString();
                break;
            case "czi" :
                for (int n = 0; n < chs; n++) 
                    channels[n] = (meta.getChannelFluor(0, n).toString().equals("")) ? Integer.toString(n) : meta.getChannelFluor(0, n).toString();
                break;
            case "ics" :
                for (int n = 0; n < chs; n++) 
                    channels[n] = meta.getChannelEmissionWavelength(0, n).value().toString();
                break;    
            case "ics2" :
                for (int n = 0; n < chs; n++) 
                    channels[n] = meta.getChannelEmissionWavelength(0, n).value().toString();
                break; 
            default :
                for (int n = 0; n < chs; n++)
                    channels[n] = Integer.toString(n);
        }
        return(channels);     
    }
    
    
    /**
     * Generate dialog box
     */
    public String[] dialog(String[] chMeta) {      
        GenericDialogPlus gd = new GenericDialogPlus("Parameters");
        gd.setInsets​(0, 80, 0);
        gd.addImage(icon);
        
        gd.addMessage("Channels", new Font("Monospace", Font.BOLD, 12), Color.blue);
        for (int n = 0; n < chDialog.length; n++)
            gd.addChoice(chDialog[n]+": ", chMeta, chMeta[n]);

        gd.addMessage("Hoechst segmentation", Font.getFont("Monospace"), Color.blue);
        String[] thMethods = AutoThresholder.getMethods();
        gd.addChoice("Thresholding method: ", thMethods, nucThMethod);
        
        gd.addMessage("KI67 segmentation", Font.getFont("Monospace"), Color.blue);
        gd.addChoice("Thresholding method: ", thMethods, ki67ThMethod);
        
        gd.addMessage("Nuclei estimated number", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Nucleus mean volume (µm3): ", nucMeanVol);
        
        gd.addMessage("Image calibration", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("XY pixel size (µm): ", cal.pixelWidth, 4);
        gd.addNumericField("Z pixel size (µm): ", cal.pixelDepth, 4);
        
        gd.addHelp(helpUrl);
        gd.showDialog();
        
        String[] chOrder = new String[chDialog.length];
        for (int n = 0; n < chOrder.length; n++)
            chOrder[n] = gd.getNextChoice();
       
        nucThMethod = gd.getNextChoice();
        
        ki67ThMethod = gd.getNextChoice();
        
        nucMeanVol = gd.getNextNumber();
        
        cal.pixelWidth = cal.pixelHeight = gd.getNextNumber();
        cal.pixelDepth = gd.getNextNumber();
        pixVol = cal.pixelWidth * cal.pixelHeight * cal.pixelDepth;
        
        if (gd.wasCanceled())
            chOrder = null;    
        return(chOrder);
    }
    
    
    /**
     * Segment stack slice by slice
     */
    public ImagePlus segmentation(ImagePlus img, String thMethod) {
        ImagePlus imgSub = img.duplicate();
        IJ.run(imgSub, "Subtract Background...", "rolling=100 sliding stack");
        ImagePlus imgMed = median2D(imgSub, 10);
        ImagePlus imgTh = threshold(imgMed, thMethod);
        ImagePlus imgOut = median2D(imgTh, 3);
        imgOut.setCalibration(cal);
        
        closeImage(imgSub);
        closeImage(imgMed);
        closeImage(imgTh);
        return(imgOut);
    }
        
    
    /**
     * 2D median filtering using CLIJ2
     */ 
    public ImagePlus median2D(ImagePlus img, double sizeXY) {
       ClearCLBuffer imgCL = clij2.push(img); 
       ClearCLBuffer imgCLMed = clij2.create(imgCL);
       clij2.median3DSliceBySliceSphere(imgCL, imgCLMed, sizeXY, sizeXY);
       ImagePlus imgMed = clij2.pull(imgCLMed);
       clij2.release(imgCL);
       clij2.release(imgCLMed);
       return(imgMed);
    }
    
    
    /**
     * Automatic thresholding using CLIJ2
     */
    public ImagePlus threshold(ImagePlus img, String thMed) {
        ClearCLBuffer imgCL = clij2.push(img);
        ClearCLBuffer imgCLBin = clij2.create(imgCL);
        clij2.automaticThreshold(imgCL, imgCLBin, thMed);
        ImagePlus imgBin = clij2.pull(imgCLBin);
        clij2.release(imgCL);
        clij2.release(imgCLBin);
        return(imgBin);
    }
    
    
    /**
     * Load ROIs, if any provided
     */
    public List<Roi> loadRois(String imgDir, String imgName, ImagePlus img) {
        List<Roi> rois = new ArrayList<>();
        
        String roiName = imgDir+imgName;
        roiName = new File(roiName+".zip").exists() ? roiName+".zip" : roiName+".roi";
        if (new File(roiName).exists()) {
            RoiManager rm = new RoiManager(false);
            rm.runCommand("Open", roiName);
            rois = Arrays.asList(rm.getRoisAsArray());
        } else {
            Roi roi = new Roi(0, 0, img.getWidth(), img.getHeight());
            roi.setName("entire image");
            rois.add(roi);
            System.out.println("WARNING: No ROI file found for image " + imgName + ", entire image is analyzed.");
        }

        return(rois);
    }
    
    
    /**
     * Compute ROI volume
     */
    public double getRoiVolume(Roi roi, ImagePlus img) {    
        img.setRoi(roi);
        img.setCalibration(cal);
        
        ResultsTable rt = new ResultsTable();
        Analyzer analyzer = new Analyzer(img, Analyzer.AREA, rt);
        analyzer.measure();
        double area = rt.getValue("Area", 0);
        
        img.deleteRoi();
        return(area * img.getNSlices() * cal.pixelDepth);
    }
    
    
    /**
     * Get object in ROI
     */
    public Object3DInt getObjInRoi(ImagePlus img, Roi roi) {
        ImagePlus imgClear = img.duplicate();
        imgClear.setRoi(roi);
        
        IJ.setBackgroundColor(0, 0, 0);
        IJ.run(imgClear, "Clear Outside", "stack");        
        imgClear.setCalibration(cal);
        Object3DInt obj = new Object3DInt(ImageHandler.wrap(imgClear));
        
        closeImage(imgClear);
        return(obj);
    } 
    
    
    /*
     * Save resulting image
     */
    public void saveResults(ImagePlus imgNuc, ImageHandler imhNuc, ImagePlus imgKI67, ImageHandler imhKI67, String imgDir, String imgName) {
        ImagePlus[] imgColors = {null, imhKI67.getImagePlus(), imhNuc.getImagePlus(), imgKI67, imgNuc};
        ImagePlus imgOut = new RGBStackMerge().mergeHyperstacks(imgColors, false);
        imgOut.setCalibration(cal);
        new FileSaver(imgOut).saveAsTiff(imgDir + imgName + ".tif"); 
        closeImage(imgOut);
    }
    
}
