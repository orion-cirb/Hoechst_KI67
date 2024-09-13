# Hoechst_KI67

* **Developed by:** Héloïse
* **Developed for:** Beetsi
* **Team:** Selimi
* **Date:** September 2024
* **Software:** Fiji

### Images description

3D images taken on spinning-disk W1 Zeiss with a x63 objective

2 channels:
  1. *CSU_405*: Hoechst nuclei
  2. *CSU_642*: KI67-positive nuclei 

With each image can be provided a *.roi* or *.zip* file containing one or multiple ROI(s).

### Plugin description

* Segment Hoechst and KI67 channels using background subtraction + median filter + automatic threshold + median filter 
* Count nuclei in each channel by calculating the total volume of packed nuclei and dividing it by the estimated volume of a single nucleus
* If an ROI is provided, perform the analysis only within that region

### Dependencies

* **3DImageSuite** Fiji plugin
* **CLIJ** Fiji plugin

### Version history

Version 1 released on September 13, 2024.
