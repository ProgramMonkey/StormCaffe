package test;

import java.io.*;
import org.bytedeco.javacpp.*;
import static org.bytedeco.javacpp.hdf5.*;

public class Hdf5 {
    static final String FILE_NAME = "h5tutr_cmprss.h5";
    static final String DATASET_NAME = "Compressed_Data";
    static final int DIM0 = 100;
    static final int DIM1 = 20;

    public static void main(String[] args) {
        long[] dims = { DIM0, DIM1 };        // dataset dimensions
        long[] chunk_dims = { 20, 20 };        // chunk dimensions
        int[] buf = new int[DIM0 * DIM1];

        // Try block to detect exceptions raised by any of the calls inside it
        try {
            // Turn off the auto-printing when failure occurs so that we can
            // handle the errors appropriately
            org.bytedeco.javacpp.hdf5.Exception.dontPrint();

            // Create a new file using the default property lists.
            H5File file = new H5File(FILE_NAME, H5F_ACC_TRUNC);

            // Create the data space for the dataset.
            DataSpace dataspace = new DataSpace(2, dims);

            // Modify dataset creation property to enable chunking
            DSetCreatPropList plist = new DSetCreatPropList();
            plist.setChunk(2, chunk_dims);

            // Set ZLIB (DEFLATE) Compression using level 6.
            // To use SZIP compression comment out this line.
            plist.setDeflate(6);

            // Uncomment these lines to set SZIP Compression
            // unsigned szip_options_mask = H5_SZIP_NN_OPTION_MASK;
            // unsigned szip_pixels_per_block = 16;
            // plist->setSzip(szip_options_mask, szip_pixels_per_block);

            // Create the dataset.
            DataSet dataset = new DataSet(file.asCommonFG().createDataSet(DATASET_NAME,
                    new DataType(PredType.STD_I32BE()), dataspace, plist));

            for (int i = 0; i <  DIM0; i++)
                for (int j = 0; j < DIM1; j++)
                    buf[i * DIM1 + j] = i + j;

            // Write data to dataset.
            dataset.write(new IntPointer(buf), new DataType(PredType.NATIVE_INT()));

            // Close objects and file.  Either approach will close the HDF5 item.
            dataspace.close();
            dataset.close();
            plist.close();
            file.close();

            // -----------------------------------------------
            // Re-open the file and dataset, retrieve filter 
            // information for dataset and read the data back.
            // -----------------------------------------------

            int[] rbuf = new int[DIM0 * DIM1];
            int numfilt;
            long nelmts = 1, namelen = 1;
            int[] flags = new int[1], filter_info = new int[1], cd_values = new int[1];
            byte[] name = new byte[1];
            int filter_type;

            // Open the file and the dataset in the file.
            file = new H5File();
            file.openFile(FILE_NAME, H5F_ACC_RDONLY);
            dataset = new DataSet(file.asCommonFG().openDataSet(DATASET_NAME));

            // Get the create property list of the dataset.
            plist = new DSetCreatPropList(dataset.getCreatePlist());

            // Get the number of filters associated with the dataset.
            numfilt = plist.getNfilters();
            System.out.println("Number of filters associated with dataset: " + numfilt);

            for (int idx = 0; idx < numfilt; idx++) {
                nelmts = 0;

                filter_type = plist.getFilter(idx, flags, new SizeTPointer(1).put(nelmts), cd_values, namelen, name, filter_info);

                System.out.print("Filter Type: ");

                switch (filter_type) {
                    case H5Z_FILTER_DEFLATE:
                        System.out.println("H5Z_FILTER_DEFLATE");
                        break;
                    case H5Z_FILTER_SZIP:
                        System.out.println("H5Z_FILTER_SZIP");
                        break;
                    default:
                        System.out.println("Other filter type included.");
                }
            }

            // Read data.
            IntPointer p = new IntPointer(rbuf);
            dataset.read(p, PredType.NATIVE_INT());
            p.get(rbuf);

            plist.close();
            dataset.close();
            file.close();        // can be skipped

        }  // end of try block

        // catch failure caused by the H5File, DataSet, and DataSpace operations
        catch (RuntimeException error) {
            System.err.println(error);
            error.printStackTrace();
            System.exit(-1);
        }

        System.out.println("Finished!");
        System.exit(0);  // successfully terminated
    }
}