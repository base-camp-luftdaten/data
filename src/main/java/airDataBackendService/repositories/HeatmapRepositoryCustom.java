package airDataBackendService.repositories;

import java.io.InputStream;

public interface HeatmapRepositoryCustom {
  public InputStream findByFilename(String filename);

  public void store(InputStream content, String filename);
}