package airDataBackendService.interpolation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class MatReader {
  private MatReader() {
  };

  public static double[][][] read(File file) {
    MatFileReader matfilereader;

    double[][][] data = new double[2][][];

    try {
      matfilereader = new MatFileReader(file);
      MatFileReader.MLDouble mP1 = matfilereader.getMLArray("grid_P1");
      MatFileReader.MLDouble mP2 = matfilereader.getMLArray("grid_P2");

      data[0] = mP1.getArray();
      data[1] = mP2.getArray();
      return data;

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;

    }

  }

  public static double[][][] read(InputStream str) {
    MatFileReader matfilereader;

    double[][][] data = new double[2][][];

    try {
      matfilereader = new MatFileReader(str);
      MatFileReader.MLDouble mP1 = matfilereader.getMLArray("grid_P1");
      MatFileReader.MLDouble mP2 = matfilereader.getMLArray("grid_P2");

      data[0] = mP1.getArray();
      data[1] = mP2.getArray();
      return data;

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;

    }

  }

  public static double[][][] meshGrid() {
    double[][][] meshGrid = new double[2][][];

    double[] latGrid = new double[2751];
    meshGrid[0] = new double[4001][];
    int count = 0;
    int temp;
    for (double lat = 47.0; lat <= 55.0; lat = lat + 0.002) {
      temp = (int) (lat * 1000.0);
      lat = ((double) temp) / 1000.0;

      Arrays.fill(latGrid, lat);
      meshGrid[0][count] = latGrid.clone();
      count = count + 1;
    }

    double[] lonGrid = new double[2751];
    double lon;

    for (int i = 0; i <= 2750; i++) {
      temp = (int) ((5 + 0.004 * i) * 1000.0);
      lon = ((double) temp) / 1000.0;
      lonGrid[i] = lon;
    }

    meshGrid[1] = new double[4001][];

    for (int j = 0; j <= 4000; j++) {
      meshGrid[1][j] = lonGrid.clone();
    }

    return meshGrid;
  }

}
