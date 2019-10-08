package airDataBackendService.repositories;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.mongodb.client.gridfs.model.GridFSFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

public class HeatmapRepositoryCustomImpl implements HeatmapRepositoryCustom {
  @Autowired
  GridFsTemplate gridFsTemplate;

  public InputStream findByFilename(String filename) {
    Query query = new Query(Criteria.where("filename").is(filename));
    GridFSFile file = gridFsTemplate.findOne(query);

    if (file == null) {
      return null;
    }

    GridFsResource resource = gridFsTemplate.getResource(file);

    try {
      InputStream inputStream = resource.getInputStream();
      return inputStream;
    } catch (IOException e) {
      System.out.println(e);
      return null;
    }
  }

  public void store(InputStream content, String filename) {
    Query query = new Query(Criteria.where("filename").is(filename));
    gridFsTemplate.delete(query);
    gridFsTemplate.store(content, filename);
  }
}