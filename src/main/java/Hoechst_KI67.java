import Hoechst_KI67_Tools.Tools;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import loci.plugins.util.ImageProcessorReader;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.measurements.MeasureVolume;
import mcib3d.image3d.ImageHandler;
import org.apache.commons.io.FilenameUtils;
import org.scijava.util.ArrayUtils;

/**
 * @author ORION-CIRB
 */ 
public class Hoechst_KI67 implements PlugIn {
    
    Tools tools = new Tools();
    
    public void run(String arg) {
        try {
            if (!tools.checkInstalledModules()) {
                return;
            }
            
            String imageDir = IJ.getDirectory("Choose images directory");
            if (imageDir == null) {
                return;
            }
            
            // Find extension of first image in input folder
            String fileExt = tools.findImageType(new File(imageDir));
            // Find all images with corresponding extension in folder
            ArrayList<String> imageFiles = tools.findImages(imageDir, fileExt);
            if (imageFiles.isEmpty()) {
                IJ.showMessage("ERROR", "No image found with " + fileExt + " extension in " + imageDir + " directory");
                return;
            }
            
            // Instantiate metadata and reader
            IMetadata meta = MetadataTools.createOMEXMLMetadata();
            ImageProcessorReader reader = new ImageProcessorReader();
            reader.setMetadataStore(meta);
            reader.setId(imageFiles.get(0));
            
            // Find image calibration
            tools.findImageCalib(meta);

            // Find channels name
            String[] chMeta = tools.findChannels(imageFiles.get(0), meta, reader);
            
            // Generate dialog box
            String[] chOrder = tools.dialog(chMeta);
            if (chOrder == null) {
                return;
            }

            // Create output folder
            String outDir = imageDir + File.separator + "Results_" + tools.nucThMethod + "_" + tools.ki67ThMethod + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + File.separator;
            if (!Files.exists(Paths.get(outDir))) {
                new File(outDir).mkdir();
            }
            
            // Write header in results file
            FileWriter fwResults = new FileWriter(outDir + "results.csv", false);
            BufferedWriter results = new BufferedWriter(fwResults);
            results.write("Image name\tROI name\tROI volume (µm3)\tHoechst total volume (µm3)\tHoechst estimated nb\tKI67 total volume (µm3)\tKI67 estimated nb\n");
            results.flush();
            
            for (String f: imageFiles) {
                String rootName = FilenameUtils.getBaseName(f);
                tools.print("--- ANALYZING IMAGE " + rootName + " ---");
                reader.setId(f);
                
                ImporterOptions options = new ImporterOptions();
                options.setId(f);
                options.setSplitChannels(true);
                options.setQuiet(true);
                options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
               
                // Open Hoechst channel
                tools.print("- Opening Hoechst channel -");
                int index = ArrayUtils.indexOf(chMeta, chOrder[0]);
                ImagePlus imgNuc = BF.openImagePlus(options)[index];

                // Segment Hoechst channel
                tools.print("- Segmenting Hoechst channel -");
                ImagePlus segNuc = tools.segmentation(imgNuc, tools.nucThMethod);

                // Open KI67 channel
                tools.print("- Opening KI67 channel -");
                index = ArrayUtils.indexOf(chMeta, chOrder[1]);
                ImagePlus imgKI67 = BF.openImagePlus(options)[index];

                // Segment Hoechst channel
                tools.print("- Segmenting KI67 channel -");
                ImagePlus segKI67 = tools.segmentation(imgKI67, tools.ki67ThMethod);
                
                // Load ROIs, if any provided
                tools.print("- Loading ROIs -");
                List<Roi> rois = tools.loadRois(imageDir, rootName, imgNuc);
                
                ImageHandler imhNuc = ImageHandler.wrap(imgNuc).createSameDimensions();
                ImageHandler imhKI67 = imhNuc.createSameDimensions();
                
                for (Roi roi : rois) {
                    String roiName = roi.getName();
                    tools.print("- Saving results for ROI " + roiName + " -");
                    
                    Object3DInt objNucRoi = tools.getObjInRoi(segNuc, roi);
                    Object3DInt objKI67Roi = tools.getObjInRoi(segKI67, roi);

                    // Draw results
                    objNucRoi.drawObject(imhNuc, 255);
                    objKI67Roi.drawObject(imhKI67, 255);
                    
                    // Compute ROI and masks volume
                    double volRoi = tools.getRoiVolume(roi, imgNuc);
                    double volNuc = new MeasureVolume(objNucRoi).getVolumeUnit();
                    double volKI67 = new MeasureVolume(objKI67Roi).getVolumeUnit();
                    
                    // Write results
                    results.write(rootName+"\t"+roiName+"\t"+volRoi+"\t"+volNuc+"\t"+(int)Math.round(volNuc/tools.nucMeanVol)+
                                  "\t"+volKI67+"\t"+(int)Math.round(volKI67/tools.nucMeanVol)+"\n");
                    results.flush();
                }
                // Save resulting image
                tools.saveResults(imgNuc, imhNuc, imgKI67, imhKI67, outDir, rootName);
                
                tools.closeImage(imgNuc);
                tools.closeImage(segNuc);
                tools.closeImage(imgKI67);
                tools.closeImage(segKI67);
                tools.closeImage(imhNuc.getImagePlus());
                tools.closeImage(imhKI67.getImagePlus());
            }
            results.close();
            tools.print("--- All done! ---");
        } catch (DependencyException | ServiceException | IOException | FormatException ex) {
            Logger.getLogger(Hoechst_KI67.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
